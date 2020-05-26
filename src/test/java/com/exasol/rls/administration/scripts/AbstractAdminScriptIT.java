package com.exasol.rls.administration.scripts;

import static com.exasol.tools.TestsConstants.PATH_TO_EXA_RLS_BASE;

import java.nio.file.Path;
import java.sql.*;

import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;
import com.exasol.dbbuilder.*;

@Testcontainers
public class AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static Statement statement;
    private static Connection connection;
    protected static Script script;
    protected static DatabaseObjectFactory factory;

    protected static synchronized Connection getConnection() throws NoDriverFoundException, SQLException {
        if (connection == null) {
            connection = container.createConnection("");
        }
        return connection;
    }

    protected static Statement getStatement() throws NoDriverFoundException, SQLException {
        if (statement == null) {
            statement = getConnection().createStatement();
        }
        return statement;
    }

    protected static void intitializeScript(final String scriptName, final Path scriptPath) throws SQLException {
        factory = new ExasolObjectFactory(getConnection());
        final Schema schema = factory.createSchema(scriptName + "_SCHEMA");
        factory.executeSqlFile(PATH_TO_EXA_RLS_BASE, scriptPath);
        script = schema.getScript(scriptName);
    }

    protected ResultSet query(final String sql) throws SQLException {
        return getStatement().executeQuery(sql);
    }
}