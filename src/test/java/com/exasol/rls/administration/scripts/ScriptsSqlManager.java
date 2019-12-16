package com.exasol.rls.administration.scripts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

public class ScriptsSqlManager {
    protected static void createTestSchema(final Statement statement, final String schemaName) throws SQLException {
        statement.execute("CREATE SCHEMA " + schemaName);
        statement.execute("OPEN SCHEMA " + schemaName);
    }

    protected static void createScript(final Statement statement, final Path pathToScript)
            throws SQLException, IOException {
        final String script = Files.readString(pathToScript, StandardCharsets.UTF_8);
        statement.execute(script);
    }

    protected static void createExaRolesMappingProjection(final Statement statement, final String tableName,
            final String tableContent) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + tableName //
                + "(ROLE_NAME VARCHAR(128), " //
                + "ROLE_ID INT)");
        statement.execute("INSERT INTO " + tableName + " VALUES " + tableContent);
    }

    protected static void dropTable(final Statement statement, final String tableName) throws SQLException {
        statement.execute("DROP TABLE " + tableName + " CASCADE");
    }

    protected static void createExaRlsUsersProjection(final Statement statement, final String tableName,
            final String tableContent) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + tableName //
                + "(EXA_USER_NAME VARCHAR(128), " //
                + "EXA_ROLE_MASK DECIMAL(20,0))");
        statement.execute("INSERT INTO " + tableName + " VALUES " + tableContent);
    }
}
