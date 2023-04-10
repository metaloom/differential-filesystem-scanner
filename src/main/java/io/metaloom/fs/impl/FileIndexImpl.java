package io.metaloom.fs.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
	public FileInfo add(FileInfo info) {
		Long inode = info.inode();
		Objects.requireNonNull(inode, "The inode for info " + info + " is missing. Can't add info to index");
		onlineFiles.put(info.inode(), info);
		return info;
	}

	@Override
	public FileInfo add(Path path) throws IOException {
		FileInfoImpl info = new FileInfoImpl(path);
		info.updateAttr(new LinuxFile(path));
		return add(info);
	}
}
