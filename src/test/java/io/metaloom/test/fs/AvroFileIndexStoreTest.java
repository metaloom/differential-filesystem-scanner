package io.metaloom.test.fs;

import static io.metaloom.test.assertj.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.metaloom.fs.FileIndexStore;
import io.metaloom.fs.FileInfo;
import io.metaloom.fs.ScanResult;
import io.metaloom.fs.linux.LinuxFileIndex;
import io.metaloom.fs.linux.LinuxFilesystemScanner;
import io.metaloom.fs.linux.impl.AvroFileIndexStore;
import io.metaloom.fs.linux.impl.LinuxFilesystemScannerImpl;

/**
 * Verifies that a {@link LinuxFileIndex} can be persisted and reloaded and that a differential scan can be resumed from a reloaded index.
 */
public class AvroFileIndexStoreTest {

	@TempDir
	Path root;

	private Path indexFile;
	private final FileIndexStore store = new AvroFileIndexStore();

	@BeforeEach
	public void setup(@TempDir Path indexDir) throws IOException {
		indexFile = indexDir.resolve("index.avro");
		Files.writeString(root.resolve("a.txt"), "a");
		Files.writeString(root.resolve("b.txt"), "bb");
		Files.createDirectories(root.resolve("sub"));
		Files.writeString(root.resolve("sub/c.txt"), "ccc");
	}

	@Test
	public void shouldReturnEmptyIndexWhenFileMissing() throws IOException {
		assertFalse(store.exists(indexFile));
		LinuxFileIndex index = store.load(indexFile);
		assertTrue(index.isEmpty(), "A missing index file must load as an empty index.");
	}

	@Test
	public void shouldRoundTripAllEntries() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		store.store(indexFile, scanner.getIndex());
		assertTrue(store.exists(indexFile));

		LinuxFileIndex loaded = store.load(indexFile);

		Map<String, FileInfo> before = byPath(scanner.getIndex());
		Map<String, FileInfo> after = byPath(loaded);
		assertEquals(before.keySet(), after.keySet(), "The reloaded index must contain the same paths.");

		for (String path : before.keySet()) {
			FileInfo a = before.get(path);
			FileInfo b = after.get(path);
			assertEquals(a.key(), b.key(), "key mismatch for " + path);
			assertEquals(a.inode(), b.inode(), "inode mismatch for " + path);
			assertEquals(a.size(), b.size(), "size mismatch for " + path);
			assertEquals(a.modTimeSecond(), b.modTimeSecond(), "mtime sec mismatch for " + path);
			assertEquals(a.modTimeNano(), b.modTimeNano(), "mtime nano mismatch for " + path);
		}
	}

	@Test
	public void shouldReconstructHardlinksOnLoad() throws IOException {
		Path source = root.resolve("source");
		Files.writeString(source, "shared");
		Files.createLink(root.resolve("hardlink"), source);

		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);
		store.store(indexFile, scanner.getIndex());

		LinuxFileIndex loaded = store.load(indexFile);
		Map<String, FileInfo> after = byPath(loaded);
		FileInfo hardlink = after.get(root.resolve("hardlink").toString());
		FileInfo source2 = after.get(source.toString());

		Set<Path> linkedPaths = hardlink.getHardLinks().stream().map(FileInfo::path).collect(Collectors.toSet());
		assertTrue(linkedPaths.contains(source2.path()), "The reloaded hardlink must reference its source.");
	}

	@Test
	public void shouldResumeDifferentialScanFromReloadedIndex() throws IOException {
		// First "process run" - initial scan then persist.
		LinuxFilesystemScanner first = new LinuxFilesystemScannerImpl();
		first.scan(root);
		store.store(indexFile, first.getIndex());

		// Second "process run" - fresh scanner seeded from the persisted index.
		LinuxFileIndex reloaded = store.load(indexFile);
		LinuxFilesystemScanner second = new LinuxFilesystemScannerImpl(reloaded);
		ScanResult unchanged = second.scan(root);
		assertThat(unchanged).hasResults(0, 0, 0, 3, 0);

		// Modify one file, persist, reload again and confirm only that file is modified.
		Files.writeString(root.resolve("b.txt"), "changed content");
		store.store(indexFile, second.getIndex());

		LinuxFilesystemScanner third = new LinuxFilesystemScannerImpl(store.load(indexFile));
		ScanResult result = third.scan(root);
		assertThat(result).hasResults(0, 0, 1, 2, 0);
		assertEquals(root.resolve("b.txt"), result.modified().iterator().next().path());
	}

	private static Map<String, FileInfo> byPath(LinuxFileIndex index) {
		return index.values().stream().collect(Collectors.toMap(info -> info.path().toString(), info -> info));
	}
}
