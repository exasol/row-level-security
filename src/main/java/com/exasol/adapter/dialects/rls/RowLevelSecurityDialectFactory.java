package com.exasol.adapter.dialects.rls;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;

import java.sql.Connection;

/**
 * Factory for the Row Level Security dialect.
 */
public class RowLevelSecurityDialectFactory implements SqlDialectFactory {
    @Override
    public String getSqlDialectName() {
        return RowLevelSecurityDialect.NAME;
    }

    @Override
    public String getSqlDialectVersion() {
        return null;
    }

    @Override
    public SqlDialect createSqlDialect(final Connection connection, final AdapterProperties properties) {
        return new RowLevelSecurityDialect(connection, properties);
    }
}

