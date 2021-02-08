package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.matcher.TypeMatchMode.NO_JAVA_TYPE_CHECK;
import static com.exasol.tools.TestsConstants.PATH_TO_ASSIGN_ROLES_TO_USER;
import static com.exasol.tools.TestsConstants.PATH_TO_EXA_RLS_BASE;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;

// [itest->dsn~assign-roles-to-a-user~1]
@Tag("integration")
@Tag("slow")
@Testcontainers
class AssignRolesToUserIT extends AbstractAdminScriptIT {
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static final String EXA_RLS_USERS = "EXA_RLS_USERS";
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "ASSIGN_ROLES_TO_USER", PATH_TO_EXA_RLS_BASE, PATH_TO_ASSIGN_ROLES_TO_USER);
        schema.createTable(EXA_ROLES_MAPPING, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)") //
                .insert("Sales", 1) //
                .insert("Development", 2) //
                .insert("Finance", 3) //
                .insert("Support", 4);
    }

    @AfterEach
    void afterEach() throws SQLException {
        execute("DROP TABLE IF EXISTS " + getUserTableName());
    }

    private String getUserTableName() {
        return schema.getFullyQualifiedName() + "." + EXA_RLS_USERS;
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~assign-roles-to-user-creates-a-table~1]
    // [itest->dsn~assign-roles-to-user-creates-a-role~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestAssignRolesToUser")
    void testAssignRolesToUser(final List<String> rolesToAssign, final int maskValue) throws SQLException {
        script.execute("MONICA", rolesToAssign);
        assertThat(query("SELECT EXA_USER_NAME, EXA_ROLE_MASK FROM " + getUserTableName()), table() //
                .row("MONICA", maskValue) //
                .matches(NO_JAVA_TYPE_CHECK));
    }

    private static Stream<Arguments> provideValuesForTestAssignRolesToUser() {
        return Stream.of(Arguments.of(List.of("Sales"), 1), //
                Arguments.of(List.of("Sales", "Development"), 3), //
                Arguments.of(List.of("Sales", "Support"), 9), //
                Arguments.of(List.of("Sales", "Development", "Finance", "Support"), 15));
    }

    // [itest->dsn~assign-roles-to-user-creates-a-role~1]
    @Test
    void testAssignRolesToUserUpdatesUserRoles() throws SQLException {
        script.execute("NORBERT", List.of("Sales", "Development"));
        script.execute("NORBERT", List.of("Sales"));
        assertThat(query("SELECT EXA_USER_NAME, EXA_ROLE_MASK FROM " + getUserTableName()), table() //
                .row("NORBERT", 1) //
                .matches(NO_JAVA_TYPE_CHECK));
    }

    // [itest->dsn~assign-roles-to-user-creates-a-role~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestAssignUnknownRoleToUserThrowsRoleNotFoundException")
    void testAssignUnknownRoleToUserThrowsRoleNotFoundException(final List<String> allRoles,
            final List<String> unknownRoles) {
        assertScriptThrows("The following roles were not found: " + String.join(", ", unknownRoles), "THE_USER",
                allRoles);
    }

    private static Stream<Arguments> provideValuesForTestAssignUnknownRoleToUserThrowsRoleNotFoundException() {
        return Stream.of(Arguments.of(List.of("Cats"), List.of("Cats")), //
                Arguments.of(List.of("Cats", "Sales"), List.of("Cats")), //
                Arguments.of(List.of("Sales", "Cats", "Dogs"), List.of("Dogs", "Cats")));
    }
}