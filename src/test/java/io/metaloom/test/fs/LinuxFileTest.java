package io.metaloom.test.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.metaloom.fs.linux.impl.LinuxFile;

public class LinuxFileTest {

	@Test
	public void testFileAPI() throws IOException {
		LinuxFile file = new LinuxFile("pom.xml");
		assertNotEquals(0, file.length());
		assertNotEquals(0, file.modTimeNano());
		assertNotEquals(0, file.modTimeSecond());
		assertNotEquals(0, file.inode());
		assertNotNull(file.toAbsolutePath());
	}

	@Test
	public void testLinks() throws IOException {
		LinuxFile sourceFile = new LinuxFile("src/test/resources/files/source-file.txt");
		LinuxFile hardlinkFile = new LinuxFile("src/test/resources/files/hardlink-file.txt");
		LinuxFile symlinkFile = new LinuxFile("src/test/resources/files/symlink-file.txt");

		assertEquals(sourceFile.inode(), hardlinkFile.inode());
		assertEquals(sourceFile.inode(), symlinkFile.inode());
		assertTrue(symlinkFile.isSymbolicLink());
		assertFalse(sourceFile.isSymbolicLink());
		assertNotNull(sourceFile.fileKey());
		assertNotNull(sourceFile.stdev());
	}
}
