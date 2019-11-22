package com.exasol.adapter.sql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TableProtectionStatusTest {
    private TableProtectionStatus tableProtectionStatus;

    @Test
    void testIsTableSecuredWithRolesTrue() throws SQLException {
        final DatabaseMetaData metadataMock = Mockito.mock(DatabaseMetaData.class);
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        this.tableProtectionStatus = new TableProtectionStatus(metadataMock);
        assertAll(
                () -> assertThat(this.tableProtectionStatus.isTableProtectedWithExaRowRoles("", "", ""), equalTo(true)),
                () -> assertThat(this.tableProtectionStatus.isTableProtectedWithRowTenants("", "", ""), equalTo(true)));
    }

    @Test
    void testIsTableSecuredWithRolesFalse() throws SQLException {
        final DatabaseMetaData metadataMock = Mockito.mock(DatabaseMetaData.class);
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        this.tableProtectionStatus = new TableProtectionStatus(metadataMock);
        assertAll(
                () -> assertThat(this.tableProtectionStatus.isTableProtectedWithExaRowRoles("", "", ""),
                        equalTo(false)),
                () -> assertThat(this.tableProtectionStatus.isTableProtectedWithRowTenants("", "", ""),
                        equalTo(false)));
    }
}