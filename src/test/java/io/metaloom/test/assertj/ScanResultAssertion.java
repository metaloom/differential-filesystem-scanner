package io.metaloom.test.assertj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.assertj.core.api.AbstractAssert;

import io.metaloom.fs.ScanResult;

public class ScanResultAssertion extends AbstractAssert<ScanResultAssertion, ScanResult> {

	protected ScanResultAssertion(ScanResult actual) {
		super(actual, ScanResultAssertion.class);
	}

	public void hasResults(int nDeleted, int nMoved, int nModified, int nPresent, int nAdded) {
		assertEquals(nDeleted, actual.deleted().size(), "The deleted did not match");
		assertEquals(nMoved, actual.moved().size(), "The moves did not match");
		assertEquals(nModified, actual.modified().size(), "The modified did not match");
		assertEquals(nPresent, actual.present().size(), "The present did not match");
		assertEquals(nAdded, actual.added().size(), "The additions did not match");
	}

}
