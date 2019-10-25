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
            final SqlStatementSelect.Builder rlsStatementBuilder =
                  SqlStatementSelect.builder().selectList(select.getSelectList()).fromClause(select.getFromClause());
            if (select.hasGroupBy()) {
                rlsStatementBuilder.groupBy(select.getGroupBy());
            }
            if (select.hasHaving()) {
                rlsStatementBuilder.having(select.getHaving());
            }
            if (select.hasLimit()) {
                rlsStatementBuilder.limit(select.getLimit());
            }
            if (select.hasOrderBy()) {
                rlsStatementBuilder.orderBy(select.getOrderBy());
            }
            applyWhereClause(select, rlsStatementBuilder);
            final SqlStatementSelect newSelectStatement = rlsStatementBuilder.build();
            return super.rewrite(newSelectStatement, exaMetadata, properties);
        } else {
            throw new IllegalArgumentException(
                  "Modified SQL statement must be a SELECT statement, but was " + statement.getClass().getName());
        }
    }

    private void applyWhereClause(final SqlStatementSelect select,
          final SqlStatementSelect.Builder rslStatementBuilder) {
        final UserInformation userInformation = new UserInformation("exa_rls_users");
        final long exaRoleMask = userInformation.getRoleMask(this.connection);
        if (select.hasFilter()) {
            final SqlNode left = createRoleCheckPredicate(exaRoleMask);
            final SqlNode right = select.getWhereClause();
            final List<SqlNode> arguments = List.of(left, right);
            final SqlNode whereClause = new SqlPredicateAnd(arguments);
            rslStatementBuilder.whereClause(whereClause);
        } else {
            final SqlNode whereClause = createRoleCheckPredicate(exaRoleMask);
            rslStatementBuilder.whereClause(whereClause);
        }
    }

    private SqlNode createRoleCheckPredicate(final Long exaRoleMask) {
        final List<SqlNode> arguments = new ArrayList<>(2);
        arguments.add(new SqlColumn(1,
              ColumnMetadata.builder().name("exa_row_roles").type(DataType.createDecimal(20, 0)).build()));
        arguments.add(new SqlLiteralExactnumeric(BigDecimal.valueOf(exaRoleMask)));
        return new SqlFunctionScalar(ScalarFunction.BIT_AND, arguments, true, false);
    }
}