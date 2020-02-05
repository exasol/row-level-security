package com.exasol.adapter.dialects.rls;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides information about tables' protection.
 */
public class TableProtectionStatus {
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
     * Check if a table is protected with roles security.
     *
     * @param tableName name of the table to check
     * @return true if protected
     */
    public boolean isTableRoleProtected(final String tableName) {
        return this.getProtectedTables().containsKey(tableName)
                && this.getProtectedTables().get(tableName).isRoleProtected();
    }

    /**
     * Check if a table is protected with tenants security.
     *
     * @param tableName name of the table to check
     * @return true if protected
     */
    public boolean isTableTenantProtected(final String tableName) {
        return this.getProtectedTables().containsKey(tableName)
                && this.getProtectedTables().get(tableName).isTenantProtected();
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
         * Add a table to the list of protected tables.
         *
         * @param tableName       name of the protected table
         * @param roleProtected   {@code true} if the table is role-protected
         * @param tenantProtected {@code true} if the table is tenant-protected
         * @return builder instance for fluent programming
         */
        public Builder addTable(final String tableName, final boolean roleProtected, final boolean tenantProtected) {
            this.protectedTables.put(tableName, new TableProtectionDetails(roleProtected, tenantProtected));
            return this;
        }

        /**
         * Add a table that is role-protected.
         *
         * @param tableName name of the protected table
         * @return builder instance for fluent programming
         */
        public Builder addRoleProtectedTable(final String tableName) {
            final TableProtectionDetails oldValue = this.protectedTables.get(tableName);
            final boolean tenantProtected = (oldValue != null) && oldValue.isTenantProtected();
            this.protectedTables.put(tableName, new TableProtectionDetails(true, tenantProtected));
            return this;
        }

        /**
         * Add a table that is tenant-protected.
         *
         * @param tableName name of the protected table
         * @return builder instance for fluent programming
         */
        public Builder addTenantProtectedTable(final String tableName) {
            final TableProtectionDetails oldValue = this.protectedTables.get(tableName);
            final boolean roleProtected = (oldValue != null) && oldValue.isRoleProtected();
            this.protectedTables.put(tableName, new TableProtectionDetails(roleProtected, true));
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