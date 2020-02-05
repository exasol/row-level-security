package com.exasol.adapter.dialects.rls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TableProtectionStatusReaderTest {
    @Test
    void testIsTableSecuredWithRolesTrue() throws SQLException {
        final DatabaseMetaData metadataMock = Mockito.mock(DatabaseMetaData.class);
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        final String tableName = "TableA";
        when(resultSet.getString(3)).thenReturn(tableName);
        when(resultSet.getString(4)).thenReturn(RowLevelSecurityDialectConstants.EXA_ROW_ROLES_COLUMN_NAME);
        final TableProtectionStatus tableProtectionStatus = new TableProtectionStatusReader(metadataMock)
                .read("IrrelevantCatalog", "IrrelevantSchema");
        assertAll(() -> assertThat(tableProtectionStatus.isTableRoleProtected(tableName), equalTo(true)),
                () -> assertThat(tableProtectionStatus.isTableTenantProtected(tableName), equalTo(false)));
    }

    @Test
    void testIsTableSecuredWithRolesFalse() throws SQLException {
        final DatabaseMetaData metadataMock = Mockito.mock(DatabaseMetaData.class);
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        final TableProtectionStatus tableProtectionStatus = new TableProtectionStatusReader(metadataMock)
                .read("IrrelevantCatalog", "IrrelevantSchema");
        assertAll(() -> assertThat(tableProtectionStatus.isTableRoleProtected(""), equalTo(false)),
                () -> assertThat(tableProtectionStatus.isTableTenantProtected(""), equalTo(false)));
    }
}