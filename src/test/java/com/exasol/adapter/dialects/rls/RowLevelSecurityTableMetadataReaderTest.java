package com.exasol.adapter.dialects.rls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test
    void testIsTableIncludedByMappingFalse() {
        assertThat(this.metadataReader.isTableIncludedByMapping("EXA_RLS_USERS"), equalTo(false));
    }
}