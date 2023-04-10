package io.metaloom.fs.impl;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.metaloom.fs.FileIndex;
import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;

/**
 * Iterator which will combine the stream of found files (present files) and the index. Once the present file iterator has been drained the index will be
 * queried for not seen (thus deleted files).
 */
public class DeltaIndexIterator implements Iterator<FileInfo> {

	private Iterator<FileInfo> presentFilesIt;
	private FileIndex index;
	private Iterator<FileInfo> remaininigIt = null;

	public DeltaIndexIterator(Iterator<FileInfo> presentFilesIt, FileIndex index) {
		this.presentFilesIt = presentFilesIt;
		this.index = index;
	}

	/**
	 * Return a stream that returns the present files first and afterwards the deleted files.
	 * 
	 * @return
	 */
	public Stream<FileInfo> stream() {
		Iterable<FileInfo> iterable = () -> this;
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	@Override
	public boolean hasNext() {
		if (presentFilesIt.hasNext()) {
			return true;
		} else if (remainingIt().hasNext()) {
			return true;
		}
		return false;
	}

	@Override
	public FileInfo next() {
		if (presentFilesIt.hasNext()) {
			FileInfo info = presentFilesIt.next();
			index.add(info);
			return info;
		} else {
			return remainingIt().next();
		}
	}

	/**
	 * Return the iterator which will utilize the index to select files with an unknown state. Those files have not been seen before and thus will be marked as
	 * deleted. This function must only be called once the present file iterator has been drained.
	 * 
	 * @return
	 */
	private Iterator<FileInfo> remainingIt() {
		if (remaininigIt == null) {
			remaininigIt = index.values().stream()
				.filter(info -> info.state() == FileState.UNKNOWN)
				.map(info -> {
					info.state(FileState.DELETED);
					return info;
				}).iterator();
		}
		return remaininigIt;
	}
}
