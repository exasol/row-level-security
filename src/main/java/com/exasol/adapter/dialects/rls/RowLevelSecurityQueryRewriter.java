package com.exasol.adapter.dialects.rls;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseQueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.exasol.ExasolQueryRewriter;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.sql.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RowLevelSecurityQueryRewriter extends ExasolQueryRewriter {
    /**
     * Create a new instance of a {@link BaseQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connection           JDBC connection to remote data source
     */
    public RowLevelSecurityQueryRewriter(SqlDialect dialect, RemoteMetadataReader remoteMetadataReader,
          Connection connection) {
        super(dialect, remoteMetadataReader, connection);
    }

    @Override
    public String rewrite(SqlStatement statement, ExaMetadata exaMetadata, AdapterProperties properties)
          throws AdapterException, SQLException {
        if (statement instanceof SqlStatementSelect) {
            final SqlStatementSelect select = (SqlStatementSelect) statement;
            final SqlStatementSelect.Builder rslStatementBuilder =
                  SqlStatementSelect.builder().selectList(select.getSelectList()).fromClause(select.getFromClause());
            if (select.hasGroupBy()) {
                rslStatementBuilder.groupBy(select.getGroupBy());
            }
            if (select.hasHaving()) {
                rslStatementBuilder.having(select.getHaving());
            }
            if (select.hasLimit()) {
                rslStatementBuilder.limit(select.getLimit());
            }
            if (select.hasOrderBy()) {
                rslStatementBuilder.orderBy(select.getOrderBy());
            }
            if (select.hasFilter()) {
                List<SqlNode> arguments = new ArrayList<>(2);
                arguments.add(getBitAndFunction());
                arguments.add(select.getWhereClause());
                SqlNode whereClause = new SqlPredicateAnd(arguments);
                rslStatementBuilder.whereClause(whereClause);
            } else {
                SqlNode whereClause = getBitAndFunction();
                rslStatementBuilder.whereClause(whereClause);
            }
            final SqlStatementSelect newSelectStatement = rslStatementBuilder.build();
            return super.rewrite(newSelectStatement, exaMetadata, properties);
        } else {
            throw new IllegalArgumentException(
                  "Modified SQL statement must be a select statement, but was " + statement.getClass().getName());
        }
    }

    private SqlNode getBitAndFunction() {
        List<SqlNode> arguments = new ArrayList<>(2);
        arguments.add(new SqlColumn(1,
              ColumnMetadata.builder().name("exa_row_roles").type(DataType.createDecimal(20, 0)).build()));
        arguments.add(new SqlLiteralExactnumeric(BigDecimal.valueOf(3)));
        return new SqlFunctionScalar(ScalarFunction.BIT_AND, arguments, true, false);
    }
}
