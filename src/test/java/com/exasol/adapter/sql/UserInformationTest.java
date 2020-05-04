package com.exasol.adapter.sql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.jdbc.ConnectionFactory;

@ExtendWith(MockitoExtension.class)
class UserInformationTest {
    private static final String DEFAULT_MASK_WITH_PUBLIC_VALUE = "9223372036854775808";
    public static final BigInteger MAX_ALLOWED_VALUE = BigInteger.valueOf(2).pow(63);
    @Mock
    private ConnectionFactory connectionFactoryMock;
    @Mock
    private Connection connectionMock;
    @Mock
    private PreparedStatement preparedStatementMock;
    private UserInformation userInformation;

    @BeforeEach
    void beforeEach() throws SQLException {
        when(this.connectionFactoryMock.getConnection()).thenReturn(this.connectionMock);
        this.userInformation = new UserInformation("sys", "schema", this.connectionFactoryMock);
    }

    @Test
    void testGetRoleMaskValidMask() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(resultSetMock.getLong(any())).thenReturn(3L);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(true);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(), equalTo("9223372036854775811"));
    }

    @Test
    void testGetRoleMaskEmptyResultSetWithDefaultMask() throws SQLException {
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(this.preparedStatementMock.executeQuery()).thenReturn(null);
        assertThat(this.userInformation.getRoleMask(), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }

    @Test
    void testGetRoleMaskInvalidMaskMoreThanOneResult() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(resultSetMock.getLong(any())).thenReturn(3L);
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(false);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }

    @Test
    void testGetRoleMaskInvalidMaskValue() throws SQLException {
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(resultSetMock.getLong(any())).thenReturn(MAX_ALLOWED_VALUE.add(BigInteger.valueOf(1)).longValue());
        when(resultSetMock.next()).thenReturn(true);
        when(resultSetMock.last()).thenReturn(true);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(this.userInformation.getRoleMask(), equalTo(DEFAULT_MASK_WITH_PUBLIC_VALUE));
    }
}