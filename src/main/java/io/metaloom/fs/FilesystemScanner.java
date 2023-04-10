package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

public interface FilesystemScanner<K extends FileKey> {

	/**
	 * Return the index that is being utilized by the scanner.
	 * 
	 * @return
	 */
	FileIndex<K> getIndex();

	/**
	 * Invoke a scan for the provided path.
	 * 
	 * @param startPath
	 * @return
	 * @throws IOException
	 */
	ScanResult scan(Path startPath) throws IOException;

	/**
	 * Invoke a scan for the provided path and stream the result.
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	Stream<FileInfo> scanStream(Path path) throws IOException;

}
