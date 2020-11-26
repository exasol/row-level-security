package com.exasol.adapter.dialects.rls;

import java.util.Map;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@Tag("virtual-schema")
class RowLevelSecurityLocalIT extends AbstractRowLevelSecurityIT {
    @Override
    protected Map<String, String> getConnectionSpecificVirtualSchemaProperties() {
        return Map.of("IS_LOCAL", "true");
    }
}