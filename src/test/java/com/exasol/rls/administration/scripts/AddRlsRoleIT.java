package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROLES_MAPPING_TABLE_NAME;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.tools.TestsConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;

// [itest->dsn~add-a-new-role~1]
@Tag("integration")
@Testcontainers
class AddRlsRoleIT extends AbstractAdminScriptIT {
    @Container
    static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>(
            EXASOL_DOCKER_IMAGE_REFERENCE).withReuse(true);

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "ADD_RLS_ROLE", PATH_TO_EXA_RLS_BASE, PATH_TO_ADD_RLS_ROLE);
    }

    @AfterEach
    void afterEach() throws SQLException {
        execute("DELETE FROM " + getRolesMappingTableName());
    }

    private String getRolesMappingTableName() {
        return schema.getFullyQualifiedName() + "." + EXA_ROLES_MAPPING_TABLE_NAME;
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~add-rls-role-creates-a-table~1]
    @Test
    void testAddRlsRole() throws SQLException {
        script.execute("Sales", 1);
        script.execute("Development", 2);
        script.execute("Finance", 3);
        assertThat(query("SELECT * FROM " + getRolesMappingTableName()), //
                table("VARCHAR", "SMALLINT") //
                        .row("Sales", (short) 1) //
                        .row("Development", (short) 2) //
                        .row("Finance", (short) 3) //
                        .matches());
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @Test
    void testAddRlsRoleExistingIdException() throws SQLException {
        script.execute("Sales", 1);
        assertScriptThrows("Role id 1 already exists (role name \"Sales\").", "Finance", 1);
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @ParameterizedTest
    @ValueSource(strings = { "SALES", "Sales", "sales" })
    void testAddRlsRoleExistingNameException(final String roleName) throws SQLException {
        script.execute("Sales", 1);
        assertScriptThrows("Role name \"" + roleName + "\" already exists (role id 1).", roleName, 2);
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @ParameterizedTest
    @ValueSource(ints = { -5, 0, 64, 70 })
    void testAddRlsRoleInvalidRoleIdException(final int rlsRole) throws SQLException {
        assertScriptThrows("Invalid role id. Role id must be between 1 and 63.", "Sales", rlsRole);
    }
}