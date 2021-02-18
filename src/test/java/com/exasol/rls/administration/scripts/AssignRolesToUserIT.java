package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.matcher.TypeMatchMode.NO_JAVA_TYPE_CHECK;
import static com.exasol.tools.TestsConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
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
        initialize(EXASOL, "ASSIGN_ROLES_TO_USER", PATH_TO_EXA_RLS_BASE, PATH_TO_EXA_IDENTIFIER,
                PATH_TO_ASSIGN_ROLES_TO_USER);
        schema.createTable(EXA_ROLES_MAPPING, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)") //
                .insert("role_1", 1) //
                .insert("role_2", 2) //
                .insert("role_3", 3) //
                .insert("role_4", 4) //
                .insert("role_53", 53) //
                .insert("role_63", 63);
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
    void testAssignRolesToUser(final List<String> rolesToAssign, final long maskValue) throws SQLException {
        script.execute("MONICA", rolesToAssign);
        assertThat(query("SELECT EXA_USER_NAME, EXA_ROLE_MASK FROM " + getUserTableName()), table() //
                .row("MONICA", maskValue) //
                .matches(NO_JAVA_TYPE_CHECK));
    }

    private static Stream<Arguments> provideValuesForTestAssignRolesToUser() {
        return Stream.of(Arguments.of(List.of("role_1"), 1), //
                Arguments.of(List.of("role_1", "role_2"), 3), //
                Arguments.of(List.of("role_1", "role_4"), 9), //
                Arguments.of(List.of("role_1", "role_2", "role_3", "role_4"), 15), //
                Arguments.of(List.of("role_2", "role_3", "role_53", "role_63"),
                        BitField64.ofIndices(1, 2, 52, 62).toLong()));
    }

    // [itest->dsn~assign-roles-to-user-creates-a-role~1]
    @Test
    void testAssignRolesToUserUpdatesUserRoles() throws SQLException {
        script.execute("NORBERT", List.of("role_1", "role_2"));
        script.execute("NORBERT", List.of("role_1"));
        assertThat(query("SELECT EXA_USER_NAME, EXA_ROLE_MASK FROM " + getUserTableName()), table() //
                .row("NORBERT", 1) //
                .matches(NO_JAVA_TYPE_CHECK));
    }

    // [itest->dsn~assign-roles-to-user-creates-a-role~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestAssignIllegalRoleToUserThrowsException")
    void testAssignIllegalRoleToUserThrowsException(final List<String> allRoles, final List<String> unknownRoles) {
        assertScriptThrows("The following role names are not valid identifiers: \""
                + String.join("\", \"", unknownRoles) + "\". Use numbers, letters and underscores only.", "THE_USER",
                allRoles);
    }

    private static Stream<Arguments> provideValuesForTestAssignIllegalRoleToUserThrowsException() {
        return Stream.of(Arguments.of(List.of("/Cats"), List.of("/Cats")), //
                Arguments.of(List.of("Cats%", "role_1"), List.of("Cats%")), //
                Arguments.of(List.of("role_1", "Cat&&s", "Dog§§s", "Mi ce"), List.of("Cat&&s", "Dog§§s", "Mi ce")));
    }

    @ValueSource(strings = { "Rabb!it", "K@ng@roo", "El ephant" })
    @ParameterizedTest
    void testAssingingToIllegalUserThrowsException(final String userName) {
        assertScriptThrows(
                "The user name \"" + userName
                        + "\" is not a valid identifier. Use numbers, letters and underscores only.",
                userName, List.of("role_1"));
    }
}