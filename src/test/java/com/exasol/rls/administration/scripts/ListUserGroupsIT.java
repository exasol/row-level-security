package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_GROUP_MEMBERS_TABLE_NAME;
import static com.exasol.tools.TestsConstants.EXASOL_DOCKER_IMAGE_REFERENCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.dialects.Table;
import com.exasol.tools.TestsConstants;

@Tag("integration")
@Testcontainers
class ListUserGroupsIT extends AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>(
            EXASOL_DOCKER_IMAGE_REFERENCE).withReuse(true);
    private static Table memberTable;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "LIST_USER_GROUPS", TestsConstants.PATH_TO_LIST_USER_GROUPS);
        memberTable = schema.createTable(EXA_GROUP_MEMBERS_TABLE_NAME, "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP",
                "VARCHAR(128)");
        memberTable.insert("KLAUS", "TENNIS_PLAYERS") //
                .insert("KLAUS", "SOCCER_PLAYERS") //
                .insert("VIVIANNE", "SOCCER_PLAYERS") //
                .insert("TAKESHI", "MARTIAL_ARTISTS");
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~listing-a-users-groups~1]
    @Test
    void testListRlsGroupsSingleUser() {
        assertThat(script.executeQuery("KLAUS"), contains(contains("SOCCER_PLAYERS"), contains("TENNIS_PLAYERS")));
    }
}