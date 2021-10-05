package com.exasol.tools;

import java.util.regex.Pattern;

/**
 * This class provides a method for extracting the fingerprint from an Exasol JDBC URL.
 */
public class FingerprintExtractor {

    private FingerprintExtractor() {
        // not instantiable
    }

    /**
     * Extract the fingerprint from an Exasol JDBC URL.
     *
     * @param jdbcUrl JDBC URL with a fingerprint
     * @return fingerprint
     * @throws IllegalStateException if the URL has an invalid format or does not contain a fingerprint
     */
    public static String extractFingerprint(final String jdbcUrl) {
        if (jdbcUrl.contains("validateservercertificate=0")) {
            throw new IllegalArgumentException("JDBC URL '" + jdbcUrl + "' does not validate the certificate");
        }
        final java.util.regex.Matcher matcher = Pattern.compile("jdbc:exa:[^/]+/([^:]+):.*").matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalStateException("Error extracting fingerprint from '" + jdbcUrl + "'");
        }
        return matcher.group(1);
    }
}
