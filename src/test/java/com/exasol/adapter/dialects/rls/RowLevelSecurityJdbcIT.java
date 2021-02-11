package com.exasol.adapter.dialects.rls;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Tag;

@Tag("integration")
@Tag("virtual-schema")
@Tag("slow")
class RowLevelSecurityJdbcIT extends AbstractRowLevelSecurityIT {
    @Override
    protected Map<String, String> getConnectionSpecificVirtualSchemaProperties() {
        return Collections.emptyMap();
    }
}