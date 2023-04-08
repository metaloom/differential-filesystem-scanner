package io.metaloom.test.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FilesystemScanner;

public class ExampleUsageTest {

	@Test
	public void testUsage() throws IOException {
		// SNIPPET START usage
		FilesystemScanner index = new FilesystemScanner();
		Path sourcePath = Paths.get("src");
		index.scan(sourcePath);

		Set<FileInfo> addedFiles = index.added();
		Set<FileInfo> deletedFiles = index.deleted();
		Set<FileInfo> modifiedFiles = index.modified();
		Set<FileInfo> movedFiles = index.moved();
		// SNIPPET END usage
	}
}
