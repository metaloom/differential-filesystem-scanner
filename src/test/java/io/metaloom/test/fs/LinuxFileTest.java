package io.metaloom.test.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.metaloom.fs.linux.impl.LinuxFile;

public class LinuxFileTest {

	@TempDir
	Path dir;

	private Path sourcePath;
	private Path hardlinkPath;
	private Path symlinkPath;

	@BeforeEach
	public void setup() throws IOException {
		// Links can not be committed to git reliably, so they are created per test run.
		sourcePath = Files.writeString(dir.resolve("source-file.txt"), "some file content");
		hardlinkPath = Files.createLink(dir.resolve("hardlink-file.txt"), sourcePath);
		symlinkPath = Files.createSymbolicLink(dir.resolve("symlink-file.txt"), sourcePath);
	}

	@Test
	public void shouldExposeFileAttributes() throws IOException {
		LinuxFile file = new LinuxFile(sourcePath);
		assertNotEquals(0, file.length());
		assertNotEquals(0, file.modTimeSecond());
		assertNotEquals(0, file.inode());
		assertNotNull(file.toAbsolutePath());
		assertNotNull(file.fileKey());
		assertNotEquals(0, file.stdev());
	}

	@Test
	public void shouldShareInodeForHardlink() throws IOException {
		LinuxFile source = new LinuxFile(sourcePath);
		LinuxFile hardlink = new LinuxFile(hardlinkPath);

		assertEquals(source.inode(), hardlink.inode(), "A hardlink must share the inode of its source.");
		assertEquals(source.fileKey(), hardlink.fileKey(), "A hardlink must share the file key of its source.");
		assertFalse(hardlink.isSymbolicLink());
	}

	@Test
	public void shouldFollowSymbolicLinkForInode() throws IOException {
		LinuxFile source = new LinuxFile(sourcePath);
		LinuxFile symlink = new LinuxFile(symlinkPath);

		// unix:ino follows symlinks, so the symlink resolves to the inode of its target.
		assertEquals(source.inode(), symlink.inode());
		assertTrue(symlink.isSymbolicLink());
		assertFalse(source.isSymbolicLink());
	}
}
