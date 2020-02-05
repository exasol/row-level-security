package com.exasol.adapter.dialects.exasol;

import com.exasol.adapter.dialects.BaseQueryRewriter;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.jdbc.*;

/**
 * Exasol-specific query rewriter for regular JDBC connections to the remote Exasol data source.
 */
public class ExasolJdbcQueryRewriter extends BaseQueryRewriter {
    /**
     * Create a new instance of the {@link ExasolJdbcQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connectionFactory    factory for JDBC connection to remote data source
     */
    public ExasolJdbcQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
            final ConnectionFactory connectionFactory) {
        super(dialect, remoteMetadataReader, connectionFactory);
    }

    @Override
    protected ConnectionDefinitionBuilder createConnectionDefinitionBuilder() {
        return new ExasolConnectionDefinitionBuilder();
    }
}