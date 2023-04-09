package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;

public interface FilesystemScanner {

	FileIndex getIndex();

	ScanResult scan(Path startPath) throws IOException;

}
