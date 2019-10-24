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
    public RowLevelSecurityQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
          final Connection connection) {
        super(dialect, remoteMetadataReader, connection);
    }

    @Override
    public String rewrite(final SqlStatement statement, final ExaMetadata exaMetadata,
          final AdapterProperties properties) throws AdapterException, SQLException {
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
            this.applyWhereClause(select, rslStatementBuilder);
            final SqlStatementSelect newSelectStatement = rslStatementBuilder.build();
            return super.rewrite(newSelectStatement, exaMetadata, properties);
        } else {
            throw new IllegalArgumentException(
                  "Modified SQL statement must be a select statement, but was " + statement.getClass().getName());
        }
    }

    private void applyWhereClause(final SqlStatementSelect select,
          final SqlStatementSelect.Builder rslStatementBuilder) {
        final UserInformation userInformation = new UserInformation();
//        final int exaRoleMask = userInformation.getRoleMask(this.connection);
        final int exaRoleMask = 3;
        if (select.hasFilter()) {
            final SqlNode left = this.getBitAndFunction(exaRoleMask);
            final SqlNode right = select.getWhereClause();
            final List<SqlNode> arguments = List.of(left, right);
            final SqlNode whereClause = new SqlPredicateAnd(arguments);
            rslStatementBuilder.whereClause(whereClause);
        } else {
            final SqlNode whereClause = this.getBitAndFunction(exaRoleMask);
            rslStatementBuilder.whereClause(whereClause);
        }
    }

    private SqlNode getBitAndFunction(final Integer exaRoleMask) {
        final List<SqlNode> arguments = new ArrayList<>(2);
        arguments.add(new SqlColumn(1,
              ColumnMetadata.builder().name("exa_row_roles").type(DataType.createDecimal(20, 0)).build()));
        arguments.add(new SqlLiteralExactnumeric(BigDecimal.valueOf(exaRoleMask)));
        return new SqlFunctionScalar(ScalarFunction.BIT_AND, arguments, true, false);
    }
}
