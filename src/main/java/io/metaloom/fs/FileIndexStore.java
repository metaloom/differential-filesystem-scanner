package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Path;

import io.metaloom.fs.linux.LinuxFileIndex;

/**
 * A binary store for a {@link LinuxFileIndex}. It allows the in-memory index built during a scan to be persisted and later reloaded, so that a differential
 * scan can be resumed across process restarts.
 *
 * <p>
 * Only the identity and diff-relevant attributes of each file are persisted (path, device, inode, size and modification time). The transient
 * {@link FileState} is not stored - a reloaded index represents the previously known state which a subsequent {@link io.metaloom.fs.FilesystemScanner#scan(Path)
 * scan} will diff against. Hardlink relationships are reconstructed on load by grouping entries which share the same file key.
 * </p>
 */
public interface FileIndexStore {

	/**
	 * Persist the provided index to the given file. An existing file is overwritten.
	 *
	 * @param file
	 *            target file
	 * @param index
	 *            index to persist
	 * @throws IOException
	 */
	void store(Path file, LinuxFileIndex index) throws IOException;

	/**
	 * Load a previously persisted index from the given file. If the file does not exist an empty index is returned.
	 *
	 * @param file
	 *            source file
	 * @return loaded index, never {@code null}
	 * @throws IOException
	 */
	LinuxFileIndex load(Path file) throws IOException;

	/**
	 * Check whether a persisted index exists at the given location.
	 *
	 * @param file
	 *            index file
	 * @return true when the file exists
	 */
	boolean exists(Path file);

}
