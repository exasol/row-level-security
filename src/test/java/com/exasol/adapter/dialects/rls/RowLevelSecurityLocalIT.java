package com.exasol.adapter.dialects.rls;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Tag;

@Tag("integration")
class RowLevelSecurityLocalIT extends AbstractRowLevelSecurityIT {
    @Override
    protected Map<String, String> getVirtualSchemaProperties() {
        return Collections.emptyMap();
    }
}