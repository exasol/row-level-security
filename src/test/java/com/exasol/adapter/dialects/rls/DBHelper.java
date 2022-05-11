package com.exasol.adapter.dialects.rls;

import com.exasol.containers.ExasolDockerImageReference;

public class DBHelper {
    public static boolean exasolVersionSupportsFingerprintInAddress(ExasolDockerImageReference exasolDockerImageReference) {
        final ExasolDockerImageReference imageReference = exasolDockerImageReference;//
        return (imageReference.getMajor() >= 7) && (imageReference.getMinor() >= 1);
    }
}
