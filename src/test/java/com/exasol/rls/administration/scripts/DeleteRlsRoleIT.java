package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static com.exasol.tools.TestsConstants.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.sql.*;
import java.util.stream.Stream;

import com.exasol.tools.SqlTestSetupManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

// [itest->dsn~delete-a-role~1]
@Tag("integration")
@Testcontainers
public class DeleteRlsRoleIT {
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static final String EXA_ROLES_MAPPING_PROJECTION = "EXA_ROLES_MAPPING_PROJECTION";
    private static final String EXA_RLS_USERS = "EXA_RLS_USERS";
    private static final String EXA_RLS_USERS_PROJECTION = "EXA_RLS_USERS_PROJECTION";
    private static SqlTestSetupManager sqlTestSetupManager;

    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static Statement statement;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        sqlTestSetupManager = new SqlTestSetupManager(statement);
        sqlTestSetupManager.createTestSchema(RLS_SCHEMA_NAME);
        sqlTestSetupManager.createScript(PATH_TO_EXA_RLS_BASE);
        sqlTestSetupManager.createScript(PATH_TO_DELETE_RLS_ROLE);
    }

    // [itest->dsn~delete-rls-role-removes-a-role-from-administrative-tables~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestDeleteRlsRoleFromExaRolesMapping")
    void testDeleteRlsRoleFromExaRolesMapping(final String roleToDelete, final String expectedTableContent)
            throws SQLException {
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING,
                "('Sales', 1), ('Development', 2), ('Finance', 3),  ('Support', 4)");
        statement.execute("EXECUTE SCRIPT DELETE_RLS_ROLE(" + roleToDelete + ")");
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING_PROJECTION, expectedTableContent);
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + EXA_ROLES_MAPPING_PROJECTION);
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + EXA_ROLES_MAPPING + " ORDER BY ROLE_ID");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING, EXA_ROLES_MAPPING_PROJECTION);
    }

    private static Stream<Arguments> provideValuesForTestDeleteRlsRoleFromExaRolesMapping() {
        return Stream.of(Arguments.of("'Sales'", "('Development', 2), ('Finance', 3),  ('Support', 4)"), //
                Arguments.of("'Development'", "('Sales', 1), ('Finance', 3),  ('Support', 4)"), //
                Arguments.of("'Support'", "('Sales', 1), ('Development', 2), ('Finance', 3)"));
    }

    // [itest->dsn~delete-rls-role-removes-a-role-from-administrative-tables~1]
    @Test
    void testDeleteRlsRoleUnknownRole() throws SQLException {
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING, "('Sales', 1), ('Development', 2)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT DELETE_RLS_ROLE('Support')"));
        assertThat(thrown.getMessage(), containsString("No such role name: \"Support\"."));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING);
    }

    // [itest->dsn~delete-rls-role-removes-a-role-from-administrative-tables~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestDeleteRlsRoleFromExaRlsUsers")
    void testDeleteRlsRoleFromExaRlsUsers(final String roleToDelete, final String expectedTableContent)
            throws SQLException {
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING,
                "('Sales', 1), ('Development', 2), ('Finance', 3),  ('Support', 4)");
        sqlTestSetupManager.createExaRlsUsersProjection(EXA_RLS_USERS, "('RLS_USR_1', 15), ('RLS_USR_2', 9)");
        statement.execute("EXECUTE SCRIPT DELETE_RLS_ROLE(" + roleToDelete + ")");
        sqlTestSetupManager.createExaRlsUsersProjection(EXA_RLS_USERS_PROJECTION, expectedTableContent);
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + EXA_RLS_USERS_PROJECTION);
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM " + EXA_RLS_USERS);
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING, EXA_RLS_USERS, EXA_RLS_USERS_PROJECTION);
    }

    private static Stream<Arguments> provideValuesForTestDeleteRlsRoleFromExaRlsUsers() {
        return Stream.of(Arguments.of("'Sales'", "('RLS_USR_1', 14), ('RLS_USR_2', 8)"), //
                Arguments.of("'Development'", "('RLS_USR_1', 13), ('RLS_USR_2', 9)"), //
                Arguments.of("'Finance'", "('RLS_USR_1', 11), ('RLS_USR_2', 9)"), //
                Arguments.of("'Support'", "('RLS_USR_1', 7), ('RLS_USR_2', 1)"));
    }

    // [itest->dsn~delete-rls-role-removes-a-role-from-user-tables~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestDeleteRlsRoleFromPayloadTable")
    void testDeleteRlsRoleFromPayloadTable(final String roleToDelete, final String expectedTableContent)
            throws SQLException {
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING,
                "('Sales', 1), ('Development', 2), ('Finance', 3),  ('Support', 4)");
        final String test_table_name = "TEST_TABLE";
        createUserTable(test_table_name, "('Row1', 1), ('Row2', 6), ('Row3', 9),  ('Row4', 15)");
        final String test_table_projection_name = "TEST_TABLE_PROJECTION";
        createUserTable(test_table_projection_name, expectedTableContent);
        statement.execute("EXECUTE SCRIPT DELETE_RLS_ROLE(" + roleToDelete + ")");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + test_table_projection_name);
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM " + test_table_name);
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
        sqlTestSetupManager.cleanUpTables(EXA_ROLES_MAPPING, test_table_name, test_table_projection_name);
    }

    private void createUserTable(final String tableName, final String tableContent) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + tableName //
                + "(TEST_COLUMN VARCHAR(128), " //
                + "EXA_ROW_ROLES DECIMAL(20,0))");
        statement.execute("INSERT INTO " + tableName + " VALUES " //
                + tableContent);
    }

    private static Stream<Arguments> provideValuesForTestDeleteRlsRoleFromPayloadTable() {
        return Stream.of(Arguments.of("'Sales'", "('Row1', 0), ('Row2', 6), ('Row3', 9),  ('Row4', 14)"), //
                Arguments.of("'Development'", "('Row1', 1), ('Row2', 4), ('Row3', 9),  ('Row4', 13)"), //
                Arguments.of("'Finance'", "('Row1', 1), ('Row2', 2), ('Row3', 9),  ('Row4', 11)"), //
                Arguments.of("'Support'", "('Row1', 1), ('Row2', 6), ('Row3', 1),  ('Row4', 7)"));
    }
}