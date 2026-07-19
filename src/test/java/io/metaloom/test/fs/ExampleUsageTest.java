package io.metaloom.test.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.ScanResult;
import io.metaloom.fs.linux.LinuxFileIndex;
import io.metaloom.fs.linux.LinuxFilesystemScanner;
import io.metaloom.fs.linux.impl.LinuxFileInfoImpl;
import io.metaloom.fs.linux.impl.LinuxFilesystemScannerImpl;

public class ExampleUsageTest {

	@TempDir
	Path sourcePath;

	private Path knownFile;

	@BeforeEach
	public void setup() throws IOException {
		knownFile = Files.writeString(sourcePath.resolve("known.txt"), "content");
	}

	@Test
	public void testUsage() throws IOException {
		// SNIPPET START usage
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		LinuxFileIndex index = scanner.getIndex();

		// Pre-populate the index with a previously known file.
		index.add(knownFile);

		// Scan the tree and diff it against the index.
		ScanResult result = scanner.scan(sourcePath);
		Set<FileInfo> addedFiles = result.added();
		Set<FileInfo> deletedFiles = result.deleted();
		Set<FileInfo> modifiedFiles = result.modified();
		Set<FileInfo> movedFiles = result.moved();
		// SNIPPET END usage
	}

	@Test
	public void testStreamUsage() throws IOException {
		// SNIPPET START stream
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		LinuxFileIndex index = scanner.getIndex();

		// Add a file to the index which does not exist.
		// It will be listed as "deleted" by the scanner.
		FileInfo missingFileInfo = new LinuxFileInfoImpl(sourcePath.resolve("missingFile.txt"), 4L, 42L, 2L, 1L, 2L);
		index.add(missingFileInfo);

		// And add an existing file. It will be tracked with state "PRESENT".
		index.add(knownFile);

		Stream<FileInfo> stream = scanner.scanStream(sourcePath);
		stream.forEach(info -> {
			System.out.println(info.state() + "\t" + info.path());
		});
		// SNIPPET END stream
	}
}
