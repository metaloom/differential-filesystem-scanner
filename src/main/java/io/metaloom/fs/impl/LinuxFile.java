package io.metaloom.fs.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import io.metaloom.utils.fs.INodeUtils;

public class LinuxFile extends File {

	private static final long serialVersionUID = 8839106040722126846L;

	private Long inode = null;
	private Long modTimeSecond = null;
	private Long modTimeNano = null;

	public LinuxFile(Path path) {
		super(path.toString());
	}

	public LinuxFile(String pathname) {
		super(pathname);
	}

	public long inode() throws IOException {
		if (inode == null) {
			inode = INodeUtils.loadInode(toPath());
		}
		return inode;
	}

	public long modTimeNano() throws IOException {
		if (modTimeNano == null) {
			modTimeNano = INodeUtils.loadModificationNano(toPath());
		}
		return modTimeNano;
	}

	public long modTimeSecond() throws IOException {
		if (modTimeSecond == null) {
			modTimeSecond = INodeUtils.loadModificationEpochSecond(toPath());
		}
		return modTimeSecond;
	}

	public Path toAbsolutePath() {
		return toPath().toAbsolutePath();
	}

}
