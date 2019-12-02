package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_ROLES_COLUMN_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROW_TENANT_COLUMN_NAME;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.dialects.exasol.ExasolColumnMetadataReader;
import com.exasol.adapter.metadata.ColumnMetadata;

/**
 * This class implements RLS-specific reading of column metadata.
 */
public class RowLevelSecurityColumnMetadataReader extends ExasolColumnMetadataReader {
    /**
     * Create a new instance of the {@link RowLevelSecurityColumnMetadataReader}.
     *
     * @param connection          JDBC connection through which the column metadata is read from the remote database
     * @param properties          user-defined adapter properties
     * @param identifierConverter converter between source and Exasol identifiers
     */
    public RowLevelSecurityColumnMetadataReader(final Connection connection, final AdapterProperties properties,
            final IdentifierConverter identifierConverter) {
        super(connection, properties, identifierConverter);
    }

    @Override
    protected List<ColumnMetadata> getColumnsFromResultSet(final ResultSet remoteColumns) throws SQLException {
        final List<ColumnMetadata> columnMetadataList = super.getColumnsFromResultSet(remoteColumns);
        final List<ColumnMetadata> newColumnMetadataList = new ArrayList<>(columnMetadataList.size());
        for (final ColumnMetadata columnMetadata : columnMetadataList) {
            hideRlsSystemColumnsInMetadata(newColumnMetadataList, columnMetadata);
        }
        return newColumnMetadataList;
    }

    private void hideRlsSystemColumnsInMetadata(final List<ColumnMetadata> newColumnMetadataList, final ColumnMetadata columnMetadata) {
        final String name = columnMetadata.getName();
        if (!name.equals(EXA_ROW_ROLES_COLUMN_NAME) && !name.equals(EXA_ROW_TENANT_COLUMN_NAME)) {
            newColumnMetadataList.add(columnMetadata);
        }
    }
}