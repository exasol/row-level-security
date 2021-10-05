package com.exasol.tools;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class FingerprintExtractorTest {

    @Test
    void testExtractFingerprintWithCertificateValidationOff() {
        ExceptionAssertions.assertThrowsWithMessage(IllegalArgumentException.class,
                () -> FingerprintExtractor.extractFingerprint("jdbc:exa:localhost:49160;validateservercertificate=0"),
                "JDBC URL 'jdbc:exa:localhost:49160;validateservercertificate=0' does not validate the certificate");
    }

    @Test
    void testExtractFingerprintFromLocalhostUrl() {
        assertThat(FingerprintExtractor.extractFingerprint(
                "jdbc:exa:localhost/fingerprint:1234;validateservercertificate=1"), equalTo("fingerprint"));
    }

    @Test
    void testExtractFingerprint() {
        assertThat(FingerprintExtractor.extractFingerprint(
                "jdbc:exa:127.0.0.1/fingerprint:1234;validateservercertificate=1"), equalTo("fingerprint"));
    }

    @Test
    void testExtractFingerprintFailed() {
        ExceptionAssertions.assertThrowsWithMessage(IllegalStateException.class,
                () -> FingerprintExtractor.extractFingerprint("jdbc:exa:127.0.0.1:1234;validateservercertificate=1"),
                "Error extracting fingerprint from 'jdbc:exa:127.0.0.1:1234;validateservercertificate=1'");
    }
}
