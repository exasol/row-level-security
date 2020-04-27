package com.exasol.adapter.dialects.rls;

/**
 * This parameter object stores details about how a table is protected.
 */
public class TableProtectionDetails {
    private final boolean roleProtected;
    private final boolean tenantProtected;
    private final boolean groupProtected;

    private TableProtectionDetails(final Builder builder) {
        this.roleProtected = builder.roleProtected;
        this.tenantProtected = builder.tenantProtected;
        this.groupProtected = builder.groupProtected;
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

    /**
     * Check whether the table is group-protected.
     *
     * @return {@code true} if the table is group-protected
     */
    public boolean isGroupProtected() {
        return this.groupProtected;
    }

    public boolean isProtected() {
        return this.roleProtected || this.tenantProtected || this.groupProtected;
    }

    /**
     * Describe the protection details in human readable form.
     *
     * @return description of the protection details
     */
    public String describe() {
        if (isTenantProtected()) {
            if (isRoleProtected()) {
                return "Protected by tenant and role.";
            } else if (isGroupProtected()) {
                return "Protected by tenant and group.";
            } else {
                return "Protected by tenant.";
            }
        } else if (isRoleProtected()) {
            return ("Protected by role.");
        } else if (isGroupProtected()) {
            return "Protected by group.";
        } else {
            return "Not protected.";
        }
    }

    /**
     * Create a builder for the {@link TableProtectionStatus}.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder for the {@link TableProtectionStatus} with a template to copy existing protection states from.
     *
     * @param template protection details template from with to copy the protection states
     *
     * @return builder instance
     */
    public static Builder builder(final TableProtectionDetails template) {
        return new Builder(template);
    }

    /**
     * Builder for {@link TableProtectionDetails}
     */
    public static final class Builder {
        private boolean roleProtected = false;
        private boolean tenantProtected = false;
        private boolean groupProtected = false;

        /**
         * Create a new instance of a {@link Builder} for the {@link TableProtectionDetails} with all protection schemes
         * disabled by default.
         */
        public Builder() {
        }

        /**
         * Create a new instance of a {@link Builder} for the {@link TableProtectionDetails} with all protection schemes
         * defaulting to the states given in the template.
         *
         * @param template protection details template from with to copy the protection states
         */
        public Builder(final TableProtectionDetails template) {
            this.roleProtected = template.roleProtected;
            this.tenantProtected = template.tenantProtected;
            this.groupProtected = template.groupProtected;
        }

        /**
         * Set whether the the table is role-protected or not.
         *
         * @param roleProtected set to {@code true} if the table is role-protected
         * @return {@link Builder} instance for fluent programming
         */
        public Builder roleProtected(final boolean roleProtected) {
            this.roleProtected = roleProtected;
            return this;
        }

        /**
         * Set whether the the table is tenant-protected or not.
         *
         * @param tenantProtected set to {@code true} if the table is tenant-protected
         * @return {@link Builder} instance for fluent programming
         */
        public Builder tenantProtected(final boolean tenantProtected) {
            this.tenantProtected = tenantProtected;
            return this;
        }

        /**
         * Set whether the the table is group-protected or not.
         *
         * @param groupProtected set to {@code true} if the table is group-protected
         * @return {@link Builder} instance for fluent programming
         */
        public Builder groupProtected(final boolean groupProtected) {
            this.groupProtected = groupProtected;
            return this;
        }

        /**
         * Build a new instance of {@link TableProtectionDetails}.
         *
         * @return new {@link TableProtectionDetails} instance
         */
        public TableProtectionDetails build() {
            return new TableProtectionDetails(this);
        }
    }
}