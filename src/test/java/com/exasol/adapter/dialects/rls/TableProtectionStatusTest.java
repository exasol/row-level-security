package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

import com.exasol.adapter.dialects.rls.TableProtectionStatus.Builder;

class TableProtectionStatusTest {
    private Builder builder;

    @BeforeEach
    void beforeEach() {
        this.builder = TableProtectionStatus.builder();
    }

    @Test
    void testBuildWithRoleProtectedTable() {
        final String table = "R";
        this.builder.addRoleProtectedTable(table);
        assertTableProtectionStatus(table, true, false);
    }

    private void assertTableProtectionStatus(final String table, final boolean roleProtected,
            final boolean tenantProtected) throws MultipleFailuresError {
        final TableProtectionStatus tableProtectionStatus = this.builder.build();
        assertAll(
                () -> assertThat("role-protected", tableProtectionStatus.isTableRoleProtected(table),
                        equalTo(roleProtected)),
                () -> assertThat("tenant-protected", tableProtectionStatus.isTableTenantProtected(table),
                        equalTo(tenantProtected)));
    }

    @Test
    void testBuildWithTenantProtectedTable() {
        final String table = "T";
        this.builder.addTenantProtectedTable(table);
        assertTableProtectionStatus(table, false, true);
    }

    @Test
    void testBuildWithRoleAndTenantProtectedTable() {
        final String table = "RT";
        this.builder.addRoleProtectedTable(table).addTenantProtectedTable(table);
        assertTableProtectionStatus(table, true, true);
    }
}