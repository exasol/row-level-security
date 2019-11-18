package com.exasol.adapter.dialects.rls;

import java.math.BigInteger;

/**
 * This class contains constants used in Row Level Security dialect.
 */
public final class RowLevelSecurityDialectConstants {
    public static final long MAX_ROLE_VALUE = BigInteger.valueOf(2).pow(63).subtract(BigInteger.valueOf(1))
            .longValue();
    public static final long DEFAULT_ROLE_MASK = BigInteger.valueOf(2).pow(63).longValue();
    public static final String EXA_ROW_ROLES_COLUMN_NAME = "EXA_ROW_ROLES";
    public static final String EXA_ROW_TENANTS_COLUMN_NAME = "EXA_ROW_TENANTS";

    private RowLevelSecurityDialectConstants() {
        // prevent instantiation
    }
}