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
            final UserInformation userInformation =
                  new UserInformation("EXA_RLS_USERS", properties.getSchemaName(), exaMetadata.getCurrentUser());
            final SqlNode whereClause = createWhereClause(select, userInformation);
            rlsStatementBuilder.whereClause(whereClause);
            final SqlStatementSelect newSelectStatement = rlsStatementBuilder.build();
            return super.rewrite(newSelectStatement, exaMetadata, properties);
        } else {
            throw new IllegalArgumentException(
                  "Modified SQL statement must be a SELECT statement, but was " + statement.getClass().getName());
        }
    }

    private SqlNode createWhereClause(final SqlStatementSelect select, final UserInformation userInformation) {
        final String exaRoleMask = userInformation.getRoleMask(this.connection);
        final SqlNode whereClause;
        if (select.hasFilter()) {
            final SqlNode left = new SqlPredicateNotEqual(createRoleCheckPredicate(exaRoleMask),
                  new SqlLiteralExactnumeric(BigDecimal.valueOf(0)));
            final SqlNode right = select.getWhereClause();
            final List<SqlNode> arguments = List.of(left, right);
            whereClause = new SqlPredicateAnd(arguments);
        } else {
            whereClause = new SqlPredicateNotEqual(createRoleCheckPredicate(exaRoleMask),
                  new SqlLiteralExactnumeric(BigDecimal.valueOf(0)));
        }
        return whereClause;
    }

    private SqlNode createRoleCheckPredicate(final String exaRoleMask) {
        final List<SqlNode> arguments = new ArrayList<>(2);
        arguments.add(new SqlColumn(1,
              ColumnMetadata.builder().name("EXA_ROW_ROLES").type(DataType.createDecimal(20, 0)).build()));
        arguments.add(new SqlLiteralExactnumeric(new BigDecimal(exaRoleMask)));
        return new SqlFunctionScalar(ScalarFunction.BIT_AND, arguments, true, false);
    }
}