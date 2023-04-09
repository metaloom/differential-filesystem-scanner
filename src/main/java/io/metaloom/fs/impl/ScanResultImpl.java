package io.metaloom.fs.impl;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;
import io.metaloom.fs.ScanResult;

public class ScanResultImpl implements ScanResult {

	private Collection<FileInfo> values;

	public ScanResultImpl(Collection<FileInfo> values) {
		this.values = values;
	}

	@Override
	public Set<FileInfo> deleted() {
		return values.stream().filter(info -> info.state() == FileState.DELETED).collect(Collectors.toSet());
	}

	@Override
	public Set<FileInfo> moved() {
		return values.stream().filter(info -> info.state() == FileState.MOVED).collect(Collectors.toSet());
	}

	@Override
	public Set<FileInfo> modified() {
		return values.stream().filter(info -> info.state() == FileState.MODIFIED).collect(Collectors.toSet());
	}

	@Override
	public Set<FileInfo> present() {
		return values.stream().filter(info -> info.state() == FileState.PRESENT).collect(Collectors.toSet());
	}

	@Override
	public Set<FileInfo> added() {
		return values.stream().filter(info -> info.state() == FileState.NEW).collect(Collectors.toSet());
	}

}
