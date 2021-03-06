package com.exasol.adapter.dialects.rls;

import java.sql.Connection;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.exasol.ExasolMetadataReader;
import com.exasol.adapter.jdbc.ColumnMetadataReader;
import com.exasol.adapter.jdbc.TableMetadataReader;

/**
 * This class reads RLS-specific database metadata.
 */
public class RowLevelSecurityMetadataReader extends ExasolMetadataReader {
    /**
     * Create a new instance of the {@link RowLevelSecurityMetadataReader}.
     *
     * @param connection database connection through which the reader retrieves the metadata from the remote source
     * @param properties user-defined properties
     */
    public RowLevelSecurityMetadataReader(final Connection connection, final AdapterProperties properties) {
        super(connection, properties);
    }

    @Override
    protected ColumnMetadataReader createColumnMetadataReader() {
        return new RowLevelSecurityColumnMetadataReader(this.connection, this.properties, getIdentifierConverter());
    }

    @Override
    protected TableMetadataReader createTableMetadataReader() {
        return new RowLevelSecurityTableMetadataReader(this.connection, this.columnMetadataReader, this.properties,
                this.identifierConverter);
    }
}