package com.exasol.adapter.dialects.rls;

import java.util.Map.Entry;

/**
 * (De-)serialize protection status of one or more tables.
 */
public class TableProtectionStatusSerializer {
    /**
     * Convert the protection status of tables into text form.
     *
     * @param tableProtectionStatus the protection status to be serialized.
     * @return serialized protection status.
     */
    public String serialize(final TableProtectionStatus tableProtectionStatus) {
        final StringBuilder builder = new StringBuilder();
        boolean needsSeparator = false;
        for (final Entry<String, TableProtectionDetails> protectedTable : tableProtectionStatus.getProtectedTables()
                .entrySet()) {
            final String tableName = protectedTable.getKey();
            final TableProtectionDetails protection = protectedTable.getValue();
            builder.append(needsSeparator ? "\n" : "");
            builder.append(tableName);
            builder.append(":");
            builder.append(protection.isRoleProtected() ? "r" : "-");
            builder.append(protection.isTenantProtected() ? "t" : "-");
            needsSeparator = true;
        }
        return builder.toString();
    }

    /**
     * Convert a text representation into table protection status.
     *
     * @param serializedStatus text representation
     * @return projection status
     */
    public TableProtectionStatus deserialize(final String serializedStatus) {
        final TableProtectionStatus.Builder builder = TableProtectionStatus.builder();
        for (final String serializedDetail : serializedStatus.split("\n")) {
            final String[] parts = serializedDetail.split(":");
            assert (parts.length == 2);
            final String tableName = parts[0];
            final String flags = parts[1];
            assert (flags.length() == 2);
            final boolean roleProtected = flags.contains("r");
            final boolean tenantProtected = flags.contains("t");
            builder.addTable(tableName, roleProtected, tenantProtected);
        }
        return builder.build();
    }
}