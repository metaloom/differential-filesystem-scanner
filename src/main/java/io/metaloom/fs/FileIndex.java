package io.metaloom.fs;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Index which holds the file information that will be used to generate the diff during a scan operation.
 */
public interface FileIndex {

	/**
	 * Add the fileinfo for the provided path to the index.
	 * 
	 * @param path
	 * @return Fluent API
	 */
	FileIndex add(Path path);

	/**
	 * Add the fileinfo to the index.
	 * 
	 * @param info
	 * @return Fluent API
	 */
	FileIndex add(FileInfo info);

	/**
	 * Retrieve the fileinfo using the provided inode.
	 * 
	 * @param inode
	 * @return Found information
	 */
	FileInfo get(Long inode);

	/**
	 * Return all values stored in the index.
	 * 
	 * @return
	 */
	Collection<FileInfo> values();

	/**
	 * Remove the info from the index for the given inode.
	 * 
	 * @param inode
	 * @return
	 */
	FileIndex remove(Long inode);

	/**
	 * Check whether the index is empty.
	 * 
	 * @return
	 */
	boolean isEmpty();

}
