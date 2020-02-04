package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.capabilities.MainCapability.*;
import static com.exasol.reflect.ReflectionUtils.getMethodReturnViaReflection;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.jdbc.ConnectionFactory;

@ExtendWith(MockitoExtension.class)
class RowLevelSecurityDialectTest {
    @Mock
    ConnectionFactory connectionFactoryMock;
    private RowLevelSecurityDialect dialect;

    @BeforeEach
    void beforeEach() throws SQLException {
        this.dialect = new RowLevelSecurityDialect(this.connectionFactoryMock, AdapterProperties.emptyProperties());
    }

    @Test
    void testGetName() {
        assertThat(this.dialect.getName(), equalTo("EXASOL_RLS"));
    }

    @Test
    void testCreateQueryRewriter(@Mock final Connection connectionMock, @Mock final DatabaseMetaData metadataMock,
            @Mock final ResultSet resultSetMock) throws SQLException {
        when(this.connectionFactoryMock.getConnection()).thenReturn(connectionMock);
        when(connectionMock.getMetaData()).thenReturn(metadataMock);
        when(metadataMock.getColumns(any(), any(), any(), any())).thenReturn(resultSetMock);
        assertThat(getMethodReturnViaReflection(this.dialect, "createQueryRewriter"),
                instanceOf(RowLevelSecurityQueryRewriter.class));
    }

    @Test
    void testCreateQueryRewriterThrowsException(@Mock final Connection connectionMock) throws SQLException {
        when(this.connectionFactoryMock.getConnection()).thenReturn(connectionMock);
        when(connectionMock.getMetaData()).thenThrow(SQLException.class);
        assertThrows(IllegalArgumentException.class, () -> this.dialect.createQueryRewriter());
    }

    @Test
    void testExasolSqlDialectSupportsAllCapabilities() {
        final Capabilities capabilities = this.dialect.getCapabilities();
        assertAll(
                () -> assertThat(capabilities.getMainCapabilities(),
                        containsInAnyOrder(SELECTLIST_PROJECTION, AGGREGATE_SINGLE_GROUP, AGGREGATE_GROUP_BY_COLUMN,
                                AGGREGATE_GROUP_BY_TUPLE, AGGREGATE_HAVING, ORDER_BY_COLUMN, LIMIT, LIMIT_WITH_OFFSET)),
                () -> assertThat(capabilities.getLiteralCapabilities(), containsInAnyOrder(LiteralCapability.values())),
                () -> assertThat(capabilities.getPredicateCapabilities(),
                        containsInAnyOrder(PredicateCapability.values())),
                () -> assertThat(capabilities.getScalarFunctionCapabilities(),
                        containsInAnyOrder(ScalarFunctionCapability.values())),
                () -> assertThat(capabilities.getAggregateFunctionCapabilities(),
                        containsInAnyOrder(AggregateFunctionCapability.values())));
    }
}