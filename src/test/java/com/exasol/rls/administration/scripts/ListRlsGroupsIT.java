package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_GROUP_MEMBERS_TABLE_NAME;
import static com.exasol.tools.TestsConstants.PATH_TO_LIST_RLS_GROUPS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.Table;

@Tag("integration")
@Testcontainers
public class ListRlsGroupsIT extends AbstractAdminScriptIT {
    @Container
    static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>();

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(container, "LIST_RLS_GROUPS", PATH_TO_LIST_RLS_GROUPS);
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return container.createConnection("");
    }

    // [itest->dsn~list-groups~1]
    @Test
    void testListRlsGroups() {
        final Table memberTable = schema.createTable(EXA_GROUP_MEMBERS_TABLE_NAME, "EXA_USER_NAME", "VARCHAR(128)",
                "EXA_GROUP", "VARCHAR(128)");
        memberTable.insert("KLAUS", "TENNIS_PLAYERS") //
                .insert("KLAUS", "SOCCER_PLAYERS") //
                .insert("VIVIANNE", "SOCCER_PLAYERS") //
                .insert("TAKESHI", "MARTIAL_ARTISTS");
        final List<List<Object>> result = script.executeQuery();
        assertThat(result, contains(contains("MARTIAL_ARTISTS", 1L), contains("SOCCER_PLAYERS", 2L),
                contains("TENNIS_PLAYERS", 1L)));
    }
}