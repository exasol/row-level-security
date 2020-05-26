package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_GROUP_MEMBERS_TABLE_NAME;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.tools.TestsConstants.PATH_TO_ADD_GROUP_MEMBER;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.*;

// [itest->dsn~add-a-new-role~1]
@Tag("integration")
public class AddUserToGroupIT extends AbstractAdminScriptIT {
    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        intitializeScript("ADD_USER_TO_GROUP", PATH_TO_ADD_GROUP_MEMBER);
    }

    @AfterEach
    void afterEach() throws SQLException {
        getStatement().execute("DELETE FROM " + EXA_GROUP_MEMBERS_TABLE_NAME);
    }

    // [itest->dsn~add-group-member-adds-a-user-to-a-group~1]
    @Test
    void testAddUserToGroup() throws SQLException {
        factory.createLoginUser("ROLF");
        factory.createLoginUser("GABI");
        script.execute("ROLF", List.of("GOLD_CARD_MEMBERS", "COLLECTORS"));
        script.execute("GABI", List.of("GOLD_CARD_MEMBERS", "SPONSORS"));
        assertThat(query("SELECT * FROM " + EXA_GROUP_MEMBERS_TABLE_NAME + " ORDER BY EXA_USER_NAME, EXA_GROUP"), //
                table("VARCHAR", "VARCHAR") //
                        .row("GABI", "GOLD_CARD_MEMBERS") //
                        .row("GABI", "SPONSORS") //
                        .row("ROLF", "COLLECTORS") //
                        .row("ROLF", "GOLD_CARD_MEMBERS") //
                        .matches());
    }
}