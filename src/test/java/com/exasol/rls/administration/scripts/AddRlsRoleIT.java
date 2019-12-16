package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;

import org.junit.jupiter.api.*;
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
    private static final String RLS_SCHEMA_NAME = "RLS_SCHEMA";
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static final String EXA_ROLES_MAPPING_PROJECTION = "EXA_ROLES_MAPPING_PROJECTION";
    private static Statement statement;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        ScriptsSqlManager.createTestSchema(statement, RLS_SCHEMA_NAME);
        ScriptsSqlManager.createScript(statement, PATH_TO_EXA_RLS_BASE);
        ScriptsSqlManager.createScript(statement, PATH_TO_ADD_RLS_ROLE);
    }

    @Test
    void testAddRlsRole() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Development', 2)");
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 3)");
        ScriptsSqlManager.createExaRolesMappingProjection(statement, EXA_ROLES_MAPPING_PROJECTION,
                "('Sales', 1), ('Development', 2), ('Finance', 3)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + EXA_ROLES_MAPPING_PROJECTION);
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM " + EXA_ROLES_MAPPING);
        ScriptsSqlManager.dropTable(statement, EXA_ROLES_MAPPING);
        ScriptsSqlManager.dropTable(statement, EXA_ROLES_MAPPING_PROJECTION);
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testAddRlsRoleExistingIdException() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 1)"));
        ScriptsSqlManager.dropTable(statement, EXA_ROLES_MAPPING);
        assertThat(thrown.getMessage(), containsString("role_id \"1\" already exists"));
    }

    @Test
    void testAddRlsRoleExistingNameException() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 2)"));
        ScriptsSqlManager.dropTable(statement, EXA_ROLES_MAPPING);
        assertThat(thrown.getMessage(), containsString("role_name \"Sales\" already exists"));
    }

    @ParameterizedTest
    @ValueSource(ints = { -5, 0, 64, 70 })
    void testAddRlsRoleInvalidRoleIdException(final int rlsRole) throws SQLException {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', " + rlsRole + ")"));
        ScriptsSqlManager.dropTable(statement, EXA_ROLES_MAPPING);
        assertThat(thrown.getMessage(), containsString("role_id must be between 1 and 63"));
    }
}
