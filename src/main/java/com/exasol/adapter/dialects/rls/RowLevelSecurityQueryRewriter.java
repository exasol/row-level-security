package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_ROLES_COLUMN_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_TENANTS_COLUMN_NAME;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseQueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlGenerationHelper;
import com.exasol.adapter.dialects.exasol.ExasolQueryRewriter;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.ColumnMetadata;
import com.exasol.adapter.metadata.DataType;
import com.exasol.adapter.metadata.TableMetadata;
import com.exasol.adapter.sql.*;

public class RowLevelSecurityQueryRewriter extends ExasolQueryRewriter {
    private static final Logger LOGGER = Logger.getLogger(RowLevelSecurityQueryRewriter.class.getName());
    private final TableProtectionStatus tableProtectionStatus;

    /**
     * Create a new instance of a {@link BaseQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connection           JDBC connection to remote data source
     */
    public RowLevelSecurityQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
            final Connection connection, final TableProtectionStatus tableProtectionStatus) {
        super(dialect, remoteMetadataReader, connection);
        this.tableProtectionStatus = tableProtectionStatus;
    }

    @Override
    public String rewrite(final SqlStatement statement, final ExaMetadata exaMetadata,
            final AdapterProperties properties) throws AdapterException, SQLException {
        if (statement instanceof SqlStatementSelect) {
            return rewriteStatement(statement, exaMetadata, properties);
        } else {
            throw new IllegalArgumentException(
                    "Modified SQL statement must be a SELECT statement, but was " + statement.getClass().getName());
        }
    }

    private String rewriteStatement(final SqlStatement statement, final ExaMetadata exaMetadata,
            final AdapterProperties properties) throws SQLException, AdapterException {
        final SqlStatementSelect select = (SqlStatementSelect) statement;
        final String schemaName = properties.getSchemaName();
        final UserInformation userInformation = new UserInformation(exaMetadata.getCurrentUser(), schemaName,
                "EXA_RLS_USERS");
        final boolean protectedWithExaRowRoles = this.tableProtectionStatus.isTableProtectedWithExaRowRoles(
                properties.getCatalogName(), schemaName, ((SqlTable) select.getFromClause()).getName());
        final boolean protectedWithExaRowTenants = this.tableProtectionStatus.isTableProtectedWithRowTenants(
                properties.getCatalogName(), schemaName, ((SqlTable) select.getFromClause()).getName());
        logTableProtectionInfo(protectedWithExaRowRoles, protectedWithExaRowTenants);
        if (protectedWithExaRowRoles || protectedWithExaRowTenants) {
            final SqlStatementSelect newSelectStatement = getNewSqlStatementSelect(select, userInformation,
                    protectedWithExaRowRoles, protectedWithExaRowTenants);
            return super.rewrite(newSelectStatement, exaMetadata, properties);
        } else {
            return super.rewrite(statement, exaMetadata, properties);
        }
    }

    private void logTableProtectionInfo(final boolean protectedWithExaRowRoles,
            final boolean protectedWithExaRowTenants) {
        if (protectedWithExaRowRoles) {
            LOGGER.info(() -> "Table is protected with " + EXA_ROW_ROLES_COLUMN_NAME);
        }
        if (protectedWithExaRowTenants) {
            LOGGER.info(() -> "Table is protected with " + EXA_ROW_TENANTS_COLUMN_NAME);
        }
        if (!protectedWithExaRowRoles && !protectedWithExaRowTenants) {
            LOGGER.info(() -> "Table is unprotected");
        }
    }

    private SqlStatementSelect getNewSqlStatementSelect(final SqlStatementSelect select,
            final UserInformation userInformation, final boolean protectedWithExaRowRoles,
            final boolean protectedWithExaRowTenants) {
        final SqlSelectList sqlSelectList = getSqlSelectList(select);
        final SqlStatementSelect.Builder rlsStatementBuilder = SqlStatementSelect.builder().selectList(sqlSelectList)
                .fromClause(select.getFromClause());
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

        final SqlNode whereClause = createWhereClause(select, userInformation, protectedWithExaRowRoles,
                protectedWithExaRowTenants);
        rlsStatementBuilder.whereClause(whereClause);
        return rlsStatementBuilder.build();
    }

    private SqlSelectList getSqlSelectList(SqlStatementSelect select) {
        final SqlSelectList oldSelectList = select.getSelectList();
        if (oldSelectList.isSelectStar()) {
            final List<TableMetadata> tableMetadata = new ArrayList<>();
            final SqlStatementSelect newSelectStatement = (SqlStatementSelect) oldSelectList.getParent();
            SqlGenerationHelper.addMetadata(newSelectStatement.getFromClause(), tableMetadata);
            return SqlSelectList.createRegularSelectList(getSelectListWithColumns(tableMetadata));
        } else {
            return oldSelectList;
        }
    }

    private List<SqlNode> getSelectListWithColumns(final List<TableMetadata> tableMetadata) {
        final List<SqlNode> selectListElements = new ArrayList<>(tableMetadata.size());
        for (int i = 0; i < tableMetadata.size(); i++) {
            final TableMetadata tableMeta = tableMetadata.get(i);
            for (final ColumnMetadata columnMeta : tableMeta.getColumns()) {
                final SqlColumn sqlColumn = new SqlColumn(i, columnMeta);
                if (!sqlColumn.getName().equals(EXA_ROW_ROLES_COLUMN_NAME)
                        && !sqlColumn.getName().equals(EXA_ROW_TENANTS_COLUMN_NAME)) {
                    selectListElements.add(sqlColumn);
                }
            }
        }
        return selectListElements;
    }

    private SqlNode createWhereClause(final SqlStatementSelect select, final UserInformation userInformation,
            final boolean protectedWithExaRowRoles, final boolean protectedWithExaRowTenants) {
        final Optional<SqlNode> whereClauseForRoles = setWhereClauseForRoles(protectedWithExaRowRoles, userInformation);
        final Optional<SqlNode> whereClauseForTenants = setWhereClauseForTenants(protectedWithExaRowTenants,
                userInformation);
        final List<SqlNode> arguments = new ArrayList<>(3);
        if (select.hasFilter()) {
            arguments.add(select.getWhereClause());
        }
        whereClauseForRoles.ifPresent(arguments::add);
        whereClauseForTenants.ifPresent(arguments::add);
        if (arguments.size() == 2 || arguments.size() == 3) {
            return new SqlPredicateAnd(arguments);
        } else if (arguments.size() == 1) {
            return arguments.get(0);
        } else {
            throw new IllegalArgumentException("Unexpected size of the result set.");
        }
    }

    private Optional<SqlNode> setWhereClauseForTenants(final boolean protectedWithExaRowTenants,
            final UserInformation userInformation) {
        if (protectedWithExaRowTenants) {
            return Optional.of(getExaRowTenantsNode(userInformation));
        } else {
            return Optional.empty();
        }
    }

    private SqlNode getExaRowTenantsNode(final UserInformation userInformation) {
        final SqlNode left = new SqlColumn(1,
                ColumnMetadata.builder().name(EXA_ROW_TENANTS_COLUMN_NAME).type(DataType.createDecimal(20, 0)).build());
        final SqlNode right = new SqlLiteralString(userInformation.getCurrentUser());
        return new SqlPredicateEqual(left, right);
    }

    private Optional<SqlNode> setWhereClauseForRoles(final boolean protectedWithExaRowRoles,
            final UserInformation userInformation) {
        if (protectedWithExaRowRoles) {
            final String exaRoleMask = userInformation.getRoleMask(this.connection);
            final SqlPredicateNotEqual whereClauseForRoles = new SqlPredicateNotEqual(
                    createRoleCheckPredicate(exaRoleMask), new SqlLiteralExactnumeric(BigDecimal.valueOf(0)));
            return Optional.of(whereClauseForRoles);
        } else {
            return Optional.empty();
        }
    }

    private SqlNode createRoleCheckPredicate(final String exaRoleMask) {
        final List<SqlNode> arguments = new ArrayList<>(2);
        arguments.add(new SqlColumn(1,
                ColumnMetadata.builder().name(EXA_ROW_ROLES_COLUMN_NAME).type(DataType.createDecimal(20, 0)).build()));
        arguments.add(new SqlLiteralExactnumeric(new BigDecimal(exaRoleMask)));
        return new SqlFunctionScalar(ScalarFunction.BIT_AND, arguments, true, false);
    }
}