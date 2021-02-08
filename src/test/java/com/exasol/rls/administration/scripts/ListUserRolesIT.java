package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_RLS_USERS_TABLE_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROLES_MAPPING_TABLE_NAME;
import static com.exasol.tools.TestsConstants.PATH_TO_BIT_POSITIONS;
import static com.exasol.tools.TestsConstants.PATH_TO_LIST_USER_ROLES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;

@Tag("integration")
@Tag("slow")
@Testcontainers
class ListUserRolesIT extends AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "LIST_USER_ROLES", PATH_TO_LIST_USER_ROLES, PATH_TO_BIT_POSITIONS);
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~listing-user-roles~1]
    @Test
    void testListRlsSingleUser() {
        try {
            schema.createTable(EXA_ROLES_MAPPING_TABLE_NAME, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)") //
                    .insert("Sales", 1) //
                    .insert("Development", 2) //
                    .insert("Finance", 3) //
                    .insert("Support", 4);
            schema.createTable(EXA_RLS_USERS_TABLE_NAME, "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK",
                    "DECIMAL(20,0)") //
                    .insert("RLS_USR_1", 1) //
                    .insert("RLS_USR_2", 3) //
                    .insert("RLS_USR_3", 12);
            assertAll(() -> assertThat(script.executeQuery("RLS_USR_1"), contains( //
                    contains("RLS_USR_1", "Sales"))), //
                    () -> assertThat(script.executeQuery("RLS_USR_2"), contains( //
                            contains("RLS_USR_2", "Sales"), //
                            contains("RLS_USR_2", "Development") //
                    )), () -> assertThat(script.executeQuery("RLS_USR_3"), contains( //
                            contains("RLS_USR_3", "Finance"), //
                            contains("RLS_USR_3", "Support") //
                    )));
        } finally {
            schema.drop();
        }
    }
}