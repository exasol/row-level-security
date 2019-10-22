package com.exasol.adapter.dialects.rls;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.capabilities.*;
import com.exasol.adapter.dialects.QueryRewriter;
import com.exasol.adapter.dialects.exasol.ExasolSqlDialect;

import java.sql.Connection;

import static com.exasol.adapter.capabilities.MainCapability.*;

public class RowLevelSecurityDialect extends ExasolSqlDialect {
    static final String NAME = "RLS";
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
     * Create a new instance of the {@link ExasolSqlDialect}.
     *
     * @param connection SQL connection
     * @param properties adapter properties
     */
    public RowLevelSecurityDialect(Connection connection, AdapterProperties properties) {
        super(connection, properties);
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
        return new RowLevelSecurityQueryRewriter(this, this.remoteMetadataReader, this.connection);
    }
}