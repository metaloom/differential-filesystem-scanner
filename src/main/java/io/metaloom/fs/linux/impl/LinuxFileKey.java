package io.metaloom.fs.linux.impl;

import io.metaloom.fs.FileKey;

/**
 * Key which uniquely identifies a file. Please note that the ino of a file may change when a filesystem gets remounted. (e.g. when using Fat32).
 */
public record LinuxFileKey(long st_dev, long st_ino) implements FileKey {

	@Override
	public int hashCode() {
		return (int) (st_dev ^ (st_dev >>> 32)) +
			(int) (st_ino ^ (st_ino >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LinuxFileKey)) {
			return false;
		}
		LinuxFileKey other = (LinuxFileKey) obj;
		return (this.st_dev == other.st_dev) && (this.st_ino == other.st_ino);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(dev=")
			.append(Long.toHexString(st_dev))
			.append(",ino=")
			.append(st_ino)
			.append(')');
		return sb.toString();
	}

}
