package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_RLS_USERS_TABLE_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROLES_MAPPING_TABLE_NAME;
import static com.exasol.rls.administration.scripts.BitField64.bitsToLong;
import static com.exasol.tools.TestsConstants.PATH_TO_BIT_POSITIONS;
import static com.exasol.tools.TestsConstants.PATH_TO_LIST_USERS_AND_ROLES;
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

@Tag("integration")
@Tag("slow")
@Testcontainers
class ListUsersAndRolesIT extends AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "LIST_USERS_AND_ROLES", PATH_TO_LIST_USERS_AND_ROLES, PATH_TO_BIT_POSITIONS);
        schema.createTable(EXA_ROLES_MAPPING_TABLE_NAME, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)") //
                .insert("ROLE_1", 1) //
                .insert("ROLE_2", 2) //
                .insert("ROLE_53", 53) //
                .insert("ROLE_63", 63);
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~listing-users-and-roles~1]
    @Test
    void testListRlsUsersWithRoles() {
        schema.createTable(EXA_RLS_USERS_TABLE_NAME, "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20,0)") //
                .insert("RLS_USR_1", bitsToLong(0)) //
                .insert("RLS_USR_2", bitsToLong(0, 1)) //
                .insert("RLS_USR_3", bitsToLong(52, 62)) //
                .insert("RLE_USR_4", 0) //
                .insert("RLS_USR_5", bitsToLong(2)) //
                .insert("RLS_USR_6", bitsToLong(0, 1, 2, 3, 62));
        assertThat(script.executeQuery(), //
                contains( //
                        contains("RLS_USR_1", "ROLE_1"), //
                        contains("RLS_USR_2", "ROLE_1"), //
                        contains("RLS_USR_2", "ROLE_2"), //
                        contains("RLS_USR_3", "ROLE_53"), //
                        contains("RLS_USR_3", "ROLE_63"), //
                        contains("RLS_USR_5", "<has unmapped role(s)>"), //
                        contains("RLS_USR_6", "<has unmapped role(s)>"), //
                        contains("RLS_USR_6", "ROLE_1"), //
                        contains("RLS_USR_6", "ROLE_2"), //
                        contains("RLS_USR_6", "ROLE_63")));
    }
}