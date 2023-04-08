package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;

public interface FileInfo {

	Path path();

	long inode() throws IOException;

	long size() throws IOException;

	long modTimeNano() throws IOException;

	long modTimeSecond() throws IOException;

	FileState state();

	FileState state(FileState state);

	boolean hasInvalidModNanoTime();

	void updateAttr(FileInfo info) throws IOException;

}
