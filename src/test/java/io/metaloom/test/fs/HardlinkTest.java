package io.metaloom.test.fs;

import static io.metaloom.test.assertj.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.metaloom.fs.ScanResult;
import io.metaloom.fs.linux.LinuxFilesystemScanner;
import io.metaloom.fs.linux.impl.LinuxFilesystemScannerImpl;

/**
 * Verifies that files sharing an inode via hardlinks are not mistaken for moves.
 */
public class HardlinkTest {

	@TempDir
	Path root;

	private Path folderA;
	private Path folderB;
	private Path sourceFile;

	@BeforeEach
	public void setup() throws IOException {
		folderA = Files.createDirectories(root.resolve("folderA"));
		folderB = Files.createDirectories(root.resolve("folderB"));

		// Two plain files plus the hardlink source -> three known files initially.
		Files.writeString(folderA.resolve("existingFile"), "a");
		Files.writeString(folderA.resolve("existingFile2"), "b");
		sourceFile = Files.writeString(folderA.resolve("source"), "source");
	}

	@Test
	public void shouldReportNewHardlinkAsAddedButNotMoved() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		Path hardlink = folderB.resolve("hardlink");
		Files.createLink(hardlink, sourceFile);

		// The hardlink shares the inode of the source but is a genuinely new path -> added, not moved.
		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(0, 0, 0, 3, 1);
		assertEquals(hardlink, result.added().iterator().next().path(), "The hardlink should be reported as added.");

		// Re-scanning without changes keeps everything present.
		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 4, 0);
	}

	@Test
	public void shouldReportDeletedHardlinkWhileSourceStaysPresent() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		Path hardlink = folderB.resolve("hardlink");
		Files.createLink(hardlink, sourceFile);
		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 3, 1);
		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 4, 0);

		// Removing the hardlink must report it as deleted while the source remains present.
		Files.delete(hardlink);
		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(1, 0, 0, 3, 0);
		assertEquals(hardlink, result.deleted().iterator().next().path(), "The removed hardlink should be reported as deleted.");

		// The state must stabilise on subsequent scans.
		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 3, 0);
	}

	@Test
	public void shouldReportDeletedSourceWhileHardlinkStaysPresent() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(root);

		Path hardlink = folderB.resolve("hardlink");
		Files.createLink(hardlink, sourceFile);
		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 3, 1);
		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 4, 0);

		// Removing the original source (the hardlink still keeps the inode alive) must be reported as deleted.
		Files.delete(sourceFile);
		ScanResult result = scanner.scan(root);
		assertThat(result).hasResults(1, 0, 0, 3, 0);
		assertEquals(sourceFile, result.deleted().iterator().next().path(), "The removed source should be reported as deleted.");

		assertThat(scanner.scan(root)).hasResults(0, 0, 0, 3, 0);
	}
}
