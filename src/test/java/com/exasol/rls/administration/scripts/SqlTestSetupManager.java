package com.exasol.rls.administration.scripts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlTestSetupManager {
    private final Statement statement;

    protected SqlTestSetupManager(final Statement statement) {
        this.statement = statement;
    }

    protected void createTestSchema(final String schemaName) throws SQLException {
        this.statement.execute("CREATE SCHEMA " + schemaName);
        this.statement.execute("OPEN SCHEMA " + schemaName);
    }

    protected void createScript(final Path pathToScript) throws SQLException, IOException {
        final String script = Files.readString(pathToScript, StandardCharsets.UTF_8);
        this.statement.execute(script);
    }

    protected void createExaRolesMappingProjection(final String tableName, final String tableContent)
            throws SQLException {
        this.statement.execute("CREATE OR REPLACE TABLE " + tableName //
                + "(ROLE_NAME VARCHAR(128), " //
                + "ROLE_ID DECIMAL(2,0))");
        this.statement.execute("INSERT INTO " + tableName + " VALUES " + tableContent);
    }

    protected void cleanUpTables(final String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            this.statement.execute("DROP TABLE " + tableName + " CASCADE");
        }
    }

    protected void createExaRlsUsersProjection(final String tableName, final String tableContent) throws SQLException {
        this.statement.execute("CREATE OR REPLACE TABLE " + tableName //
                + "(EXA_USER_NAME VARCHAR(128), " //
                + "EXA_ROLE_MASK DECIMAL(20,0))");
        this.statement.execute("INSERT INTO " + tableName + " VALUES " + tableContent);
    }
}