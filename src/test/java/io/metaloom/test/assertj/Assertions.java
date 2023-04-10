package io.metaloom.test.assertj;

import io.metaloom.fs.ScanResult;

public class Assertions extends org.assertj.core.api.Assertions {

	public static ScanResultAssertion assertThat(ScanResult actual) {
		return new ScanResultAssertion(actual);
	}
}
