package com.exasol.adapter.sql;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.jdbc.ColumnMetadataReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInformationTest {
    @Mock
    private final ColumnMetadataReader columnMetadataReaderMock = mock(ColumnMetadataReader.class);
    @Mock
    private Connection connectionMock;
    @Mock
    private PreparedStatement preparedStatementMock;

    @Test
    void testGetRoleMask() throws SQLException {
        final UserInformation userInformation = new UserInformation();
        final ResultSet resultSetMock = mock(ResultSet.class);
        when(resultSetMock.getInt(any())).thenReturn(3);
        when(this.connectionMock.prepareStatement(any())).thenReturn(this.preparedStatementMock);
        when(this.preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        assertThat(userInformation.getRoleMask(this.connectionMock), equalTo(3));
    }
}