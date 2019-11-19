package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.AbstractQueryRewriterTestBase;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.exasol.ExasolMetadataReader;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.DataType.ExaCharset;
import com.exasol.adapter.metadata.TableMetadata;
import com.exasol.adapter.sql.*;

@ExtendWith(MockitoExtension.class)
class RowLevelSecurityQueryRewriterTest extends AbstractQueryRewriterTestBase {
    private SqlDialect dialect;
    private AdapterProperties properties;
    private RemoteMetadataReader metadataReader;
    @Mock
    private Connection connectionMock;
    @Mock
    private TableProtectionStatus tableProtectionStatusMock;

    @BeforeEach
    void beforeEach() throws SQLException {
        this.exaMetadata = mock(ExaMetadata.class);
        when(this.exaMetadata.getCurrentUser()).thenReturn("USER_1");
        this.rawProperties = new HashMap<>();
        this.properties = new AdapterProperties(this.rawProperties);
        this.dialect = new RowLevelSecurityDialect(this.connectionMock, this.properties);
        final ResultSetMetaData resultSetMetadataMock = mock(ResultSetMetaData.class);
        when(resultSetMetadataMock.getColumnCount()).thenReturn(1);
        when(resultSetMetadataMock.getColumnType(1)).thenReturn(4);
        final PreparedStatement preparedStatementMock = mock(PreparedStatement.class);
        when(preparedStatementMock.getMetaData()).thenReturn(resultSetMetadataMock);
        final ResultSet resultSetMock = mock(ResultSet.class);
        lenient().when(resultSetMock.getLong(any())).thenReturn(3L);
        lenient().when(resultSetMock.next()).thenReturn(true);
        lenient().when(resultSetMock.last()).thenReturn(true);
        lenient().when(preparedStatementMock.executeQuery()).thenReturn(resultSetMock);
        when(this.connectionMock.prepareStatement(ArgumentMatchers.any())).thenReturn(preparedStatementMock);
        setConnectionNameProperty();
        this.metadataReader = new ExasolMetadataReader(this.connectionMock, this.properties);
    }

