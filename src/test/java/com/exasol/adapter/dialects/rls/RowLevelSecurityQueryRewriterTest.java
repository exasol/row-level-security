package com.exasol.adapter.dialects.rls;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.AbstractQueryRewriterTest;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.exasol.ExasolMetadataReader;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.metadata.DataType.ExaCharset;
import com.exasol.adapter.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static com.exasol.adapter.capabilities.MainCapability.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

class RowLevelSecurityQueryRewriterTest extends AbstractQueryRewriterTest {
    private SqlDialect dialect;

    @BeforeEach
    void beforeEach() {
        this.exaMetadata = mock(ExaMetadata.class);
        this.rawProperties = new HashMap<>();
        this.dialect = new RowLevelSecurityDialect(null, AdapterProperties.emptyProperties());
    }

    @Test
    void testExasolSqlDialectSupportsAllCapabilities() {
        final Capabilities capabilities = this.dialect.getCapabilities();
        assertAll(() -> assertThat(capabilities.getMainCapabilities(),
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

    @Test
    void testRewrite() throws SQLException, AdapterException {
        final Connection connectionMock = mockConnection();
        setConnectionNameProperty();
        this.statement = createSelectStatement().build();
        final AdapterProperties properties = new AdapterProperties(this.rawProperties);
        final RemoteMetadataReader metadataReader = new ExasolMetadataReader(connectionMock, properties);
        RowLevelSecurityQueryRewriter rewriter =
              new RowLevelSecurityQueryRewriter(dialect, metadataReader, connectionMock);
        assertThat(rewriter.rewrite(this.statement, this.exaMetadata, properties),
              containsString("STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE BIT_AND(\"exa_row_roles\", 3)'"));
    }

    @Test
    void testRewrite2() throws SQLException, AdapterException {
        final Connection connectionMock = mockConnection();
        setConnectionNameProperty();
        final SqlColumn left =
              new SqlColumn(1, ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        this.statement = createSelectStatement().whereClause(new SqlPredicateEqual(left, right)).build();
        final AdapterProperties properties = new AdapterProperties(this.rawProperties);
        final RemoteMetadataReader metadataReader = new ExasolMetadataReader(connectionMock, properties);
        RowLevelSecurityQueryRewriter rewriter =
              new RowLevelSecurityQueryRewriter(dialect, metadataReader, connectionMock);
        assertThat(rewriter.rewrite(this.statement, this.exaMetadata, properties), containsString(
              "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE (BIT_AND(\"exa_row_roles\", 3) "
                    + "AND \"amount\" = 2)'"));
    }

    @Test
    void testRewrite3() throws SQLException, AdapterException {
        final Connection connectionMock = mockConnection();
        setConnectionNameProperty();
        final SqlColumn left1 =
              new SqlColumn(1, ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right1 = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlPredicateEqual firstPartOfWhereClause = new SqlPredicateEqual(left1, right1);
        final SqlColumn left2 = new SqlColumn(2,
              ColumnMetadata.builder().name("item").type(DataType.createVarChar(100, ExaCharset.UTF8)).build());
        final SqlLiteralString right2 = new SqlLiteralString("Screwdriver");
        final SqlPredicateEqual secondPartOfWhereClause = new SqlPredicateEqual(left2, right2);
        final SqlPredicateAnd whereClause =
              new SqlPredicateAnd(List.of(firstPartOfWhereClause, secondPartOfWhereClause));

        this.statement = createSelectStatement().whereClause(whereClause).build();
        final AdapterProperties properties = new AdapterProperties(this.rawProperties);
        final RemoteMetadataReader metadataReader = new ExasolMetadataReader(connectionMock, properties);
        RowLevelSecurityQueryRewriter rewriter =
              new RowLevelSecurityQueryRewriter(dialect, metadataReader, connectionMock);
        assertThat(rewriter.rewrite(this.statement, this.exaMetadata, properties), containsString(
              "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE (BIT_AND(\"exa_row_roles\", 3) AND (\"amount\" = 2 "
                    + "AND \"item\" = ''Screwdriver''))'"));
    }

    private SqlStatementSelect.Builder createSelectStatement() {
        ColumnMetadata columnMetadata =
              ColumnMetadata.builder().name("amount").type(DataType.createDecimal(10, 0)).build();
        ColumnMetadata columnMetadata2 =
              ColumnMetadata.builder().name("item").type(DataType.createVarChar(20, ExaCharset.UTF8)).build();
        ColumnMetadata columnMetadata3 =
              ColumnMetadata.builder().name("exa_row_roles").type(DataType.createDecimal(20, 0)).build();
        TableMetadata tableMetadata =
              new TableMetadata("order_items", "", Arrays.asList(columnMetadata, columnMetadata2, columnMetadata3), "");
        SqlNode fromClause = new SqlTable("order_items", tableMetadata);
        SqlSelectList selectList = SqlSelectList.createRegularSelectList(List.of(new SqlColumn(2, columnMetadata2)));
        return SqlStatementSelect.builder().selectList(selectList).fromClause(fromClause);
    }
}