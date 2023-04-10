package io.metaloom.test.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.metaloom.fs.FileIndex;
import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FilesystemScanner;
import io.metaloom.fs.ScanResult;
import io.metaloom.fs.impl.FileInfoImpl;
import io.metaloom.fs.impl.FilesystemScannerImpl;

public class ExampleUsageTest {

	@Test
	public void testUsage() throws IOException {
		// SNIPPET START usage
		FilesystemScanner scanner = new FilesystemScannerImpl();
		FileIndex index = scanner.getIndex();

		index.add(Paths.get("target/testfs/folderB/modByTime.txt"));

		Path sourcePath = Paths.get("src");
		ScanResult result = scanner.scan(sourcePath);
		Set<FileInfo> addedFiles = result.added();
		Set<FileInfo> deletedFiles = result.deleted();
		Set<FileInfo> modifiedFiles = result.modified();
		Set<FileInfo> movedFiles = result.moved();
		// SNIPPET END usage
	}

	@Test
	public void testStreamUsage() throws IOException {
		// SNIPPET START usage
		FilesystemScanner scanner = new FilesystemScannerImpl();
		FileIndex index = scanner.getIndex();

		// Add a file to the index which does not exist.
		// It will be listed as "deleted" by the scanner.
		FileInfo missingFileInfo = new FileInfoImpl(Paths.get("missingFile.txt"), 42L, 2L, 1L, 2L);
		index.add(missingFileInfo);

		// And add a existing file. It will be tracked with state "PRESENT"
		index.add(Paths.get("src/test/resources/logback.xml"));

		Path sourcePath = Paths.get("src");
		Stream<FileInfo> stream = scanner.scanStream(sourcePath);
		stream.forEach(info -> {
			System.out.println(info.state() + "\t" + info.path());
		});
		// SNIPPET END usage
	}
}
