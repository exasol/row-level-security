package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        final String tableName = "R";
        this.builder.addRoleProtectedTable(tableName);
        assertTableProtectionStatus(tableName, true, false, false);
    }

    private void assertTableProtectionStatus(final String tableName, final boolean roleProtected,
            final boolean tenantProtected, final boolean groupProtected) throws MultipleFailuresError {
        final TableProtectionStatus tableProtectionStatus = this.builder.build();
        assertAll(
                () -> assertThat("role-protected", tableProtectionStatus.isTableRoleProtected(tableName),
                        equalTo(roleProtected)),
                () -> assertThat("tenant-protected", tableProtectionStatus.isTableTenantProtected(tableName),
                        equalTo(tenantProtected)),
                () -> assertThat("group-protected", tableProtectionStatus.isTableGroupProtected(tableName),
                        equalTo(groupProtected)));
    }

    @Test
    void testBuildWithTenantProtectedTable() {
        final String tableName = "T";
        this.builder.addTenantProtectedTable(tableName);
        assertTableProtectionStatus(tableName, false, true, false);
    }

    @Test
    void testBuildWithRoleAndTenantProtectedTable() {
        final String tableName = "RT";
        this.builder.addRoleProtectedTable(tableName).addTenantProtectedTable(tableName);
        assertTableProtectionStatus(tableName, true, true, false);
    }

    @Test
    void testBuildWithGroupProtectedTable() {
        final String tableName = "G";
        this.builder.addGroupProtectedTable(tableName);
        assertTableProtectionStatus(tableName, false, false, true);
    }

    @CsvSource({ "true,false,false", //
            "false,true,false", //
            "false,false,true", //
            "true,true,false", //
            "true,false,true" })
    @ParameterizedTest
    void testGetTableProtectionStatus(final boolean roleProtected, final boolean tenantProtected,
            final boolean groupProtected) {
        final String tableName = "table";
        if (roleProtected) {
            this.builder.addRoleProtectedTable(tableName);
        }
        if (tenantProtected) {
            this.builder.addTenantProtectedTable(tableName);
        }
        if (groupProtected) {
            this.builder.addGroupProtectedTable(tableName);
        }
        final TableProtectionDetails details = this.builder.build().getTableProtectionDetails(tableName);
        assertAll(() -> assertThat("role-protected", details.isRoleProtected(), equalTo(roleProtected)),
                () -> assertThat("tenant-protected", details.isTenantProtected(), equalTo(tenantProtected)),
                () -> assertThat("group-protected", details.isGroupProtected(), equalTo(groupProtected)));
    }

    @Test
    void testGetProtectionStatusReturnsUnprotectedForTableThatIsNotListed() {
        final String unprotectedTableName = "UnprotectedTable";
        this.builder.addGroupProtectedTable("ProtectedA");
        this.builder.addRoleProtectedTable("ProtectedA");
        assertThat(this.builder.build().getTableProtectionDetails(unprotectedTableName).isProtected(), equalTo(false));
    }

    @CsvSource({ //
            "false, false, false, false", //
            "true , false, false, true", //
            "false, true , false, true", //
            "false, false, true , true", //
            "true , true , false, true", //
            "false, true , true , true" })
    @ParameterizedTest
    void testIsTableProtected(final boolean roleProtected, final boolean tenantProtected, final boolean groupProtected,
            final boolean expectedGenerallyProtected) {
        final String tableName = "combinationtable";
        if (roleProtected) {
            this.builder.addRoleProtectedTable(tableName);
        }
        if (tenantProtected) {
            this.builder.addTenantProtectedTable(tableName);
        }
        if (groupProtected) {
            this.builder.addGroupProtectedTable(tableName);
        }
        assertThat(
                "Protected by role: " + roleProtected + ", tenant: " + tenantProtected + ", group: " + groupProtected
                        + " is generally protected?",
                this.builder.build().isTableProtected(tableName), equalTo(expectedGenerallyProtected));
    }
}