package com.exasol.rls.administration.scripts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.*;

import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;

import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.dialects.DatabaseObjectException;
import com.exasol.dbbuilder.dialects.exasol.*;

public abstract class AbstractAdminScriptIT {
    protected static ExasolSchema schema;
    protected static Script script;
    protected static ExasolObjectFactory factory;

    protected static void initialize(final ExasolContainer<? extends ExasolContainer<?>> container,
            final String scriptName, final Path... scriptFilePaths) throws SQLException {
        final Connection connection = container.createConnection("");
        factory = new ExasolObjectFactory(connection);
        schema = factory.createSchema(scriptName + "_SCHEMA");
        factory.executeSqlFile(scriptFilePaths);
        script = schema.getScript(scriptName);
    }

    protected abstract Connection getConnection() throws NoDriverFoundException, SQLException;

    protected void execute(final String sql) throws SQLException {
        getConnection().createStatement().execute(sql);
    }

    protected ResultSet query(final String sql) throws SQLException {
        return getConnection().createStatement().executeQuery(sql);
    }

    protected static void assertScriptThrows(final String expectedMessageFragment, final Object... parameters) {
        final Throwable exception = assertThrows(DatabaseObjectException.class, () -> script.execute(parameters));
        assertThat(exception.getCause().getMessage(), containsString(expectedMessageFragment));
    }
}