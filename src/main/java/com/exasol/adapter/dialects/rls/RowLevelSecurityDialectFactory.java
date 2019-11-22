package com.exasol.adapter.dialects.rls;

import java.sql.Connection;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.SqlDialect;
import com.exasol.adapter.dialects.SqlDialectFactory;
import com.exasol.logging.VersionCollector;

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
        final VersionCollector versionCollector = new VersionCollector(
                "META-INF/maven/com.exasol/row-level-security/pom.properties");
        return versionCollector.getVersionNumber();
    }

    @Override
    public SqlDialect createSqlDialect(final Connection connection, final AdapterProperties properties) {
        return new RowLevelSecurityDialect(connection, properties);
    }
}
