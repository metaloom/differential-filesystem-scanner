package io.metaloom.fs.impl;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.metaloom.fs.FileIndex;
import io.metaloom.fs.FileInfo;

public class FileIndexImpl implements FileIndex {

	private Map<Long, FileInfo> onlineFiles = new HashMap<>();

	@Override
	public boolean isEmpty() {
		return onlineFiles.isEmpty();
	}

	@Override
	public FileIndexImpl remove(Long inode) {
		onlineFiles.remove(inode);
		return this;
	}

	@Override
	public Collection<FileInfo> values() {
		return onlineFiles.values();
	}

	@Override
	public FileInfo get(Long inode) {
		return onlineFiles.get(inode);
	}

	@Override
	public FileIndex add(FileInfo info) {
		onlineFiles.put(info.inode(), info);
		return this;
	}

	@Override
	public FileIndex add(Path path) {
		add(new FileInfoImpl(path));
		return this;
	}
}
