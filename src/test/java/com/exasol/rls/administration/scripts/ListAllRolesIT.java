package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_GROUP_MEMBERS_TABLE_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROLES_MAPPING_TABLE_NAME;
import static com.exasol.tools.TestsConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

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
class ListAllRolesIT extends AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>(
            EXASOL_DOCKER_IMAGE_REFERENCE).withReuse(true);

    @BeforeAll
    static void beforeAll() throws SQLException {
        initialize(EXASOL, "LIST_ALL_ROLES", PATH_TO_LIST_ALL_ROLES);
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~listing-all-roles~1]
    @Test
    void testListAllRoles() {
        try {
            schema.createTable(EXA_ROLES_MAPPING_TABLE_NAME, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)") //
                    .insert("Sales", 1) //
                    .insert("Development", 2) //
                    .insert("Finance", 3);
            assertThat(script.executeQuery(), contains(contains("Sales", (short) 1), contains("Development", (short) 2),
                    contains("Finance", (short) 3)));
        } finally {
            schema.drop();
        }
    }
}