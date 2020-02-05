package com.exasol.adapter.dialects.exasol;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.adapter.jdbc.ConnectionFactory;

/**
 * Factory for the Exasol SQL dialect.
 */
public class ExasolSqlDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return ExasolSqlDialect.NAME;
    }

    @Override
    public String getSqlDialectVersion() {
        return null;
    }

    @Override
    public SqlDialect createSqlDialect(final ConnectionFactory connectionFactory, final AdapterProperties properties) {
        return new ExasolSqlDialect(connectionFactory, properties);
    }
}