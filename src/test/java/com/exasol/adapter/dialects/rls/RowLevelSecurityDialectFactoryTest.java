package com.exasol.adapter.dialects.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.sql.Connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.exasol.adapter.AdapterProperties;

@ExtendWith({ MockitoExtension.class })
class RowLevelSecurityDialectFactoryTest {
    private RowLevelSecurityDialectFactory factory;
    @Mock
    Connection connection;

    @BeforeEach
    void beforeEach() {
        this.factory = new RowLevelSecurityDialectFactory();
    }

    @Test
    void testGetName() {
        assertThat(this.factory.getSqlDialectName(), equalTo("EXASOL_RLS"));
    }

    @Test
    void testCreateDialect() {
        assertThat(this.factory.createSqlDialect(connection, AdapterProperties.emptyProperties()),
                instanceOf(RowLevelSecurityDialect.class));
    }
}