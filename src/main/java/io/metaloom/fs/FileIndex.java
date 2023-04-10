package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import io.metaloom.fs.linux.impl.LinuxFileKey;

/**
 * Index which holds the file information that will be used to generate the diff during a scan operation.
 */
public interface FileIndex<K extends FileKey> {

	/**
	 * Add the fileinfo for the provided path to the index.
	 * 
	 * @param path
	 * @return Added info
	 * @throws IOException
	 */
	FileInfo add(Path path) throws IOException;

	/**
	 * Add the fileinfo to the index.
	 * 
	 * @param info
	 * @return Added info
	 */
	FileInfo add(FileInfo info);

	/**
	 * Retrieve the fileinfo using the provided file key.
	 * 
	 * @param key
	 * @return Found information
	 */
	FileInfo get(LinuxFileKey key);

	/**
	 * Return all values stored in the index.
	 * 
	 * @return
	 */
	Collection<FileInfo> values();

	/**
	 * Remove the info from the index.
	 * 
	 * @param file
	 * @return Fluent API
	 */
	FileIndex<K> remove(FileInfo file);

	/**
	 * Check whether the index is empty.
	 * 
	 * @return
	 */
	boolean isEmpty();

	/**
	 * Check if the index contains file info for the given key.
	 * 
	 * @param key
	 * @return
	 */
	boolean contains(K key);

	/**
	 * Clear the index. This will reset the index to an empty state.
	 */
	void clear();

}
