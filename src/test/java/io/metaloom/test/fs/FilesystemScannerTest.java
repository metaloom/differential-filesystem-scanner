package io.metaloom.test.fs;

import static io.metaloom.test.assertj.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.io.Files;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.ScanResult;
import io.metaloom.fs.linux.LinuxFilesystemScanner;
import io.metaloom.fs.linux.impl.LinuxFilesystemScannerImpl;

public class FilesystemScannerTest {

	public static File ROOT = new File("target", "testfs");
	public static File folderA = new File(ROOT, "folderA");
	public static File folderB = new File(ROOT, "folderB");
	public static Path ROOT_PATH = ROOT.toPath();
	public static File existingFile = new File(folderA, "existingFile");
	public static File hardlinkFile = new File(folderB, "hardlinkFile");
	public static File existingFile2 = new File(folderA, "existingFile2");
	public static File newFile = new File(folderA, "new.txt");
	public static File deletedFile = new File(folderA, "deleted.txt");
	public static File modifiedByModTimeFile = new File(folderB, "modByTime.txt");
	public static File movingFile = new File(folderA, "moved.txt");
	public static File moveTargetFile = new File(folderB, "movedTo.txt");

	@BeforeEach
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(ROOT);
		// Prepare test files
		folderA.mkdirs();
		folderB.mkdirs();
		Files.touch(existingFile);
		Files.touch(existingFile2);
		Files.touch(deletedFile);
		Files.touch(movingFile);
		Files.touch(modifiedByModTimeFile);
	}

	@Test
	public void testStream() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scanStream(ROOT_PATH).count();
		System.out.println("----------");

		scanner.getIndex().values().forEach(info -> {
			System.out.println(info);
		});
		System.out.println("-------");

		// Mod file
		Files.touch(modifiedByModTimeFile);

		// New file
		Files.touch(newFile);

		// Del file
		deletedFile.delete();

		// Move file
		movingFile.renameTo(moveTargetFile);

		Stream<FileInfo> stream = scanner.scanStream(ROOT_PATH);
		stream.forEach(info -> {
			System.out.println(info);
		});
		System.out.println("-------");
		scanner.getIndex().values().forEach(info -> {
			System.out.println(info);
		});
	}

	@Test
	public void testHardLinkHandling() throws IOException {
		LinuxFilesystemScanner scanner = new LinuxFilesystemScannerImpl();
		scanner.scan(ROOT_PATH);

		// Now add a hardlink
		java.nio.file.Files.createLink(hardlinkFile.toPath(), existingFile.toPath());

		ScanResult result = scanner.scan(ROOT_PATH);
		assertThat(result).hasResults(0, 0, 0, 5, 1);
		assertEquals(hardlinkFile.toPath(), result.added().iterator().next().path(), "The hardlink file was added");
		assertEquals(existingFile.toPath(), result.added().iterator().next().getHardLinks().iterator().next().path(),
			"The added file is a hardlink to the existing file");

		ScanResult result2 = scanner.scan(ROOT_PATH);
		assertThat(result2).hasResults(0, 0, 0, 5 + 1, 0);

		// Now remove hardlink
		hardlinkFile.delete();

		ScanResult result3 = scanner.scan(ROOT_PATH);
		assertThat(result3).hasResults(1, 0, 0, 6 - 1, 0);
		assertEquals(hardlinkFile.toPath(), result3.deleted().iterator().next().path());

		System.out.println("------");
		for (int i = 0; i < 3; i++) {
			ScanResult result4 = scanner.scan(ROOT_PATH);
			result4.deleted().forEach(info -> {
				System.out.println(info);
			});
			assertThat(result4).hasResults(0, 0, 0, 5, 0);
		}
	}

	@Test
	public void testScan() throws IOException {
		LinuxFilesystemScanner index = new LinuxFilesystemScannerImpl();

		// 1. Initial scan
		index.scan(ROOT_PATH);

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
		ScanResult result = index.scan(ROOT_PATH);
		assertEquals(1, result.deleted().size(), "There should only be one file marked as deleted");
		assertEquals(1, result.moved().size(), "There should only be one file marked as moved");
		assertEquals(1, result.modified().size(), "There should only be one file marked as modified");
		assertEquals(2, result.present().size(), "There should only be two file marked as present");
		assertEquals(1, result.added().size(), "There should only be one file marked as added");

		// 3. Run - Now the diff should be different since we did not change files
		result = index.scan(ROOT_PATH);
		assertEquals(0, result.deleted().size());
		assertEquals(0, result.moved().size());
		assertEquals(0, result.modified().size());
		assertEquals(0, result.added().size());
		assertEquals(5, result.present().size());
	}
}
