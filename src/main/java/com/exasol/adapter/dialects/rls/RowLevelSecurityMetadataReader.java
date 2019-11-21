package com.exasol.adapter.dialects.rls;

import java.sql.Connection;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.exasol.ExasolMetadataReader;
import com.exasol.adapter.jdbc.ColumnMetadataReader;

public class RowLevelSecurityMetadataReader extends ExasolMetadataReader {
    /**
     * Create a new instance of the {@link ExasolMetadataReader}.
     *
     * @param connection database connection through which the reader retrieves the metadata from the remote source
     * @param properties user-defined properties
     */
    public RowLevelSecurityMetadataReader(Connection connection, AdapterProperties properties) {
        super(connection, properties);
    }

    @Override
    protected ColumnMetadataReader createColumnMetadataReader() {
        return new RowLevelSecurityColumnMetadataReader(this.connection, this.properties, getIdentifierConverter());
    }
}