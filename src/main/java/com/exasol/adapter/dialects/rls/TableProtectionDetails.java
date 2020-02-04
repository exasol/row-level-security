package com.exasol.adapter.dialects.rls;

/**
 * This parameter object stores details about how a table is protected.
 */
public class TableProtectionDetails {
    private final boolean roleProtected;
    private final boolean tenantProtected;

    /**
     * Create a new instance of the {@link TableProtectionDetails}.
     *
     * @param roleProtected   {@code true} if the table is role-protected
     * @param tenantProtected {@code true} if the table is tenant-protected
     */
    public TableProtectionDetails(final boolean roleProtected, final boolean tenantProtected) {
        this.roleProtected = roleProtected;
        this.tenantProtected = tenantProtected;
    }

    /**
     * Check whether the table is role-protected.
     *
     * @return {@code true} if the table is role-protected
     */
    public boolean isRoleProtected() {
        return this.roleProtected;
    }

    /**
     * Check whether the table is tenant-protected.
     *
     * @return {@code true} if the table is tenant-protected
     */
    public boolean isTenantProtected() {
        return this.tenantProtected;
    }
}