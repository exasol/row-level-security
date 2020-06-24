package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TableProtectionStatusReaderTest {
    private static final String PROTECTED_TABLE_NAME = "TableA";

    // [utest->dsn~table-protection-status-reader-identifies-protected-tables~2]
    // [utest->dsn~table-protection-status-reader-identifies-unprotected-tables~2]
    @CsvSource({ //
            EXA_ROW_ROLES_COLUMN_NAME + ", true, false, false", //
            EXA_ROW_TENANT_COLUMN_NAME + ", false, true, false", //
            EXA_ROW_GROUP_COLUMN_NAME + ", false, false, true", //
            "NOT_A_PROTECTION_COLUMN, false, false, false" //
    })
    @ParameterizedTest
    void testIsTableXProteced(final String protectionColumnName, final boolean roleProtected,
            final boolean tenantProtected, final boolean groupProtected) throws SQLException {
        final ResultSet resultSet = mockColumnMetadataWithProtectionColumn(protectionColumnName);
        final TableProtectionStatus status = readProtectionStatusFromMockedMetadata(resultSet);
        assertAll(
                () -> assertThat("role-protected", status.isTableRoleProtected(PROTECTED_TABLE_NAME),
                        equalTo(roleProtected)),
                () -> assertThat("tenant-protected", status.isTableTenantProtected(PROTECTED_TABLE_NAME),
                        equalTo(tenantProtected)),
                () -> assertThat("group-protected", status.isTableGroupProtected(PROTECTED_TABLE_NAME),
                        equalTo(groupProtected)));
    }

    private ResultSet mockColumnMetadataWithProtectionColumn(final String protectionColumnName) throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(3)).thenReturn(PROTECTED_TABLE_NAME);
        when(resultSet.getString(4)).thenReturn(protectionColumnName);
        return resultSet;
    }

    private TableProtectionStatus readProtectionStatusFromMockedMetadata(final ResultSet resultSet)
            throws SQLException {
        final DatabaseMetaData metadataMock = mock(DatabaseMetaData.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        final TableProtectionStatus tableProtectionStatus = new TableProtectionStatusReader(metadataMock)
                .read("IrrelevantCatalog", "IrrelevantSchema");
        return tableProtectionStatus;
    }

    @Test
    void testIsTableSecuredWithRolesFalse() throws SQLException {
        final ResultSet resultSet = mockColumnMetadataForNoProtectedTables();
        final TableProtectionStatus status = readProtectionStatusFromMockedMetadata(resultSet);
        assertAll(() -> assertThat("role-protected", status.isTableRoleProtected(""), equalTo(false)),
                () -> assertThat("tenant-protected", status.isTableTenantProtected(""), equalTo(false)),
                () -> assertThat("group-protected", status.isTableGroupProtected(""), equalTo(false)));
    }

    private ResultSet mockColumnMetadataForNoProtectedTables() throws SQLException {
        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);
        return resultSet;
    }
}