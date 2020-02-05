package com.exasol.adapter.dialects.exasol;

import static com.exasol.adapter.AdapterProperties.*;
import static com.exasol.adapter.dialects.exasol.ExasolProperties.EXASOL_IMPORT_PROPERTY;
import static com.exasol.reflect.ReflectionUtils.getMethodReturnViaReflection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.exasol.*;
import com.exasol.adapter.AdapterException;
import com.exasol.adapter.AdapterProperties;
import com.exasol.adapter.dialects.*;
import com.exasol.adapter.jdbc.BaseRemoteMetadataReader;
import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.sql.TestSqlStatementFactory;

class ExasolFromExaQueryRewriterTest extends AbstractQueryRewriterTestBase {
    @BeforeEach
    void beforeEach() {
        this.exaConnectionInformation = mock(ExaConnectionInformation.class);
        this.exaMetadata = mock(ExaMetadata.class);
        this.rawProperties = new HashMap<>();
        this.statement = TestSqlStatementFactory.createSelectOneFromDual();
    }

    @Test
    void testRewriteWithJdbcConnection() throws AdapterException, SQLException, ExaConnectionAccessException {
        mockExasolNamedConnection();
        final Connection connectionMock = mockConnection();
        final ConnectionFactory connectionFactoryMock = mock(ConnectionFactory.class);
        when(connectionFactoryMock.getConnection()).thenReturn(connectionMock);
        setConnectionNameProperty();
        final AdapterProperties properties = new AdapterProperties(this.rawProperties);
        final SqlDialect dialect = new ExasolSqlDialect(connectionFactoryMock, properties);
        final BaseRemoteMetadataReader metadataReader = new BaseRemoteMetadataReader(connectionMock, properties);
        final QueryRewriter queryRewriter = new ExasolJdbcQueryRewriter(dialect, metadataReader, connectionFactoryMock);
        assertThat(queryRewriter.rewrite(this.statement, this.exaMetadata, properties),
                equalTo("IMPORT INTO (c1 DECIMAL(18, 0)) FROM JDBC AT " + CONNECTION_NAME
                        + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }

    @Test
    void testRewriteLocal() throws AdapterException, SQLException {
        setIsLocalProperty();
        final AdapterProperties properties = new AdapterProperties(this.rawProperties);
        final SqlDialect dialect = new ExasolSqlDialect(null, properties);
        final QueryRewriter queryRewriter = new ExasolLocalQueryRewriter(dialect);
        assertThat(queryRewriter.rewrite(this.statement, this.exaMetadata, properties),
                equalTo("SELECT 1 FROM \"DUAL\""));
    }

    private void setIsLocalProperty() {
        this.rawProperties.put(IS_LOCAL_PROPERTY, "true");
    }

    @Test
    void testRewriteToImportFromExaWithConnectionDetailsInProperties()
            throws AdapterException, SQLException, ExaConnectionAccessException {
        setImportFromExaProperty();
        this.rawProperties.put(CONNECTION_STRING_PROPERTY, "irrelevant");
        this.rawProperties.put(USERNAME_PROPERTY, "alibaba");
        this.rawProperties.put(PASSWORD_PROPERTY, "open sesame");
        this.rawProperties.put(ExasolProperties.EXASOL_CONNECTION_STRING_PROPERTY, "localhost:7861");
        final AdapterProperties properties = new AdapterProperties(this.rawProperties);
        final SqlDialect dialect = new ExasolSqlDialect(null, properties);
        final QueryRewriter queryRewriter = new ExasolFromExaQueryRewriter(dialect, null, null);
        assertThat(queryRewriter.rewrite(this.statement, this.exaMetadata, properties),
                equalTo("IMPORT FROM EXA AT 'localhost:7861' USER 'alibaba' IDENTIFIED BY 'open sesame'"
                        + " STATEMENT 'SELECT 1 FROM \"DUAL\"'"));
    }

    private void setImportFromExaProperty() {
        this.rawProperties.put(EXASOL_IMPORT_PROPERTY, "true");
    }

    @Test
    void testConnectionDefinitionBuilderClass() {
        final SqlDialect dialect = new ExasolSqlDialect(null, AdapterProperties.emptyProperties());
        final QueryRewriter queryRewriter = new ExasolFromExaQueryRewriter(dialect, null, null);
        assertThat(getMethodReturnViaReflection(queryRewriter, "createConnectionDefinitionBuilder"),
                instanceOf(ExasolConnectionDefinitionBuilder.class));
    }
}