package io.metaloom.fs.linux.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.metaloom.fs.FileIndexStore;
import io.metaloom.fs.FileInfo;
import io.metaloom.fs.linux.LinuxFileIndex;
import io.metaloom.fs.linux.impl.avro.LinuxFileEntry;

/**
 * Apache Avro backed {@link FileIndexStore}. The index is stored as a single Avro data file containing one {@link LinuxFileEntry} record per tracked file.
 */
public class AvroFileIndexStore implements FileIndexStore {

	private static final Logger log = LoggerFactory.getLogger(AvroFileIndexStore.class);

	@Override
	public void store(Path file, LinuxFileIndex index) throws IOException {
		Path parent = file.toAbsolutePath().getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		DatumWriter<LinuxFileEntry> datumWriter = new SpecificDatumWriter<>(LinuxFileEntry.class);
		try (DataFileWriter<LinuxFileEntry> writer = new DataFileWriter<>(datumWriter)) {
			writer.setCodec(CodecFactory.deflateCodec(CodecFactory.DEFAULT_DEFLATE_LEVEL));
			writer.create(LinuxFileEntry.getClassSchema(), file.toFile());
			for (FileInfo info : index.values()) {
				writer.append(toEntry(info));
			}
		}
		log.debug("Stored index with {} entries to {}", index.values().size(), file);
	}

	@Override
	public LinuxFileIndex load(Path file) throws IOException {
		LinuxFileIndex index = new LinuxFileIndexImpl();
		if (!exists(file)) {
			log.debug("Index file {} does not exist - returning empty index", file);
			return index;
		}

		// Group loaded infos by their file key so that hardlinks (which share a key but have distinct paths) can be re-linked after all entries were added.
		Map<LinuxFileKey, List<FileInfo>> byKey = new HashMap<>();
		DatumReader<LinuxFileEntry> datumReader = new SpecificDatumReader<>(LinuxFileEntry.class);
		try (DataFileReader<LinuxFileEntry> reader = new DataFileReader<>(file.toFile(), datumReader)) {
			LinuxFileEntry entry = null;
			while (reader.hasNext()) {
				entry = reader.next(entry);
				FileInfo info = toInfo(entry);
				index.add(info);
				byKey.computeIfAbsent(info.key(), k -> new ArrayList<>()).add(info);
			}
		}

		reconstructHardlinks(byKey);
		log.debug("Loaded index with {} entries from {}", index.values().size(), file);
		return index;
	}

	@Override
	public boolean exists(Path file) {
		return file != null && Files.isRegularFile(file);
	}

	private LinuxFileEntry toEntry(FileInfo info) {
		LinuxFileKey key = info.key();
		return new LinuxFileEntry(
			info.path().toString(),
			key.st_dev(),
			key.st_ino(),
			info.size(),
			info.modTimeSecond(),
			info.modTimeNano());
	}

	private FileInfo toInfo(LinuxFileEntry entry) {
		Path path = new File(entry.getPath().toString()).toPath();
		return new LinuxFileInfoImpl(
			path,
			entry.getStDev(),
			entry.getStIno(),
			entry.getSize(),
			entry.getMtimeSec(),
			entry.getMtimeNano());
	}

	private void reconstructHardlinks(Map<LinuxFileKey, List<FileInfo>> byKey) {
		for (List<FileInfo> group : byKey.values()) {
			if (group.size() < 2) {
				continue;
			}
			// Link every file in the group to the first one. hardlinkTo is bidirectional and idempotent.
			FileInfo first = group.get(0);
			for (int i = 1; i < group.size(); i++) {
				first.hardlinkTo(group.get(i));
			}
		}
	}

}
