package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.rewriting.SqlGenerationHelper;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.metadata.*;
import com.exasol.adapter.sql.*;
import com.exasol.db.ExasolIdentifier;

/**
 * RLS-specific query rewriter.
 */
// [impl->dsn~query-rewriter~1]
public class RowLevelSecurityQueryRewriter implements QueryRewriter {
    private static final Logger LOGGER = Logger.getLogger(RowLevelSecurityQueryRewriter.class.getName());
    private final TableProtectionStatus tableProtectionStatus;
    private final QueryRewriter delegateRewriter;
    private final ConnectionFactory connectionFactory;

    /**
     * Create a new instance of a {@link RowLevelSecurityQueryRewriter}.
     *
     * @param dialect               dialect
     * @param remoteMetadataReader  remote metadata reader
     * @param connectionFactory     factory for JDBC connection to remote data source
     * @param tableProtectionStatus table protection information
     * @param delegateRewriter      rewriter handling the dialect specific parts
     */
    public RowLevelSecurityQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
            final ConnectionFactory connectionFactory, final TableProtectionStatus tableProtectionStatus,
            final QueryRewriter delegateRewriter) {
        this.connectionFactory = connectionFactory;
        this.tableProtectionStatus = tableProtectionStatus;
        this.delegateRewriter = delegateRewriter;
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
        final UserInformation userInformation = new UserInformation(ExasolIdentifier.of(exaMetadata.getCurrentUser()),
                ExasolIdentifier.of(schemaName), this.connectionFactory);
        final String tableName = ((SqlTable) select.getFromClause()).getName();
        final TableProtectionDetails protection = this.tableProtectionStatus.getTableProtectionDetails(tableName);
        logTableProtectionInfo(tableName, protection);
        if (protection.isProtected()) {
            final SqlStatementSelect protectedSelectStatement = getProtectedSqlStatementSelect(select, userInformation,
                    protection);
            return this.delegateRewriter.rewrite(protectedSelectStatement, exaMetadata, properties);
        } else {
            return this.delegateRewriter.rewrite(statement, exaMetadata, properties);
        }
    }

    private void logTableProtectionInfo(final String tableName, final TableProtectionDetails protection) {
        LOGGER.info(() -> "Table \"" + tableName + "\": " + protection.describe());
    }

    private SqlStatementSelect getProtectedSqlStatementSelect(final SqlStatementSelect select,
            final UserInformation userInformation, final TableProtectionDetails protection) throws SQLException {
        final SqlSelectList sqlSelectList = getSqlSelectList(select);
        final SqlStatementSelect.Builder rlsStatementBuilder = copyOriginalClauses(select, sqlSelectList);
        final SqlNode whereClause = createWhereClause(select, userInformation, protection);
        rlsStatementBuilder.whereClause(whereClause);
        return rlsStatementBuilder.build();
    }

    private SqlSelectList getSqlSelectList(final SqlStatementSelect select) {
        final SqlSelectList oldSelectList = select.getSelectList();
        if (!oldSelectList.hasExplicitColumnsList()) {
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
                if (!RLS_COLUMNS.contains(sqlColumn.getName())) {
                    selectListElements.add(sqlColumn);
                }
            }
        }
        return selectListElements;
    }

    private SqlStatementSelect.Builder copyOriginalClauses(final SqlStatementSelect select,
            final SqlSelectList sqlSelectList) {
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
        return rlsStatementBuilder;
    }

    private SqlNode createWhereClause(final SqlStatementSelect select, final UserInformation userInformation,
            final TableProtectionDetails protection) throws SQLException {
        if (select.hasFilter()) {
            return combineRegularFilterWithRlsFilter(select, userInformation, protection);
        } else {
            return createRlsFilter(userInformation, protection);
        }
    }

    private SqlNode combineRegularFilterWithRlsFilter(final SqlStatementSelect select,
            final UserInformation userInformation, final TableProtectionDetails protection) throws SQLException {
        final SqlNode where = select.getWhereClause();
        if (protection.isTenantProtected()) {
            if (protection.isRoleProtected()) {
                return and(where, createTenantsNode(userInformation), createRolesNode(userInformation));
            } else if (protection.isGroupProtected()) {
                return and(where, or(createTenantsNode(userInformation), createGroupNode(userInformation)));
            } else {
                return and(where, createTenantsNode(userInformation));
            }
        } else if (protection.isRoleProtected()) {
            return and(where, createRolesNode(userInformation));
        } else if (protection.isGroupProtected()) {
            return and(where, createGroupNode(userInformation));
        } else {
            return where;
        }
    }

    private SqlNode and(final SqlNode... operands) {
        return new SqlPredicateAnd(List.of(operands));
    }

    private SqlNode or(final SqlNode... operands) {
        return new SqlPredicateOr(List.of(operands));
    }

    // [impl->dsn~query-rewriter-adds-row-filter-for-tenants~1]
    private SqlNode createTenantsNode(final UserInformation userInformation) {
        final SqlNode left = createColumn(EXA_ROW_TENANT_COLUMN_NAME, DataType.createDecimal(20, 0));
        final SqlNode right = new SqlLiteralString(userInformation.getCurrentUser().toString());
        return new SqlPredicateEqual(left, right);
    }

    // [impl->dsn~query-rewriter-adds-row-filter-for-roles~1]
    // [impl->dsn~null-values-in-role-ids-and-masks~1]:
    // The BIT_AND function returns NULL in case one of its parameters is NULL.
    // In Exasol the following comparison returns false: NULL <> 0
    private SqlNode createRolesNode(final UserInformation userInformation) {
        final String exaRoleMask = userInformation.getRoleMask();
        return new SqlPredicateNotEqual(createRoleCheckPredicate(exaRoleMask),
                new SqlLiteralExactnumeric(BigDecimal.valueOf(0)));
    }

    private SqlNode createRoleCheckPredicate(final String exaRoleMask) {
        final List<SqlNode> operands = List.of(createColumn(EXA_ROW_ROLES_COLUMN_NAME, MASK_TYPE),
                new SqlLiteralExactnumeric(new BigDecimal(exaRoleMask)));
        return new SqlFunctionScalar(ScalarFunction.BIT_AND, operands);
    }

    private SqlColumn createColumn(final String name, final DataType type) {
        return new SqlColumn(1, ColumnMetadata.builder().name(name).type(type).build());
    }

    // [impl->dsn~query-rewriter-adds-row-filter-for-group~1]
    private SqlNode createGroupNode(final UserInformation userInformation) throws SQLException {
        final List<String> groups = userInformation.getGroups();
        if (groups.size() == 1) {
            return createSingleGroupNode(groups.get(0));
        } else {
            return createMultiGroupNode(groups);
        }
    }

    // This is an optimization to keep memory usage down in corner cases where a user is assigned to a single group
    // only.
    private SqlNode createSingleGroupNode(final String group) {
        LOGGER.fine(() -> "Filtering results by user's memebership in a single group: " + group);
        return new SqlPredicateEqual(createColumn(EXA_ROW_GROUP_COLUMN_NAME, IDENTIFIER_TYPE),
                new SqlLiteralString(group));
    }

    private SqlNode createMultiGroupNode(final List<String> groups) {
        LOGGER.fine(() -> "Filtering results by user's memebership in groups: " + groups.toString());
        final List<SqlNode> groupNodes = new ArrayList<>(groups.size());
        for (final String group : groups) {
            groupNodes.add(new SqlLiteralString(group));
        }
        return new SqlPredicateInConstList(createColumn(EXA_ROW_GROUP_COLUMN_NAME, IDENTIFIER_TYPE), groupNodes);
    }

    // [impl->dsn~query-rewriter-treats-protected-tables-with-roles-and-tenant-restrictions~1]
    // [impl->dsn~query-rewriter-treats-protected-tables-with-group-and-tenant-restrictions~1]
    private SqlNode createRlsFilter(final UserInformation userInformation, final TableProtectionDetails protection)
            throws SQLException {
        if (protection.isTenantProtected()) {
            if (protection.isRoleProtected()) {
                return and(createTenantsNode(userInformation), createRolesNode(userInformation));
            } else if (protection.isGroupProtected() && userInformation.hasGroups()) {
                return or(createTenantsNode(userInformation), createGroupNode(userInformation));
            } else {
                return createTenantsNode(userInformation);
            }
        } else if (protection.isRoleProtected()) {
            return createRolesNode(userInformation);
        } else if (protection.isGroupProtected()) {
            return createGroupNode(userInformation);
        } else {
            throw new IllegalArgumentException("Unfiltered WHERE clause in RLS.");
        }
    }
}