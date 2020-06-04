package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_GROUP_MEMBERS_TABLE_NAME;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.tools.TestsConstants.PATH_TO_EXA_IDENTIFIER;
import static com.exasol.tools.TestsConstants.PATH_TO_REMOVE_USER_FROM_GROUP;
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
import com.exasol.dbbuilder.Table;

@Testcontainers
@Tag("integration")
public class RemoveUserFromGroupIT extends AbstractAdminScriptIT {
    @Container
    static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>();
    private Table memberTable;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(container, "REMOVE_USER_FROM_GROUP", PATH_TO_EXA_IDENTIFIER, PATH_TO_REMOVE_USER_FROM_GROUP);
    }

    @BeforeEach
    void beforeEach() {
        this.memberTable = schema.createTable(EXA_GROUP_MEMBERS_TABLE_NAME, "EXA_USER_NAME", "VARCHAR(128)",
                "EXA_GROUP", "VARCHAR(128)");
    }

    @AfterEach
    void afterEach() throws SQLException {
        execute("DROP TABLE " + this.memberTable.getFullyQualifiedName());
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return container.createConnection("");
    }

    @Test
    void testRemoveUserFromGroup() throws SQLException {
        this.memberTable.insert("ROLF", "ARTISTS") //
                .insert("ROLF", "HANDCRAFTERS") //
                .insert("ROLF", "TEACHERS") //
                .insert("GABI", "HANDCRAFTERS");
        script.execute("ROLF", List.of("ARTISTS", "HANDCRAFTERS"));
        assertThat(
                query("SELECT * FROM " + this.memberTable.getFullyQualifiedName()
                        + " ORDER BY EXA_USER_NAME, EXA_GROUP"), //
                table("VARCHAR", "VARCHAR") //
                        .row("GABI", "HANDCRAFTERS") //
                        .row("ROLF", "TEACHERS") //
                        .matches());
    }

    static Stream<String> illegalIdentifiers() {
        return Stream.of("", "   ", " LEADING_SPACE", "TRAILING_SPACE ", "CONTAINS SPACE", "CONTAINS-DASH",
                "$STARTS_WITH_SPECIAL_CHAR", null);
    }

    @CsvSource({ "'',<null>", //
            "'   ','\"   \"'", //
            "' LEADING_SPACE','\" LEADING_SPACE\"'", //
            "'TRAILING_SPACE ', '\"TRAILING_SPACE \"'", //
            "'CONTAINS SPACE','\"CONTAINS SPACE\"'", //
            "'CONTAINS-DASH','\"CONTAINS-DASH\"'", //
            "'$STARTS_WITH_SPECIAL_CHAR','\"$STARTS_WITH_SPECIAL_CHAR\"'", //
            ",'<null>'" })
    @ParameterizedTest
    void testRemoveUserFromGroupValidatesUserName(final String identifier, final String mangledIdentifier) {
        assertScriptThrows(
                "Invalid username " + mangledIdentifier
                        + ". Must be a valid identifier (numbers, letters and underscores only).",
                identifier, List.of("IRRELEVANT"));
    }

    @CsvSource({ "'',<null>", //
            "'   ','\"   \"'", //
            "' LEADING_SPACE','\" LEADING_SPACE\"'", //
            "'TRAILING_SPACE ', '\"TRAILING_SPACE \"'", //
            "'CONTAINS SPACE','\"CONTAINS SPACE\"'", //
            "'CONTAINS-DASH','\"CONTAINS-DASH\"'", //
            "'$STARTS_WITH_SPECIAL_CHAR','\"$STARTS_WITH_SPECIAL_CHAR\"'" })
    @ParameterizedTest
    void testRemoveUserFromGroupValidatesGroups(final String identifier, final String mangledIdentifier) {
        assertScriptThrows("Groups found that are not valid identifiers (numbers, letters and underscores only): "
                + mangledIdentifier, "THE_USER", List.of("GROUP_A", identifier, "GROUP_C"));
    }
}