package com.exasol.adapter.dialects.rls;

import com.exasol.containers.ExasolDockerImageReference;

public class DBHelper {
    public static boolean exasolVersionSupportsFingerprintInAddress(ExasolDockerImageReference exasolDockerImageReference) {
        return (exasolDockerImageReference.getMajor() >= 7) && (exasolDockerImageReference.getMinor() >= 1);
    }
}
