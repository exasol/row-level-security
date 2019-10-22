package com.exasol.adapter.dialects.rls;

import com.exasol.adapter.AdapterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.exasol.reflect.ReflectionUtils.getMethodReturnViaReflection;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

class RowLevelSecurityDialectTest {
    private RowLevelSecurityDialect dialect;

    @BeforeEach
    void beforeEach() {
        this.dialect = new RowLevelSecurityDialect(null, AdapterProperties.emptyProperties());
    }

    @Test
    void testGetName() {
        assertThat(dialect.getName(), equalTo("RLS"));
    }

    @Test
    void testCreateQueryRewriter() {
        assertThat(getMethodReturnViaReflection(this.dialect, "createQueryRewriter"),
              instanceOf(RowLevelSecurityQueryRewriter.class));
    }
}