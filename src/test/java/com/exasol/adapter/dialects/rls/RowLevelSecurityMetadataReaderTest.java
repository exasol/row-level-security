package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.jdbc.BaseTableMetadataReader;

class RowLevelSecurityMetadataReaderTest {
    private RowLevelSecurityMetadataReader metadataReader;

    @BeforeEach
    void beforeEach() {
        this.metadataReader = new RowLevelSecurityMetadataReader(null, AdapterProperties.emptyProperties());
    }

    @Test
    void testGetTableMetadataReader() {
        assertThat(this.metadataReader.getTableMetadataReader(), instanceOf(BaseTableMetadataReader.class));
    }

    @Test
    void testGetColumnMetadataReader() {
        assertThat(this.metadataReader.getColumnMetadataReader(),
                instanceOf(RowLevelSecurityColumnMetadataReader.class));
    }
}