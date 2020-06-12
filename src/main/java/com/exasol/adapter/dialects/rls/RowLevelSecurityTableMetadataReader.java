package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.RLS_METADATA_TABLES;

import java.sql.Connection;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.IdentifierConverter;
import com.exasol.adapter.jdbc.BaseTableMetadataReader;
import com.exasol.adapter.jdbc.ColumnMetadataReader;

/**
 * This class implements RLS-specific reading of table metadata.
 */
public class RowLevelSecurityTableMetadataReader extends BaseTableMetadataReader {
    /**
     * Create a new instance of a {@link RowLevelSecurityTableMetadataReader}.
     *
     * @param connection           JDBC connection to remote data source
     * @param columnMetadataReader reader to be used to map the tables columns
     * @param properties           user-defined adapter properties
     * @param identifierConverter  converter between source and Exasol identifiers
     */
    public RowLevelSecurityTableMetadataReader(final Connection connection,
            final ColumnMetadataReader columnMetadataReader, final AdapterProperties properties,
            final IdentifierConverter identifierConverter) {
        super(connection, columnMetadataReader, properties, identifierConverter);
    }

    @Override
    public boolean isTableIncludedByMapping(final String tableName) {
        return !RLS_METADATA_TABLES.contains(tableName);
    }
}