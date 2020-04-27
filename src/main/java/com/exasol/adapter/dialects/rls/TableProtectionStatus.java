package com.exasol.adapter.dialects.rls;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides information about tables' protection.
 */
// [impl->dsn~table-protection-status~2]
public class TableProtectionStatus {
    private static final TableProtectionDetails UNPROTECTED_TABLE_DETAILS = TableProtectionDetails.builder().build();
    private final Map<String, TableProtectionDetails> protectedTables;

    /**
     * Create a new instance of {@link TableProtectionStatus}.
     *
     * @param builder builder for {@link TableProtectionStatus}
     */
    public TableProtectionStatus(final Builder builder) {
        this.protectedTables = builder.protectedTables;
    }

    /**
     * Get the protection details for a specific table.
     *
     * @param tableName name of the table for which the protection status is checked
     * @return protection details
     */
    public TableProtectionDetails getTableProtectionDetails(final String tableName) {
        if (this.protectedTables.containsKey(tableName)) {
            return this.protectedTables.get(tableName);
        } else {
            return UNPROTECTED_TABLE_DETAILS;
        }
    }

    /**
     * Check if a table is protected with roles security.
     *
     * @param tableName name of the table to check
     * @return {@code true} if protected
     */
    public boolean isTableRoleProtected(final String tableName) {
        return this.getProtectedTables().containsKey(tableName)
                && this.getProtectedTables().get(tableName).isRoleProtected();
    }

    /**
     * Check if a table is protected with tenants security.
     *
     * @param tableName name of the table to check
     * @return {@code true} if protected
     */
    public boolean isTableTenantProtected(final String tableName) {
        return this.getProtectedTables().containsKey(tableName)
                && this.getProtectedTables().get(tableName).isTenantProtected();
    }

    /**
     * Check if a table is protected with group security.
     *
     * @param tableName name of the table to check
     * @return {@code true} if protected
     */
    public boolean isTableGroupProtected(final String tableName) {
        return this.getProtectedTables().containsKey(tableName)
                && this.getProtectedTables().get(tableName).isGroupProtected();
    }

    /**
     * Check if a table is protected with any kind of RLS security.
     *
     * @param tableName name of the table to check
     * @return {@code true} if protected
     */
    public boolean isTableProtected(final String tableName) {
        return isTableRoleProtected(tableName) || isTableTenantProtected(tableName) || isTableGroupProtected(tableName);
    }

    /**
     * Get a map of all protected tables and their protection details.
     *
     * @return map of protected tables.
     */
    public Map<String, TableProtectionDetails> getProtectedTables() {
        return this.protectedTables;
    }

    /**
     * Create a builder for {@link TableProtectionStatus} objects.
     *
     * @return new {@link TableProtectionStatus} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TableProtectionStatus} objects.
     */
    public static class Builder {
        private final Map<String, TableProtectionDetails> protectedTables = new HashMap<>();

        /**
         * Add a table that is role-protected.
         *
         * @param tableName name of the protected table
         * @return builder instance for fluent programming
         */
        public Builder addRoleProtectedTable(final String tableName) {
            this.protectedTables.put(tableName, getProtectionDetailsBuilder(tableName).roleProtected(true).build());
            return this;
        }

        private TableProtectionDetails.Builder getProtectionDetailsBuilder(final String tableName) {
            return this.protectedTables.containsKey(tableName)
                    ? TableProtectionDetails.builder(this.protectedTables.get(tableName))
                    : TableProtectionDetails.builder();
        }

        /**
         * Add a table that is tenant-protected.
         *
         * @param tableName name of the protected table
         * @return builder instance for fluent programming
         */
        public Builder addTenantProtectedTable(final String tableName) {
            this.protectedTables.put(tableName, getProtectionDetailsBuilder(tableName).tenantProtected(true).build());
            return this;
        }

        /**
         * Add a table that is group-protected.
         *
         * @param tableName name of the protected table
         * @return builder instance for fluent programming
         */
        public Builder addGroupProtectedTable(final String tableName) {
            this.protectedTables.put(tableName, getProtectionDetailsBuilder(tableName).groupProtected(true).build());
            return this;
        }

        /**
         * Build a new instance of a {@link TableProtectionStatus}.
         *
         * @return new instance
         */
        public TableProtectionStatus build() {
            return new TableProtectionStatus(this);
        }
    }
}