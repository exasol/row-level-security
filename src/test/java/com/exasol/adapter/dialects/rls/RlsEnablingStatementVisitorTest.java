package com.exasol.adapter.dialects.rls;

import com.exasol.adapter.AdapterException;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.sql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class RlsEnablingStatementVisitorTest {
    private SqlNodeVisitor visitor;

    @BeforeEach
    void beforeEach() {
        this.visitor = new RlsEnablingStatementVisitor();
    }

    @Test
    void visitSqlStatementSelect() throws AdapterException {
        SqlStatementSelect sqlStatementSelect = createSelectStatement();
        SqlStatementSelect.Builder selectBuilder = (SqlStatementSelect.Builder) visitor.visit(sqlStatementSelect);
        assertAll(() -> assertThat(selectBuilder, instanceOf(SqlStatementSelect.Builder.class)),
              () -> assertThat(selectBuilder.build().getSelectList(), equalTo(sqlStatementSelect.getSelectList())),
              () -> assertThat(selectBuilder.build().getFromClause(), equalTo(sqlStatementSelect.getFromClause())),
              () -> assertThat(selectBuilder.build().getWhereClause(), equalTo(sqlStatementSelect.getWhereClause())),
              () -> assertThat(selectBuilder.build().getOrderBy(), equalTo(sqlStatementSelect.getOrderBy())),
              () -> assertThat(selectBuilder.build().getGroupBy(), equalTo(sqlStatementSelect.getGroupBy())),
              () -> assertThat(selectBuilder.build().getHaving(), equalTo(sqlStatementSelect.getHaving())),
              () -> assertThat(selectBuilder.build().getLimit(), equalTo(sqlStatementSelect.getLimit())),
              () -> assertThat(selectBuilder.build().getType(), equalTo(SqlNodeType.SELECT)));
    }

    private SqlStatementSelect createSelectStatement() {
        ColumnMetadata columnMetadata =
              ColumnMetadata.builder().name("amount").type(DataType.createDecimal(10, 0)).build();
        ColumnMetadata columnMetadata2 =
              ColumnMetadata.builder().name("item").type(DataType.createVarChar(20, DataType.ExaCharset.UTF8)).build();
        ColumnMetadata columnMetadata3 =
              ColumnMetadata.builder().name("exa_row_roles").type(DataType.createDecimal(20, 0)).build();
        TableMetadata tableMetadata =
              new TableMetadata("order_items", "", Arrays.asList(columnMetadata, columnMetadata2, columnMetadata3), "");
        SqlNode fromClause = new SqlTable("order_items", tableMetadata);
        SqlSelectList selectList = SqlSelectList.createRegularSelectList(List.of(new SqlColumn(2, columnMetadata2)));
        return SqlStatementSelect.builder().selectList(selectList).fromClause(fromClause).build();
    }

}