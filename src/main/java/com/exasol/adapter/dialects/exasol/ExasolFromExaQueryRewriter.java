package com.exasol.adapter.dialects.exasol;

import java.sql.SQLException;

import com.exasol.ExaConnectionInformation;
import com.exasol.ExaMetadata;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.*;
import com.exasol.adapter.sql.SqlNodeVisitor;
import com.exasol.adapter.sql.SqlStatement;

/**
 * Exasol-specific query rewriter for {@code IMPORT FROM EXA}.
 */
public class ExasolFromExaQueryRewriter extends BaseQueryRewriter {
    /**
     * Create a new instance of the {@link ExasolFromExaQueryRewriter}.
     *
     * @param dialect              dialect
     * @param remoteMetadataReader remote metadata reader
     * @param connectionFactory    factory for the JDBC connection to remote data source
     */
    public ExasolFromExaQueryRewriter(final SqlDialect dialect, final RemoteMetadataReader remoteMetadataReader,
            final ConnectionFactory connectionFactory) {
        super(dialect, remoteMetadataReader, connectionFactory);
    }

    @Override
    protected ConnectionDefinitionBuilder createConnectionDefinitionBuilder() {
        return new ExasolConnectionDefinitionBuilder();
    }

    @Override
    public String rewrite(final SqlStatement statement, final ExaMetadata exaMetadata,
            final AdapterProperties properties) throws AdapterException, SQLException {
        final SqlGenerationContext context = new SqlGenerationContext(properties.getCatalogName(),
                properties.getSchemaName(), false);
        final SqlNodeVisitor<String> sqlGeneratorVisitor = this.dialect.getSqlGenerationVisitor(context);
        final ExaConnectionInformation exaConnectionInformation = getConnectionInformation(exaMetadata, properties);
        final String connectionDefinition = this.connectionDefinitionBuilder.buildConnectionDefinition(properties,
                exaConnectionInformation);
        return "IMPORT FROM EXA " + connectionDefinition + " STATEMENT '"
                + statement.accept(sqlGeneratorVisitor).replace("'", "''") + "'";
    }
}