package com.exasol.adapter.dialects.rls;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

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
        assertTrue(this.metadataReader.isTableIncludedByMapping("MY_TABLE"));
    }

    @Test
    void testIsTableIncludedByMappingFalse() {
        assertFalse(this.metadataReader.isTableIncludedByMapping("EXA_RLS_USERS"));
    }
}