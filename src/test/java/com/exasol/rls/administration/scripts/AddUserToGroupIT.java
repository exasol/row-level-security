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
import org.junit.jupiter.params.provider.CsvSource;
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
    @CsvSource({ "'',<null>", "'   ','\"   \"'", "' LEADING_SPACE','\" LEADING_SPACE\"'",
            "'TRAILING_SPACE ', '\"TRAILING_SPACE \"'", "'CONTAINS SPACE','\"CONTAINS SPACE\"'",
            "'CONTAINS-DASH','\"CONTAINS-DASH\"'", "'$STARTS_WITH_SPECIAL_CHAR','\"$STARTS_WITH_SPECIAL_CHAR\"'",
            ",'<null>'" })
    @ParameterizedTest
    void testAddUserToGroupValidatesUserName(final String identifier, final String mangledIdentifier) {
        assertScriptThrows(
                "Invalid username " + mangledIdentifier
                        + ". Must be a valid identifier (numbers, letters and underscores only).",
                identifier, List.of("IRRELEVANT"));
    }

    // [itest->dsn~add-user-to-group-validates-group-names~1]
    @CsvSource({ "'',<null>", "'   ','\"   \"'", "' LEADING_SPACE','\" LEADING_SPACE\"'",
            "'TRAILING_SPACE ', '\"TRAILING_SPACE \"'", "'CONTAINS SPACE','\"CONTAINS SPACE\"'",
            "'CONTAINS-DASH','\"CONTAINS-DASH\"'", "'$STARTS_WITH_SPECIAL_CHAR','\"$STARTS_WITH_SPECIAL_CHAR\"'" })
    @ParameterizedTest
    void testAddUserToGroupValidatesGroups(final String identifier, final String mangledIdentifier) {
        assertScriptThrows("Groups found that are not valid identifiers (numbers, letters and underscores only): "
                + mangledIdentifier, "THE_USER", List.of("GROUP_A", identifier, "GROUP_C"));
    }
}