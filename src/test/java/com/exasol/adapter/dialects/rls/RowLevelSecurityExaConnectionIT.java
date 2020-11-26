package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.exasol.ExasolProperties.EXASOL_CONNECTION_PROPERTY;

import java.util.Map;

import org.junit.jupiter.api.*;

import com.exasol.dbbuilder.dialects.exasol.ConnectionDefinition;

@Tag("integration")
@Tag("virtual-schema")
class RowLevelSecurityExaConnectionIT extends AbstractRowLevelSecurityIT {
    private static final String EXA_CONNECTION_NAME = "EXA_CONNECTION";
    private ConnectionDefinition exaConnection;

    @BeforeEach
    void beforeEach() {
        this.exaConnection = objectFactory.createConnectionDefinition(EXA_CONNECTION_NAME,
                "127.0.0.1:" + EXASOL.getDefaultInternalDatabasePort(), EXASOL.getUsername(), EXASOL.getPassword());
    }

    @AfterEach
    void afterEach() {
        this.exaConnection.drop();
        this.exaConnection = null;
    }

    @Override
    protected Map<String, String> getConnectionSpecificVirtualSchemaProperties() {
        return Map.of("IMPORT_FROM_EXA", "true", EXASOL_CONNECTION_PROPERTY, this.exaConnection.getName());
    }
}