package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_ROLES_COLUMN_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_TENANT_COLUMN_NAME;

import java.sql.*;

import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

/**
 * Reads the list of protected tables and the protection mechanism from the database.
 */
public class TableProtectionStatusReader {
    private static final int TABLE_NAME_COLUMN = 3;
    private static final int COLUMN_NAME_COLUMN = 4;
    private static final String PROTECTION_COLUMN_NAME_PATTERN = "EXA_ROW_%";
    private static final String TABLE_NAME_PATTERN = "%";
    private final DatabaseMetaData metadata;

    /**
     * Create a new instance of a {@link TableProtectionStatusReader}.
     *
     * @param metadata database metadata
     */
    public TableProtectionStatusReader(final DatabaseMetaData metadata) {
        this.metadata = metadata;
    }

    /**
     * Read the table protection status from the database metadata.
     *
     * @param catalogName name of the catalog to be scanned
     * @param schemaName  name of the schema to be scanned
     * @return table protection status
     */
    public TableProtectionStatus read(final String catalogName, final String schemaName) {
        try {
            final TableProtectionStatus.Builder builder = TableProtectionStatus.builder();
            final ResultSet columns = this.metadata.getColumns(catalogName, schemaName, TABLE_NAME_PATTERN,
                    PROTECTION_COLUMN_NAME_PATTERN);
            while (columns.next()) {
                final String tableName = columns.getString(TABLE_NAME_COLUMN);
                final String columnName = columns.getString(COLUMN_NAME_COLUMN);
                if (EXA_ROW_ROLES_COLUMN_NAME.equals(columnName)) {
                    builder.addRoleProtectedTable(tableName);
                } else if (EXA_ROW_TENANT_COLUMN_NAME.equals(columnName)) {
                    builder.addTenantProtectedTable(tableName);
                }
            }
            return builder.build();
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                    "Unable to read protection status from database metadata of tables in catalog \"" + catalogName
                            + "\", schema \"" + schemaName + "\".");
        }
    }
}