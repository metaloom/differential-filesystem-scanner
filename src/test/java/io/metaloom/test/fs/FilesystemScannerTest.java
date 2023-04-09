package io.metaloom.test.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.io.Files;

import io.metaloom.fs.ScanResult;
import io.metaloom.fs.impl.FilesystemScannerImpl;

public class FilesystemScannerTest {

	public static File ROOT = new File("target", "testfs");

	@BeforeEach
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(ROOT);
	}

	@Test
	public void testScan() throws IOException {
		File folderA = new File(ROOT, "folderA");
		File folderB = new File(ROOT, "folderB");
		folderA.mkdirs();
		folderB.mkdirs();

		File existingFile = new File(folderA, "existingFile");
		File existingFile2 = new File(folderA, "existingFile2");
		File newFile = new File(folderA, "new.txt");
		File deletedFile = new File(folderA, "deleted.txt");
		File modifiedByModTimeFile = new File(folderB, "modByTime.txt");
		File movingFile = new File(folderA, "moved.txt");
		File moveTargetFile = new File(folderB, "movedTo.txt");

		// Prepare test files
		Files.touch(existingFile);
		Files.touch(existingFile2);
		Files.touch(deletedFile);
		Files.touch(movingFile);
		Files.touch(modifiedByModTimeFile);

		FilesystemScannerImpl index = new FilesystemScannerImpl();
		Path sourcePath = Paths.get("target/testfs");
		// 1. Initial scan
		index.scan(sourcePath);

		System.out.println("----------");

		// Mod file
		Files.touch(modifiedByModTimeFile);

		// New file
		Files.touch(newFile);

		// Del file
		deletedFile.delete();

		// Move file
		movingFile.renameTo(moveTargetFile);

		// 2. Diff with second scan
		ScanResult result = index.scan(sourcePath);
		assertEquals(1, result.deleted().size(), "There should only be one file marked as deleted");
		assertEquals(1, result.moved().size(), "There should only be one file marked as moved");
		assertEquals(1, result.modified().size(), "There should only be one file marked as modified");
		assertEquals(2, result.present().size(), "There should only be two file marked as present");
		assertEquals(1, result.added().size(), "There should only be one file marked as added");

		// 3. Run - Now the diff should be different since we did not change files
		result = index.scan(sourcePath);
		assertEquals(0, result.deleted().size());
		assertEquals(0, result.moved().size());
		assertEquals(0, result.modified().size());
		assertEquals(0, result.added().size());
		assertEquals(5, result.present().size());
	}
}
