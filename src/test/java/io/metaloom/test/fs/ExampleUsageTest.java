package io.metaloom.test.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.metaloom.fs.FileIndex;
import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FilesystemScanner;
import io.metaloom.fs.ScanResult;
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
}
