package com.exasol.adapter.sql;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_ROLES_COLUMN_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_TENANT_COLUMN_NAME;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

/**
 * This class provides information about tables' protection.
 */
public class TableProtectionStatus {
    private final DatabaseMetaData metadata;

    /**
     * Create a new instance of {@link TableProtectionStatus}.
     *
     * @param metadata database metadata
     */
    public TableProtectionStatus(final DatabaseMetaData metadata) {
        this.metadata = metadata;
    }

    /**
     * Check if a table is protected with tenants security.
     *
     * @param catalogName name of the database catalog
     * @param schemaName  name of the schema which contains table
     * @param tableName   name of the table to check
     * @return true if protected
     */
    public boolean isTableProtectedWithRowTenants(final String catalogName, final String schemaName,
            final String tableName) {
        return containsColumn(this.metadata, catalogName, schemaName, tableName, EXA_ROW_TENANT_COLUMN_NAME);
    }

    /**
     * Check if a table is protected with roles security.
     *
     * @param catalogName name of the database catalog
     * @param schemaName  name of the schema which contains table
     * @param tableName   name of the table to check
     * @return true if protected
     */
    public boolean isTableProtectedWithExaRowRoles(final String catalogName, final String schemaName,
            final String tableName) {
        return containsColumn(this.metadata, catalogName, schemaName, tableName, EXA_ROW_ROLES_COLUMN_NAME);
    }

    private boolean containsColumn(final DatabaseMetaData metadata, final String catalogName, final String schemaName,
            final String tableName, final String columnName) {
        try (final ResultSet column = metadata.getColumns(catalogName, schemaName, tableName, columnName)) {
            return column != null && column.next();
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                    "Unable to check existence of column \"" + columnName + "\".  Caused by: " + exception.getMessage(),
                    exception);
        }
    }
}
