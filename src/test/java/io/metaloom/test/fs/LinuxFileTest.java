package io.metaloom.test.fs;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.metaloom.fs.impl.LinuxFile;

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
}
