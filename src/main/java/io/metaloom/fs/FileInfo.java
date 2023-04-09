package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;

import io.metaloom.fs.impl.LinuxFile;

public interface FileInfo {

	Path path();

	Long inode();

	Long size();

	Long modTimeNano();

	Long modTimeSecond();

	FileState state();

	FileState state(FileState state);

	void updateAttr(LinuxFile file) throws IOException;

}
