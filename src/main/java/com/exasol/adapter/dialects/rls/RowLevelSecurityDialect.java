package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.capabilities.MainCapability.*;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.exasol.ExasolSqlDialect;
import com.exasol.adapter.jdbc.*;

/**
 * This class implements Row Level Security dialect.
 */
public class RowLevelSecurityDialect extends ExasolSqlDialect {
    static final String NAME = "EXASOL_RLS";
    private static final Capabilities CAPABILITIES = createCapabilityList();

    private static Capabilities createCapabilityList() {
        return Capabilities.builder() //
                .addMain(SELECTLIST_PROJECTION, AGGREGATE_SINGLE_GROUP, AGGREGATE_GROUP_BY_COLUMN,
                        AGGREGATE_GROUP_BY_TUPLE, AGGREGATE_HAVING, ORDER_BY_COLUMN, LIMIT, LIMIT_WITH_OFFSET) //
                .addLiteral(LiteralCapability.values()) //
                .addPredicate(PredicateCapability.values()) //
                .addAggregateFunction(AggregateFunctionCapability.values()) //
                .addScalarFunction(ScalarFunctionCapability.values()) //
                .build();
    }

    /**
     * Create a new instance of the {@link RowLevelSecurityDialect}.
     *
     * @param connectionFactory factory for JDBC connection to remote data source
     * @param properties        adapter properties
     */
    public RowLevelSecurityDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        super(connectionFactory, properties);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    protected QueryRewriter createQueryRewriter() {
        try {
            final String catalogName = this.properties.getCatalogName();
            final String schemaName = this.properties.getSchemaName();
            final DatabaseMetaData metadata = this.connectionFactory.getConnection().getMetaData();
            final TableProtectionStatus tableProtectionStatus = new TableProtectionStatusReader(metadata)
                    .read(catalogName, schemaName);
            final QueryRewriter delegate = super.createQueryRewriter();
            return new RowLevelSecurityQueryRewriter(this, createRemoteMetadataReader(), this.connectionFactory,
                    tableProtectionStatus, delegate);
        } catch (final SQLException exception) {
            throw new IllegalArgumentException("Unable to read metadata for instantiating TableProtectionStatus.");
        }
    }

    @Override
    protected RemoteMetadataReader createRemoteMetadataReader() {
        try {
            return new RowLevelSecurityMetadataReader(this.connectionFactory.getConnection(), this.properties);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException("Unable to create metadata reader for Row-Level Security dialect.",
                    exception);
        }
    }
}