package com.exasol.adapter.dialects.rls;

import java.util.Map;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@Tag("virtual-schema")
class RowLevelSecurityLocalExaConnectionIT extends AbstractRowLevelSecurityIT {
    @Override
    protected Map<String, String> getVirtualSchemaProperties() {
        return Map.of("IS_LOCAL", "true", "IMPORT_FROM_EXA", "true", "EXA_CONNECTION_STRING", "localhost:8888");
    }
}