package com.exasol.adapter.sql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserInformationTest {
    private static final String DEFAULT_MASK_WITH_PUBLIC_VALUE = "9223372036854775808";
    @Mock
    private Connection connectionMock;
    @Mock
    private PreparedStatement preparedStatementMock;
    private UserInformation userInformation;

    @BeforeEach
    void beforeEach() {
        this.userInformation = new UserInformation("sys", "schema", "table");
    }

    @Test
    void testIsTableSecuredWithRolesTrue() throws SQLException {
        final DatabaseMetaData metadataMock = Mockito.mock(DatabaseMetaData.class);
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        assertAll(
                () -> assertThat(this.userInformation.isTableProtectedWithExaRowRoles("", "", metadataMock),
                        equalTo(true)),
                () -> assertThat(this.userInformation.isTableProtectedWithRowTenants("", "", metadataMock),
                        equalTo(true)));
    }

    @Test
    void testIsTableSecuredWithRolesFalse() throws SQLException {
        final DatabaseMetaData metadataMock = Mockito.mock(DatabaseMetaData.class);
        final ResultSet resultSet = Mockito.mock(ResultSet.class);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        assertAll(
                () -> assertThat(this.userInformation.isTableProtectedWithExaRowRoles("", "", metadataMock),
                        equalTo(false)),
                () -> assertThat(this.userInformation.isTableProtectedWithRowTenants("", "", metadataMock),
                        equalTo(false)));
    }

    @Test
    void testGetRoleMaskValidMask() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(resultSetMock.getLong(any())).thenReturn(3L);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(true);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(this.connectionMock), equalTo("9223372036854775811"));
    }

    @Test
    void testGetRoleMaskEmptyResultSetWithDefaultMask() throws SQLException {
        final ResultSet resultSet = null;
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSet);
        assertThat(this.userInformation.getRoleMask(this.connectionMock), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }

    @Test
    void testGetRoleMaskInvalidMaskMoreThanOneResult() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(resultSetMock.getLong(any())).thenReturn(3L);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(false);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(this.connectionMock), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }

    @Test
    void testGetRoleMaskInvalidMaskValue() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(resultSetMock.getLong(any()))
                .thenReturn(BigInteger.valueOf(2).pow(63).add(BigInteger.valueOf(1)).longValue());
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(true);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(this.connectionMock), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }
}