    @Test
    void testRewriteUnprotectedTables() throws SQLException, AdapterException {
        final SqlColumn left = new SqlColumn(1,
                ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlStatementSelect statement = createSelectStatement().whereClause(new SqlPredicateEqual(left, right))
                .build();
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(false);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(false);
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties),
                containsString("STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE \"amount\" = 2'"));
    }

    @Test
    void testRewriteRolesWithoutWhereClause() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(true);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(false);
        final SqlStatementSelect statement = createSelectStatement().build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE BIT_AND(\"EXA_ROW_ROLES\", 9223372036854775811) <> 0'"));
    }

    @Test
    void testRewriteTenantsWithoutWhereClause() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(false);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(true);
        final SqlStatementSelect statement = createSelectStatement().build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE \"EXA_ROW_TENANTS\" = ''USER_1'''"));
    }

    @Test
    void testRewriteRolesWithSimpleWhereClause() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(true);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(false);
        final SqlColumn left = new SqlColumn(1,
                ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlStatementSelect statement = createSelectStatement().whereClause(new SqlPredicateEqual(left, right))
                .build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE (\"amount\" = 2 AND BIT_AND(\"EXA_ROW_ROLES\", 9223372036854775811) <> 0)'"));
    }

    @Test
    void testRewriteTenantsWithSimpleWhereClause() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(false);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(true);
        final SqlColumn left = new SqlColumn(1,
                ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlStatementSelect statement = createSelectStatement().whereClause(new SqlPredicateEqual(left, right))
                .build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE (\"amount\" = 2 AND \"EXA_ROW_TENANTS\" = ''USER_1'')'"));
    }

    @Test
    void testRewriteRolesWithWhereClauseWithMultipleValues() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(true);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(false);
        final SqlColumn left1 = new SqlColumn(1,
                ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right1 = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlPredicateEqual firstPartOfWhereClause = new SqlPredicateEqual(left1, right1);
        final SqlColumn left2 = new SqlColumn(2,
                ColumnMetadata.builder().name("item").type(DataType.createVarChar(100, ExaCharset.UTF8)).build());
        final SqlLiteralString right2 = new SqlLiteralString("Screwdriver");
        final SqlPredicateEqual secondPartOfWhereClause = new SqlPredicateEqual(left2, right2);
        final SqlPredicateAnd whereClause = new SqlPredicateAnd(
                List.of(firstPartOfWhereClause, secondPartOfWhereClause));
        final SqlStatementSelect statement = createSelectStatement().whereClause(whereClause).build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE ((\"amount\" = 2 AND \"item\" = ''Screwdriver'') AND BIT_AND(\"EXA_ROW_ROLES\", 9223372036854775811) <> 0)'"));
    }

    @Test
    void testRewriteTenantsWithWhereClauseWithMultipleValues() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(false);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(true);
        final SqlColumn left1 = new SqlColumn(1,
                ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right1 = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlPredicateEqual firstPartOfWhereClause = new SqlPredicateEqual(left1, right1);
        final SqlColumn left2 = new SqlColumn(2,
                ColumnMetadata.builder().name("item").type(DataType.createVarChar(100, ExaCharset.UTF8)).build());
        final SqlLiteralString right2 = new SqlLiteralString("Screwdriver");
        final SqlPredicateEqual secondPartOfWhereClause = new SqlPredicateEqual(left2, right2);
        final SqlPredicateAnd whereClause = new SqlPredicateAnd(
                List.of(firstPartOfWhereClause, secondPartOfWhereClause));
        final SqlStatementSelect statement = createSelectStatement().whereClause(whereClause).build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE ((\"amount\" = 2 AND \"item\" = ''Screwdriver'') AND \"EXA_ROW_TENANTS\" = ''USER_1'')'"));
    }

    @Test
    void testRewriteTenantsAndRolesWithWhereClauseWithMultipleValues() throws SQLException, AdapterException {
        when(this.tableProtectionStatusMock.isTableProtectedWithExaRowRoles(any(), any(), any())).thenReturn(true);
        when(this.tableProtectionStatusMock.isTableProtectedWithRowTenants(any(), any(), any())).thenReturn(true);
        final SqlColumn left1 = new SqlColumn(1,
                ColumnMetadata.builder().name("amount").type(DataType.createDecimal(20, 0)).build());
        final SqlLiteralExactnumeric right1 = new SqlLiteralExactnumeric(BigDecimal.valueOf(2));
        final SqlPredicateEqual firstPartOfWhereClause = new SqlPredicateEqual(left1, right1);
        final SqlColumn left2 = new SqlColumn(2,
                ColumnMetadata.builder().name("item").type(DataType.createVarChar(100, ExaCharset.UTF8)).build());
        final SqlLiteralString right2 = new SqlLiteralString("Screwdriver");
        final SqlPredicateEqual secondPartOfWhereClause = new SqlPredicateEqual(left2, right2);
        final SqlPredicateAnd whereClause = new SqlPredicateAnd(
                List.of(firstPartOfWhereClause, secondPartOfWhereClause));
        final SqlStatementSelect statement = createSelectStatement().whereClause(whereClause).build();
        final RowLevelSecurityQueryRewriter rewriter = new RowLevelSecurityQueryRewriter(this.dialect,
                this.metadataReader, this.connectionMock, this.tableProtectionStatusMock);
        assertThat(rewriter.rewrite(statement, this.exaMetadata, this.properties), containsString(
                "STATEMENT 'SELECT \"item\" FROM \"order_items\" WHERE ((\"amount\" = 2 AND \"item\" = ''Screwdriver'') AND BIT_AND(\"EXA_ROW_ROLES\", 9223372036854775811) <> 0 AND \"EXA_ROW_TENANTS\" = ''USER_1'')'"));
    }

    private SqlStatementSelect.Builder createSelectStatement() {
        final ColumnMetadata columnMetadata = ColumnMetadata.builder().name("amount")
                .type(DataType.createDecimal(10, 0)).build();
        final ColumnMetadata columnMetadata2 = ColumnMetadata.builder().name("item")
                .type(DataType.createVarChar(20, ExaCharset.UTF8)).build();
        final ColumnMetadata columnMetadata3 = ColumnMetadata.builder().name("EXA_ROW_ROLES")
                .type(DataType.createDecimal(20, 0)).build();
        final TableMetadata tableMetadata = new TableMetadata("order_items", "",
                Arrays.asList(columnMetadata, columnMetadata2, columnMetadata3), "");
        final SqlNode fromClause = new SqlTable("order_items", tableMetadata);
        final SqlSelectList selectList = SqlSelectList
                .createRegularSelectList(List.of(new SqlColumn(2, columnMetadata2)));
        return SqlStatementSelect.builder().selectList(selectList).fromClause(fromClause);
    }
}