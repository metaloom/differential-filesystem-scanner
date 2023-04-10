package io.metaloom.fs.linux.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.linux.LinuxFileIndex;

/**
 * Linux specific file index implementation which relies on {@link LinuxFileKey} to identify files.
 */
public class LinuxFileIndexImpl implements LinuxFileIndex {

	/**
	 * Index used to lookup files using the key
	 */
	private Map<LinuxFileKey, FileInfo> keyIndex = new HashMap<>();

	private Set<FileInfo> files = new HashSet<>();

	@Override
	public void clear() {
		files.clear();
		keyIndex.clear();
	}

	@Override
	public boolean isEmpty() {
		return keyIndex.isEmpty();
	}

	@Override
	public LinuxFileIndex remove(FileInfo info) {
		FileInfo foundInfo = keyIndex.get(info.key());
		if (foundInfo == null) {
			return this;
		} else {
			// Let's check whether the info points to a removed hardlink.
			for (FileInfo linked : foundInfo.getHardLinks()) {
				if (linked.path().equals(info.path())) {
					files.remove(linked);
					return this;
				}
			}
			files.remove(info);
		}
		return this;
	}

	@Override
	public Collection<FileInfo> values() {
		return files;
	}

	@Override
	public FileInfo get(LinuxFileKey key) {
		return keyIndex.get(key);
	}

	@Override
	public FileInfo add(FileInfo info) {
		LinuxFileKey key = info.key();
		Objects.requireNonNull(key, "The filekey for info " + info + " is missing. Can't add info to index.");
		keyIndex.put(key, info);
		files.add(info);
		return info;
	}

	@Override
	public FileInfo add(Path path) throws IOException {
		LinuxFileInfoImpl info = new LinuxFileInfoImpl(path);
		info.updateAttr(new LinuxFile(path));
		return add(info);
	}

	@Override
	public boolean contains(LinuxFileKey key) {
		return keyIndex.containsKey(key);
	}
}
