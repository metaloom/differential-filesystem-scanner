package io.metaloom.fs.impl;

import java.io.IOException;
import java.nio.file.Path;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;

public class FileInfoImpl implements FileInfo {

	private Path path;
	private Long inode;
	private Long size;
	private Long modTimeSecond;
	private Long modTimeNano;
	private FileState state;

	public FileInfoImpl(Path path) {
		this.path = path;
	}

	public FileInfoImpl(Path path, long inode, long size, long modTimeSecond, long modTimeNano) {
		this.path = path;
		this.inode = inode;
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
	public FileState state(FileState state) {
		FileState oldState = this.state;
		this.state = state;
		return oldState;
	}

	@Override
	public String toString() {
		return "FileInfo[path:" + path + ", inode:" + inode + ", size:" + size + ", modTimeSecond:" + modTimeSecond + ", modTimeNano:" + modTimeNano
			+ ", state:" + state + "]";
	}

	@Override
	public void updateAttr(LinuxFile file) throws IOException {
		this.path = file.toPath();
		this.inode = file.inode();
		this.modTimeNano = file.modTimeNano();
		this.modTimeSecond = file.modTimeSecond();
		this.size = file.length();
	}

}
