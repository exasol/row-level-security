package com.exasol.adapter.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.sql.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInformationTest {
    private static final String DEFAULT_MASK_WITH_PUBLIC_VALUE = "9223372036854775808";
    @Mock
    private Connection connectionMock;
    @Mock
    private PreparedStatement preparedStatementMock;
    private UserInformation userInformation;

    @BeforeEach
    void beforeEach() throws SQLException {
        this.userInformation = new UserInformation("table", "schema", "sys");
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
    }

    @Test
    void testGetRoleMaskValidMask() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(resultSetMock.getLong(any())).thenReturn(3L);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(true);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(this.connectionMock), equalTo("9223372036854775811"));
    }

    @Test
    void testGetRoleMaskEmptyResultSet() throws SQLException {
        final UserInformation userInformation = new UserInformation("table", "schema", "sys");
        final ResultSet resultSet = null;
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSet);
        assertThat(userInformation.getRoleMask(this.connectionMock), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }

    @Test
    void testGetRoleMaskInvalidMaskMoreThanOneResult() throws SQLException {
        final UserInformation userInformation = new UserInformation("table", "schema", "sys");
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(resultSetMock.getLong(any())).thenReturn(3L);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(false);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(userInformation.getRoleMask(this.connectionMock), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }

    @Test
    void testGetRoleMaskInvalidMaskValue() throws SQLException {
        final UserInformation userInformation = new UserInformation("table", "schema", "sys");
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(resultSetMock.getLong(any()))
              .thenReturn(BigInteger.valueOf(2).pow(63).add(BigInteger.valueOf(1)).longValue());
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(true);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(userInformation.getRoleMask(this.connectionMock), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }
}