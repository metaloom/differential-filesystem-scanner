package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface FilesystemScanner {

	FileIndex getIndex();

	ScanResult scan(Path startPath) throws IOException;

	Stream<FileInfo> scanStream(Path path) throws IOException;

}
