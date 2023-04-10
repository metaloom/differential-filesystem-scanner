package io.metaloom.fs.linux.impl;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.metaloom.fs.FileInfo;
import io.metaloom.fs.FileState;
import io.metaloom.fs.ScanResult;
import io.metaloom.fs.impl.DeltaIndexIterator;
import io.metaloom.fs.impl.ScanResultImpl;
import io.metaloom.fs.linux.LinuxFileIndex;
import io.metaloom.fs.linux.LinuxFilesystemScanner;

/**
 * Linux specific filesystem scanner implementation which utilizes the inode and st_time information from the file to detect changes.
 */
public class LinuxFilesystemScannerImpl implements LinuxFilesystemScanner {

	private static final Logger log = LoggerFactory.getLogger(LinuxFilesystemScannerImpl.class);

	private LinuxFileIndex index = new LinuxFileIndexImpl();

	@Override
	public LinuxFileIndex getIndex() {
		return index;
	}

	@Override
	public Stream<FileInfo> scanStream(Path startPath) throws IOException {
		// Reset the state in the index to unknown for all known files
		// This way we can find deleted files later
		Set<FileInfo> purgeSet = new HashSet<>();
		for (FileInfo info : index.values()) {
			if (info.state() == FileState.DELETED) {
				purgeSet.add(info);
			}
			info.state(FileState.UNKNOWN);
		}
		for (FileInfo info : purgeSet) {
			index.remove(info);
		}
		Iterator<FileInfo> presentFilesIt = Files.walk(startPath)
			.filter(Files::isRegularFile)
			.map(LinuxFile::new)
			.filter(not(LinuxFile::isSymbolicLink))
			.map(this::check)
			.iterator();

		DeltaIndexIterator it = new DeltaIndexIterator(presentFilesIt, index);
		return it.stream();
	}

	@Override
	public ScanResult scan(Path startPath) throws IOException {
		scanStream(startPath).count();
		return new ScanResultImpl(index.values());
	}

	private FileInfo check(LinuxFile file) {
		try {
			FileInfo indexFile = index.get(file.fileKey());
			// Found matching file by st_dev+inode. Lets compare the mod time/size
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
						return indexFile;
					} else {
						// Before we can assume the file has been moved we need to check whether there are any known hardlinks for the file.
						// The discovered file could be a new or existing hardlink
						for (FileInfo linkedFile : indexFile.getHardLinks()) {
							if (linkedFile.path().toAbsolutePath().equals(file.toAbsolutePath())) {
								linkedFile.state(FileState.PRESENT);
								return linkedFile;
							}
						}
						// We found a matching known hardlink so the file is still present
						log.debug("Checking whether file {} exists and has matching key", indexFile.path());
						// The index yields a file for the filekey but the path does not match up
						// Lets check whether the original file exists and has the same filekey.
						// This means the current file is a hardlinked to that file. Otherwise it has just been moved.
						LinuxFile indexLinuxFile = new LinuxFile(indexFile.path());
						if (indexLinuxFile.exists() && indexLinuxFile.fileKey().equals(indexFile.key())) {
							log.debug("File {} exists and has matching key", indexFile.path());
							FileInfo info = add(file);
							info.state(FileState.NEW);
							indexFile.hardlinkTo(info);
							log.debug("[HARDLINK] " + indexFile);
							return info;
						} else {
							// File has been moved. Update it with the new path
							indexFile.state(FileState.MOVED);
							indexFile.updateAttr(file);
							log.debug("[MOVED] " + indexFile);
							return indexFile;
						}
					}
				}
				return indexFile;
			} else {
				// Not known by file key -> new file
				FileInfo info = add(file);
				info.state(FileState.NEW);
				log.debug("[NEW] " + info);
				return info;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private FileInfo add(LinuxFile file) throws IOException {
		FileInfo info = new LinuxFileInfoImpl(file.toPath());
		info.updateAttr(file);
		index.add(info);
		return info;
	}

	private boolean hasInvalidModNanoTime(LinuxFile file) {
		// TODO we may need to handle files which have a zero / sub zero nano second timestamp.
		// return modTimeNano == STAT_NSEC_INVALID;
		return false;
	}

}
