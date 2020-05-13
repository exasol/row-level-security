package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static com.exasol.tools.TestsConstants.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.sql.*;

import com.exasol.tools.SqlTestSetupManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;
import com.exasol.dbbuilder.*;

// [itest->dsn~add-a-new-role~1]
@Tag("integration")
@Testcontainers
public class AddRlsRoleIT {
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static final String EXA_ROLES_MAPPING_PROJECTION = "EXA_ROLES_MAPPING_PROJECTION";
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static Statement statement;
    private static SqlTestSetupManager sqlTestSetupManager;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        final DatabaseObjectFactory factory = new ExasolObjectFactory(connection);
        final Schema scriptSchema = factory.createSchema("SCRIPT_SCHEMA");
        scriptSchema.
    }

    // [itest->dsn~add-rls-role-creates-a-table~1]
    @Test
    void testAddRlsRole() throws SQLException {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("SELECT * FROM " + EXA_ROLES_MAPPING));
        assertThat(thrown.getMessage(), containsString("object EXA_ROLES_MAPPING not found"));
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Development', 2)");
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 3)");
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING_PROJECTION,
                "('Sales', 1), ('Development', 2), ('Finance', 3)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + EXA_ROLES_MAPPING_PROJECTION);
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM " + EXA_ROLES_MAPPING);
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING, EXA_ROLES_MAPPING_PROJECTION);
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @Test
    void testAddRlsRoleExistingIdException() throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 1)"));
        assertThat(thrown.getMessage(), containsString("Role id 1 already exists (role name \"Sales\")."));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING);
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @ParameterizedTest
    @ValueSource(strings = { "SALES", "Sales", "sales" })
    void testAddRlsRoleExistingNameException(final String role_name) throws SQLException {
        statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('" + role_name + "', 2)"));
        assertThat(thrown.getMessage(), containsString("Role name \"" + role_name + "\" already exists (role id 1)."));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING);
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @ParameterizedTest
    @ValueSource(ints = { -5, 0, 64, 70 })
    void testAddRlsRoleInvalidRoleIdException(final int rlsRole) throws SQLException {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ADD_RLS_ROLE('Sales', " + rlsRole + ")"));
        assertThat(thrown.getMessage(), containsString("Invalid role id. Role id must be between 1 and 63."));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING);
    }
}