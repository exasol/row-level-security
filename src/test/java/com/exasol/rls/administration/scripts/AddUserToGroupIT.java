package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_GROUP_MEMBERS_TABLE_NAME;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.tools.TestsConstants.PATH_TO_ADD_USER_TO_GROUP;
import static com.exasol.tools.TestsConstants.PATH_TO_EXA_IDENTIFIER;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;

@Testcontainers
@Tag("integration")
@Tag("slow")
class AddUserToGroupIT extends AbstractAdminScriptIT {
    @Container
    static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "ADD_USER_TO_GROUP", PATH_TO_EXA_IDENTIFIER, PATH_TO_ADD_USER_TO_GROUP);
    }

    @AfterEach
    void afterEach() throws SQLException {
        execute("DELETE FROM " + getGroupMembersTableName());
    }

    private String getGroupMembersTableName() {
        return schema.getFullyQualifiedName() + "." + EXA_GROUP_MEMBERS_TABLE_NAME;
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~add-user-to-group~1]
    // [itest->dsn~adding-user-to-group-creates-member-table~1]
    @Test
    void testAddUserToGroup() throws SQLException {
        factory.createLoginUser("ROLF");
        factory.createLoginUser("GABI");
        script.execute("ROLF", List.of("HANDCRAFTERS", "COLLECTORS"));
        script.execute("GABI", List.of("ARTISTS", "HANDCRAFTERS"));
        assertThat(query("SELECT * FROM " + getGroupMembersTableName() + " ORDER BY EXA_USER_NAME, EXA_GROUP"), //
                table("VARCHAR", "VARCHAR") //
                        .row("GABI", "ARTISTS") //
                        .row("GABI", "HANDCRAFTERS") //
                        .row("ROLF", "COLLECTORS") //
                        .row("ROLF", "HANDCRAFTERS") //
                        .matches());
    }

    static Stream<String> illegalIdentifiers() {
        return Stream.of("", "   ", " LEADING_SPACE", "TRAILING_SPACE ", "CONTAINS SPACE", "CONTAINS-DASH",
                "$STARTS_WITH_SPECIAL_CHAR", null);
    }

    // [itest->dsn~add-user-to-group-validates-user-name~1]
    @MethodSource("produceInvalidIdentifiers")
    @ParameterizedTest
    void testAddUserToGroupValidatesUserName(final String identifier, final String quotedIdentifier) {
        assertScriptThrows("The user name " + quotedIdentifier + " is invalid. " + ALLOWED_IDENTIFIER_EXPLAINATION,
                identifier, List.of("IRRELEVANT"));
    }

    // [itest->dsn~add-user-to-group-validates-group-names~1]
    @MethodSource("produceInvalidIdentifiersInList")
    @ParameterizedTest
    void testAddUserToGroupValidatesGroups(final List<String> invalidGroupNames, final String quotedGroupNames) {
        assertScriptThrows(
                "The following group names are invalid: " + quotedGroupNames + ". " + ALLOWED_IDENTIFIER_EXPLAINATION,
                "THE_USER", invalidGroupNames);
    }
}