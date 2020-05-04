package com.exasol.adapter.dialects.rls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.adapter.AdapterProperties;

class RowLevelSecurityTableMetadataReaderTest {
    private RowLevelSecurityTableMetadataReader metadataReader;

    @BeforeEach
    void setUp() {
        this.metadataReader = new RowLevelSecurityTableMetadataReader(null, null, AdapterProperties.emptyProperties(),
                null);
    }

    @Test
    void testIsTableIncludedByMappingTrue() {
        assertThat(this.metadataReader.isTableIncludedByMapping("MY_TABLE"), equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "EXA_RLS_USERS", "EXA_ROLES_MAPPING", "EXA_USER_GROUPS" })
    void testIsTableIncludedByMappingFalse(final String tableName) {
        assertThat(this.metadataReader.isTableIncludedByMapping(tableName), equalTo(false));
    }
}