package com.exasol.adapter.sql;

import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

@Tag("integration")
@Testcontainers
public class AddRlsRoleIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static final Path PATH_TO_ADD_RLS_ROLE = Path.of("src/main/sql/add_rls_role.sql");
    private static final Path PATH_TO_EXA_RLS_BASE = Path.of("src/main/sql/exa_rls_base.sql");
    private static Statement statement;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        createTestSchema();
        createScript(PATH_TO_EXA_RLS_BASE);
        createScript(PATH_TO_ADD_RLS_ROLE);
    }

    private static void createTestSchema() throws SQLException {
        statement.execute("CREATE SCHEMA RLS_SCHEMA");
        statement.execute("OPEN SCHEMA RLS_SCHEMA");
    }

    private static void createScript(final Path pathToScript) throws SQLException, IOException {
        final String script = Files.readString(pathToScript, StandardCharsets.UTF_8);
        statement.execute(script);
    }

    @Test
    void testAddRlsRole() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Development', 2)");
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 3)");
        createExaRolesMappingProjection("('Sales', 1), ('Development', 2), ('Finance', 3)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM EXA_ROLES_MAPPING_PROJECTION");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM EXA_ROLES_MAPPING");
        cleanUpExaRolesMapping();
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testAddRlsRoleExistingIdException() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 1)"));
        cleanUpExaRolesMapping();
        assertThat(thrown.getMessage(), containsString("role_id \"1\" already exists"));
    }

    @Test
    void testAddRlsRoleExistingNameException() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 2)"));
        cleanUpExaRolesMapping();
        assertThat(thrown.getMessage(), containsString("role_name \"Sales\" already exists"));
    }

    @ParameterizedTest
    @ValueSource(ints = { -5, 0, 64, 70 })
    void testAddRlsRoleInvalidRoleIdException(int rlsRole) throws SQLException {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', " + rlsRole + ")"));
        cleanUpExaRolesMapping();
        assertThat(thrown.getMessage(), containsString("role_id must be between 1 and 63"));
    }

    private void createExaRolesMappingProjection(final String tableContent) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.EXA_ROLES_MAPPING_PROJECTION " //
                + "(EXA_USER_NAME VARCHAR(128), " //
                + "ROLE_ID INT)");
        statement.execute("INSERT INTO RLS_SCHEMA.EXA_ROLES_MAPPING_PROJECTION VALUES " + tableContent);
    }

    private void cleanUpExaRolesMapping() throws SQLException {
        statement.execute("DROP TABLE RLS_SCHEMA.EXA_ROLES_MAPPING CASCADE");
    }

    private void createExaRlsUsersProjection(final String tableContent) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.EXA_RLS_USERS_PROJECTION " //
                + "(ROLE_NAME VARCHAR(128), " //
                + "EXA_ROLE_MASK DECIMAL(20,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.EXA_RLS_USERS_PROJECTION VALUES " + tableContent);
    }
}
