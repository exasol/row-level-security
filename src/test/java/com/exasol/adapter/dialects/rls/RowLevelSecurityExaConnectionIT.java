package com.exasol.adapter.dialects.rls;

import java.util.Map;

import org.junit.jupiter.api.*;

import com.exasol.dbbuilder.dialects.exasol.ConnectionDefinition;

import static com.exasol.adapter.dialects.rls.DBHelper.exasolVersionSupportsFingerprintInAddress;

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
        if (exasolVersionSupportsFingerprintInAddress(EXASOL.getDockerImageReference())) {
            final String fingerprint = EXASOL.getTlsCertificateFingerprint().get();
            return "127.0.0.1:" + EXASOL.getDefaultInternalDatabasePort() + ";validateservercertificate=1;fingerprint="+fingerprint;
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