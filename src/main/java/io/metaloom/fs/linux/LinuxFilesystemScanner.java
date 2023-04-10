package io.metaloom.fs.linux;

import io.metaloom.fs.FilesystemScanner;
import io.metaloom.fs.linux.impl.LinuxFileKey;

public interface LinuxFilesystemScanner extends FilesystemScanner<LinuxFileKey> {

	@Override
	LinuxFileIndex getIndex();

}
