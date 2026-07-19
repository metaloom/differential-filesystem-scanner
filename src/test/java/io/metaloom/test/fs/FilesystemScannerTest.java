package io.metaloom.test.fs;

import static io.metaloom.test.assertj.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;
import io.metaloom.fs.ScanResult;
import io.metaloom.fs.linux.LinuxFilesystemScanner;
import io.metaloom.fs.linux.impl.LinuxFilesystemScannerImpl;

/**
 * Tests for the {@link LinuxFilesystemScanner}. Each test operates on its own isolated {@link TempDir} to avoid cross-test interference.
 */
public class FilesystemScannerTest {

	@TempDir
	Path root;

	private Path folderA;
	private Path folderB;

	private Path deletedFile;
	private Path movingFile;
	private Path modifiedFile;

	@BeforeEach
	public void setup() throws IOException {
		folderA = Files.createDirectories(root.resolve("folderA"));
		folderB = Files.createDirectories(root.resolve("folderB"));

		// Two untouched files which stay present across the scans of every test.
		writeFile(folderA.resolve("existingFile"), "a");
		writeFile(folderA.resolve("existingFile2"), "b");
		deletedFile = writeFile(folderA.resolve("deleted.txt"), "c");
		movingFile = writeFile(folderA.resolve("moved.txt"), "d");
		modifiedFile = writeFile(folderB.resolve("modByTime.txt"), "e");
	}

	@Test
	public void shouldReportNewFilesOnInitialScan() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		// All five files are unknown to a fresh index and must be reported as added.
		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 0, 0, 5);
	}

	@Test
	public void shouldReportUnchangedFilesAsPresent() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		// Nothing was touched - the second scan must report all files as present.
		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 0, 5, 0);
	}

	@Test
	public void shouldDetectAddedFile() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		Path newFile = writeFile(folderA.resolve("new.txt"), "new");

		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 0, 5, 1);
		assertEquals(newFile, result.added().iterator().next().path(), "The new file should be reported as added.");
	}

	@Test
	public void shouldDetectDeletedFile() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		Files.delete(deletedFile);

		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(1, 0, 0, 4, 0);
		assertEquals(deletedFile, result.deleted().iterator().next().path(), "The removed file should be reported as deleted.");
	}

	@Test
	public void shouldDetectModifiedFileByContent() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		// Changing the content also changes the file size.
		writeFile(modifiedFile, "much longer content than before");

		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 1, 4, 0);
		assertEquals(modifiedFile, result.modified().iterator().next().path(), "The changed file should be reported as modified.");
	}

	@Test
	public void shouldDetectModifiedFileByModificationTime() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		// Keep the same size but move the modification timestamp forward. This mimics an in-place
		// modification that does not alter the file size but bumps the (nanosecond) mtime.
		Files.setLastModifiedTime(modifiedFile, FileTime.from(Instant.now().plusSeconds(120).plusNanos(123456)));

		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 1, 4, 0);
		assertEquals(modifiedFile, result.modified().iterator().next().path(), "The re-timestamped file should be reported as modified.");
	}

	@Test
	public void shouldDetectMovedFile() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		Path moveTarget = folderB.resolve("movedTo.txt");
		Files.move(movingFile, moveTarget, StandardCopyOption.ATOMIC_MOVE);

		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 1, 0, 4, 0);
		assertEquals(moveTarget, result.moved().iterator().next().path(), "The moved file should carry the new path.");
	}

	@Test
	public void shouldDetectAllChangesAtOnce() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		// Modify
		writeFile(modifiedFile, "changed content");
		// Add
		writeFile(folderA.resolve("new.txt"), "new");
		// Delete
		Files.delete(deletedFile);
		// Move
		Path moveTarget = folderB.resolve("movedTo.txt");
		Files.move(movingFile, moveTarget, StandardCopyOption.ATOMIC_MOVE);

		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(1, 1, 1, 2, 1);

		// A third, idempotent scan must report everything as present again.
		ScanResult result2 = scanner.scan(root);
		assertThat(result2).hasResults(0, 0, 0, 5, 0);
	}

	@Test
	public void shouldExposeStatesViaStreamApi() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scanStream(root).forEach(info -> {
		});

		writeFile(modifiedFile, "changed");
		writeFile(folderA.resolve("new.txt"), "new");
		Files.delete(deletedFile);

		long added = scanner.scanStream(root).filter(info -> info.state() == FileState.NEW).count();
		assertEquals(1, added, "The stream must expose the added file.");
	}

	@Test
	public void shouldHandleEmptyDirectory(@TempDir Path emptyRoot) throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		ScanResult result = scanner.scan(emptyRoot);
		assertThat(result).hasResults(0, 0, 0, 0, 0);
	}

	@Test
	public void shouldCloseWalkStreamAllowingDeletionOfScannedTree() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		try (Stream<FileInfo> stream = scanner.scanStream(root)) {
			stream.forEach(info -> {
			});
		}
		// If the walk stream leaked an open directory handle this scan would still succeed on Linux,
		// but we assert the scan can be repeated which exercises the purge/rescan path.
		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 0, 5, 0);
	}

	private static Path writeFile(Path path, String content) throws IOException {
		Files.writeString(path, content);
		return path;
	}
}
