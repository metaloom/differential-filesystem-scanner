package io.metaloom.fs.linux.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;

public class LinuxFileInfoImpl implements FileInfo {

	private Set<FileInfo> hardLinks = new HashSet<>();
	private Path path;
	private LinuxFileKey key;
	private Long inode;
	private Long size;
	private Long modTimeSecond;
	private Long modTimeNano;
	private FileState state;

	public LinuxFileInfoImpl(Path path) {
		this.path = path;
	}

	public LinuxFileInfoImpl(Path path, long stdev, long inode, long size, long modTimeSecond, long modTimeNano) {
		this.path = path;
		this.inode = inode;
		this.key = new LinuxFileKey(stdev, inode);
		this.size = size;
		this.modTimeSecond = modTimeSecond;
		this.modTimeNano = modTimeNano;
	}

	@Override
	public Path path() {
		return path;
	}

	@Override
	public Long inode() {
		return inode;
	}

	@Override
	public Long size() {
		return size;
	}

	@Override
	public Long modTimeNano() {
		return modTimeNano;
	}

	@Override
	public Long modTimeSecond() {
		return modTimeSecond;
	}

	@Override
	public FileState state() {
		return state;
	}

	@Override
	public LinuxFileKey key() {
		return key;
	}

	@Override
	public FileState state(FileState state) {
		FileState oldState = this.state;
		this.state = state;
		return oldState;
	}

	@Override
	public void hardlinkTo(FileInfo info) {
		if (hardLinks.contains(info)) {
			return;
		}
		hardLinks.add(info);
		info.hardlinkTo(this);
	}

	@Override
	public Set<FileInfo> getHardLinks() {
		return hardLinks;
	}

	@Override
	public String toString() {
		return "FileInfo[path:" + path + ", inode:" + inode + ", size:" + size + ", modTimeSecond:" + modTimeSecond + ", modTimeNano:" + modTimeNano
			+ ", key: " + key + ", state:" + state + "]";
	}

	@Override
	public void updateAttr(LinuxFile file) throws IOException {
		this.key = file.fileKey();
		this.path = file.toPath();
		this.inode = file.inode();
		this.modTimeNano = file.modTimeNano();
		this.modTimeSecond = file.modTimeSecond();
		this.size = file.length();
	}

}
