package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import io.metaloom.fs.linux.impl.LinuxFile;
import io.metaloom.fs.linux.impl.LinuxFileKey;

public interface FileInfo {

	Path path();

	LinuxFileKey key();

	Long inode();

	Long size();

	Long modTimeNano();

	Long modTimeSecond();

	FileState state();

	FileState state(FileState state);

	void updateAttr(LinuxFile file) throws IOException;

	/**
	 * Create a link reference between both files.
	 * 
	 * @param info
	 */
	void hardlinkTo(FileInfo info);

	Set<FileInfo> getHardLinks();

}
