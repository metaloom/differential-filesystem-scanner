package io.metaloom.fs.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;
import io.metaloom.utils.fs.INodeUtils;

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
	public long inode() throws IOException {
		if (inode == null) {
			inode = INodeUtils.loadInode(path);
		}
		return inode;
	}

	@Override
	public long size() throws IOException {
		if (size == null) {
			size = Files.size(path);
		}
		return size;
	}

	@Override
	public long modTimeNano() throws IOException {
		if (modTimeNano == null) {
			modTimeNano = INodeUtils.loadModificationNano(path);
		}
		return modTimeNano;
	}

	@Override
	public long modTimeSecond() throws IOException {
		if (modTimeSecond == null) {
			modTimeSecond = INodeUtils.loadModificationEpochSecond(path);
		}
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
	public boolean hasInvalidModNanoTime() {
		// return modTimeNano == STAT_NSEC_INVALID;
		return false;
	}

	@Override
	public void updateAttr(FileInfo info) throws IOException {
		this.path = info.path();
		this.modTimeNano = info.modTimeNano();
		this.modTimeSecond = info.modTimeSecond();
		this.size = info.size();
	}

}
