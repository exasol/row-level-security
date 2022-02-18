package com.exasol.adapter.dialects.rls;

import java.util.Map;

import org.junit.jupiter.api.*;

import com.exasol.dbbuilder.dialects.exasol.ConnectionDefinition;
import com.exasol.tools.FingerprintExtractor;

@Tag("integration")
@Tag("virtual-schema")
class RowLevelSecurityExaConnectionIT extends AbstractRowLevelSecurityIT {
    private static final String EXA_CONNECTION_NAME = "EXA_CONNECTION";
    private ConnectionDefinition exaConnection;

    @BeforeEach
    void beforeEach() {
        this.exaConnection = objectFactory.createConnectionDefinition(EXA_CONNECTION_NAME, getTargetAddress(),
                EXASOL.getUsername(), EXASOL.getPassword());
    }

    private String getTargetAddress() {
        if (exasolVersionSupportsFingerprintInAddress()) {
            final String fingerprint = FingerprintExtractor.extractFingerprint(EXASOL.getJdbcUrl());
            return "127.0.0.1/" + fingerprint + ":" + EXASOL.getDefaultInternalDatabasePort();
        }
        return "127.0.0.1:" + EXASOL.getDefaultInternalDatabasePort();
    }

    @AfterEach
    void afterEach() {
        this.exaConnection.drop();
        this.exaConnection = null;
    }

    @Override
    protected Map<String, String> getConnectionSpecificVirtualSchemaProperties() {
        return Map.of("IMPORT_FROM_EXA", "true", "EXA_CONNECTION", this.exaConnection.getName());
    }
}