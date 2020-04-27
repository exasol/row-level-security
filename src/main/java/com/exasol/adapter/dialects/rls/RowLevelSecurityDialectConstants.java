package com.exasol.adapter.dialects.rls;

import java.math.BigInteger;

/**
 * This class contains constants used in Row Level Security dialect.
 */
public final class RowLevelSecurityDialectConstants {
    public static final long MAX_ROLE_VALUE = BigInteger.valueOf(2).pow(63).subtract(BigInteger.valueOf(1)).longValue();
    public static final long DEFAULT_ROLE_MASK = BigInteger.valueOf(2).pow(63).longValue();
    public static final String EXA_ROW_ROLES_COLUMN_NAME = "EXA_ROW_ROLES";
    public static final String EXA_ROW_TENANT_COLUMN_NAME = "EXA_ROW_TENANT";
    public static final String EXA_ROW_GROUP_COLUMN_NAME = "EXA_ROW_GROUP";
    public static final String EXA_RLS_USERS_TABLE_NAME = "EXA_RLS_USERS";
    public static final String EXA_ROLES_MAPPING_TABLE_NAME = "EXA_ROLES_MAPPING";

    private RowLevelSecurityDialectConstants() {
        // prevent instantiation
    }
}