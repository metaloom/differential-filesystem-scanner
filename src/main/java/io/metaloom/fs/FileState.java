package io.metaloom.fs;

public enum FileState {

	/**
	 * File is present / unmodified
	 */
	PRESENT,

	/**
	 * File has been moved
	 */
	MOVED,

	/**
	 * File has been deleted
	 */
	DELETED,

	/**
	 * File has been modified
	 */
	MODIFIED,

	/**
	 * File is new / was not listed in the index
	 */
	NEW,

	/**
	 * File state is unknown / has not yet been validated by a scan
	 */
	UNKNOWN;

}
