package io.metaloom.fs.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.metaloom.fs.FileIndex;
import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;
import io.metaloom.fs.FilesystemScanner;
import io.metaloom.fs.ScanResult;

public class FilesystemScannerImpl implements FilesystemScanner {

	private static final Logger log = LoggerFactory.getLogger(FilesystemScannerImpl.class);

	private FileIndex index = new FileIndexImpl();

	@Override
	public FileIndex getIndex() {
		return index;
	}

	@Override
	public Stream<FileInfo> scanStream(Path startPath) throws IOException {
		boolean wasEmpty = index.isEmpty();
		// Reset the state to unknown for all known files
		// This way we can find deleted files later
		Set<FileInfo> purgeSet = new HashSet<>();
		for (FileInfo info : index.values()) {
			if (info.state() == FileState.DELETED) {
				purgeSet.add(info);
			}
			info.state(FileState.UNKNOWN);
		}
		for (FileInfo info : purgeSet) {
			index.remove(info.inode());
		}
		Iterator<FileInfo> presentFilesIt = Files.walk(startPath)
			.filter(Files::isRegularFile)
			.map(LinuxFile::new)
			.map(file -> {
				try {
					// No need to check if the index is empty
					if (wasEmpty) {
						FileInfo info = new FileInfoImpl(file.toPath());
						info.state(FileState.PRESENT);
						info.updateAttr(file);
						log.debug("[NEW]: " + info);
						return info;
					} else {
						return check(file);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).iterator();

		DeltaIndexIterator it = new DeltaIndexIterator(presentFilesIt, index);
		return it.stream();
	}

	@Override
	public ScanResult scan(Path startPath) throws IOException {
		scanStream(startPath).count();
		return new ScanResultImpl(index.values());
	}

	private FileInfo check(LinuxFile file) throws IOException {
		FileInfo indexFile = index.get(file.inode());
		// Found matching file by inode. Lets compare the mod time/size
		if (indexFile != null) {
			// Check if the file was modified
			boolean modified = indexFile.size() != file.length() || indexFile.modTimeNano() != file.modTimeNano()
				|| indexFile.modTimeSecond() != file.modTimeSecond()
				|| hasInvalidModNanoTime(file);
			if (modified) {
				indexFile.state(FileState.MODIFIED);
				indexFile.updateAttr(file);
				log.debug("[MODIFIED] " + indexFile);
			} else {
				// Now lets check the path
				if (indexFile.path().toAbsolutePath().equals(file.toAbsolutePath())) {
					// Same path - same inode - same size -> keep it online
					indexFile.state(FileState.PRESENT);
					log.debug("[PRESENT] " + indexFile);
				} else {
					indexFile.state(FileState.MOVED);
					indexFile.updateAttr(file);
					log.debug("[MOVED] " + indexFile);
				}
			}
			return indexFile;
		} else {
			// Not known by inode -> new file
			FileInfo info = new FileInfoImpl(file.toPath());
			info.updateAttr(file);
			info.state(FileState.NEW);
			index.add(info);
			log.debug("[NEW] " + info);
			return info;
		}
	}

	private boolean hasInvalidModNanoTime(LinuxFile file) {
		// return modTimeNano == STAT_NSEC_INVALID;
		return false;
	}

}
