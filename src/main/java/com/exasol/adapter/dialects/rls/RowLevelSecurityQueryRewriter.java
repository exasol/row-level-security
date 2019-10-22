package com.exasol.adapter.dialects.rls;

import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.BaseQueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.exasol.ExasolQueryRewriter;
import com.exasol.adapter.jdbc.RemoteMetadataReader;
import com.exasol.adapter.sql.SqlStatement;

import java.sql.Connection;
import java.sql.SQLException;

public class RowLevelSecurityQueryRewriter extends ExasolQueryRewriter {
    /**
     * Create a new instance of a {@link BaseQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connection           JDBC connection to remote data source
     */
    public RowLevelSecurityQueryRewriter(SqlDialect dialect, RemoteMetadataReader remoteMetadataReader,
          Connection connection) {
        super(dialect, remoteMetadataReader, connection);
    }

    @Override
    public String rewrite(SqlStatement statement, ExaMetadata exaMetadata, AdapterProperties properties)
          throws AdapterException, SQLException {
        String query = super.rewrite(statement, exaMetadata, properties);
        query = query.substring(1, query.length() - 1);
        query += " WHERE amount = 2 AND item = 'Screwdriver'".replaceAll("'", "''") + "'";
        return query;
    }
}
