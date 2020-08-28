package com.exasol.adapter.dialects.rls;

import java.util.Map;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@Tag("virtual-schema")
class RowLevelSecurityLocalIT extends AbstractRowLevelSecurityIT {
    @Override
    protected Map<String, String> getVirtualSchemaProperties() {
        // return Collections.emptyMap();
        return Map.of("DEBUG_ADDRESS", "172.17.0.1:3000", "LOG_LEVEL", "ALL");
    }
}