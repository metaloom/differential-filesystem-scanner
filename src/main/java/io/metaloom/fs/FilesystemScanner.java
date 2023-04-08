package io.metaloom.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.metaloom.fs.impl.FileInfoImpl;

public class FilesystemScanner {

	private static final Logger log = LoggerFactory.getLogger(FilesystemScanner.class);

	private Map<Long, FileInfo> onlineFiles = new HashMap<>();

	public boolean isEmpty() {
		return onlineFiles.isEmpty();
	}

	public void scan(Path startPath) throws IOException {
		boolean wasEmpty = onlineFiles.isEmpty();
		// Reset the state to unknown for all known files
		// This way we can find deleted files later
		Set<FileInfo> purgeSet = new HashSet<>();
		for (FileInfo info : onlineFiles.values()) {
			if (info.state() == FileState.DELETED) {
				purgeSet.add(info);
			}
			info.state(FileState.UNKNOWN);
		}
		for (FileInfo info : purgeSet) {
			onlineFiles.remove(info.inode());
		}
		Files.walk(startPath)
			.filter(Files::isRegularFile)
			.forEach(path -> {
				try {
					// No need to check if the index is empty
					if (wasEmpty) {
						FileInfo info = new FileInfoImpl(path);
						info.state(FileState.PRESENT);
						info.size();
						info.modTimeNano();
						info.modTimeSecond();
						info.inode();
						log.info("[NEW]: " + info);
						add(info);
					} else {
						check(path);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		if (!wasEmpty) {
			// Set<FileInfo> deletedFiles = new HashSet<>();
			for (FileInfo info : onlineFiles.values()) {
				if (info.state() == FileState.UNKNOWN) {
					info.state(FileState.DELETED);
					log.info("[DELETE] " + info);
				}
			}
		}

	}

	private void add(FileInfo info) throws IOException {
		onlineFiles.put(info.inode(), info);
	}

	public void check(Path path) throws IOException {
		FileInfo info = new FileInfoImpl(path);
		FileInfo indexFile = onlineFiles.get(info.inode());
		// Found matching file by inode. Lets compare the mod time/size
		if (indexFile != null) {
			// Check if the file was modified
			boolean modified = indexFile.size() != info.size() || indexFile.modTimeNano() != info.modTimeNano()
				|| indexFile.modTimeSecond() != info.modTimeSecond()
				|| info.hasInvalidModNanoTime();
			if (modified) {
				indexFile.state(FileState.MODIFIED);
				indexFile.updateAttr(info);
				log.info("[MODIFIED] " + indexFile);
			} else {
				// Now lets check the path
				if (indexFile.path().toAbsolutePath().equals(info.path().toAbsolutePath())) {
					// Same path - same inode - same size -> keep it online
					indexFile.state(FileState.PRESENT);
					log.info("[PRESENT] " + indexFile);
				} else {
					indexFile.state(FileState.MOVED);
					indexFile.updateAttr(info);
					log.info("[MOVED] " + indexFile);
				}
			}
		} else {
			// Not known by inode -> new file
			info.state(FileState.NEW);
			log.info("[NEW] " + info);
			add(info);
		}
	}

	public Set<FileInfo> deleted() {
		return onlineFiles.values().stream().filter(info -> info.state() == FileState.DELETED).collect(Collectors.toSet());
	}

	public Set<FileInfo> moved() {
		return onlineFiles.values().stream().filter(info -> info.state() == FileState.MOVED).collect(Collectors.toSet());
	}

	public Set<FileInfo> modified() {
		return onlineFiles.values().stream().filter(info -> info.state() == FileState.MODIFIED).collect(Collectors.toSet());
	}

	public Set<FileInfo> present() {
		return onlineFiles.values().stream().filter(info -> info.state() == FileState.PRESENT).collect(Collectors.toSet());
	}

	public Set<FileInfo> added() {
		return onlineFiles.values().stream().filter(info -> info.state() == FileState.NEW).collect(Collectors.toSet());
	}

}
