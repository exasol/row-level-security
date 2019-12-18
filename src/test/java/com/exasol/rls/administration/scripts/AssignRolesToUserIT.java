package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static com.exasol.rls.administration.scripts.IntegrationTestsConstants.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.sql.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

@Tag("integration")
@Testcontainers
public class AssignRolesToUserIT {
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static final String EXA_RLS_USERS = "EXA_RLS_USERS";
    private static final String EXA_RLS_USERS_PROJECTION = "EXA_RLS_USERS_PROJECTION";
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
        sqlTestSetupManager = new SqlTestSetupManager(statement);
        sqlTestSetupManager.createTestSchema(RLS_SCHEMA_NAME);
        sqlTestSetupManager.createScript(PATH_TO_EXA_RLS_BASE);
        sqlTestSetupManager.createScript(PATH_TO_ASSIGN_ROLES_TO_USER);
        sqlTestSetupManager.createExaRolesMappingProjection(EXA_ROLES_MAPPING,
                "('Sales', 1), ('Development', 2), ('Finance', 3),  ('Support', 4)");
    }

    @ParameterizedTest
    @MethodSource("provideValuesForTestAssignRolesToUser")
    void testAssignRolesToUser(final String rolesToAssign, final int maskValue) throws SQLException {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("SELECT * FROM " + EXA_RLS_USERS));
        assertThat(thrown.getMessage(), containsString("object EXA_RLS_USERS not found"));
        statement.execute("EXECUTE SCRIPT ASSIGN_ROLES_TO_USER('RLS_USR_1', ARRAY(" + rolesToAssign + "))");
        sqlTestSetupManager.createExaRlsUsersProjection(EXA_RLS_USERS_PROJECTION, "('RLS_USR_1', " + maskValue + ")");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + EXA_RLS_USERS_PROJECTION);
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM " + EXA_RLS_USERS);
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
        sqlTestSetupManager.cleanUpTables(EXA_RLS_USERS_PROJECTION, EXA_RLS_USERS);
    }

    private static Stream<Arguments> provideValuesForTestAssignRolesToUser() {
        return Stream.of(Arguments.of("'Sales'", 1), //
                Arguments.of("'Sales', 'Development'", 3), //
                Arguments.of("'Sales', 'Support'", 9), //
                Arguments.of("'Sales', 'Development', 'Finance', 'Support'", 15));
    }

    @Test
    void testAssignRolesToUserUpdatesUserRoles() throws SQLException {
        statement.execute("EXECUTE SCRIPT ASSIGN_ROLES_TO_USER('RLS_USR_1', ARRAY('Sales', 'Development'))");
        statement.execute("EXECUTE SCRIPT ASSIGN_ROLES_TO_USER('RLS_USR_1', ARRAY('Sales'))");
        sqlTestSetupManager.createExaRlsUsersProjection(EXA_RLS_USERS_PROJECTION, "('RLS_USR_1', 1)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM " + EXA_RLS_USERS_PROJECTION);
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM " + EXA_RLS_USERS);
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
        sqlTestSetupManager.cleanUpTables(EXA_RLS_USERS_PROJECTION, EXA_RLS_USERS);
    }

    @ParameterizedTest
    @MethodSource("provideValuesForTestAssignUnknownRoleToUserThrowsRoleNotFoundException")
    void testAssignUnknownRoleToUserThrowsRoleNotFoundException(final String allRoles, final String unknownRoles) {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("EXECUTE SCRIPT ASSIGN_ROLES_TO_USER('RLS_USR_1', ARRAY(" + allRoles + "))"));
        assertThat(thrown.getMessage(), containsString("The following roles were not found: " + unknownRoles));
    }

    private static Stream<Arguments> provideValuesForTestAssignUnknownRoleToUserThrowsRoleNotFoundException() {
        return Stream.of(Arguments.of("'Cats'", "Cats"), //
                Arguments.of("'Cats', 'Sales'", "Cats"), //
                Arguments.of("'Sales', 'Cats', 'Dogs'", "Dogs, Cats"));
    }
}