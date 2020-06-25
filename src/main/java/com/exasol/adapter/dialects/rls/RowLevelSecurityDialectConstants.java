package com.exasol.adapter.dialects.rls;

import java.math.BigInteger;
import java.util.List;

import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.DataType.ExaCharset;

/**
 * This class contains constants used in Row Level Security dialect.
 */
public final class RowLevelSecurityDialectConstants {
    public static final long MAX_ROLE_VALUE = BigInteger.valueOf(2).pow(63).subtract(BigInteger.valueOf(1)).longValue();
    // [impl->dsn~public-access-role-id~1]]
    public static final long DEFAULT_ROLE_MASK = BigInteger.valueOf(2).pow(63).longValue();
    public static final String EXA_ROW_ROLES_COLUMN_NAME = "EXA_ROW_ROLES";
    public static final String EXA_ROW_TENANT_COLUMN_NAME = "EXA_ROW_TENANT";
    public static final String EXA_ROW_GROUP_COLUMN_NAME = "EXA_ROW_GROUP";
    public static final String EXA_RLS_USERS_TABLE_NAME = "EXA_RLS_USERS";
    public static final String EXA_ROLES_MAPPING_TABLE_NAME = "EXA_ROLES_MAPPING";
    public static final String EXA_GROUP_MEMBERS_TABLE_NAME = "EXA_GROUP_MEMBERS";
    public static final List<String> RLS_COLUMNS = List.of(EXA_ROW_ROLES_COLUMN_NAME, EXA_ROW_TENANT_COLUMN_NAME,
            EXA_ROW_GROUP_COLUMN_NAME);
    public static final List<String> RLS_METADATA_TABLES = List.of(EXA_RLS_USERS_TABLE_NAME,
            EXA_ROLES_MAPPING_TABLE_NAME, EXA_GROUP_MEMBERS_TABLE_NAME);
    public static final DataType IDENTIFIER_TYPE = DataType.createVarChar(128, ExaCharset.ASCII);
    public static final DataType MASK_TYPE = DataType.createDecimal(20, 0);

    private RowLevelSecurityDialectConstants() {
        // prevent instantiation
    }
}