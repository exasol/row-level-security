package com.exasol.rls.administration.scripts;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROLES_MAPPING_TABLE_NAME;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.tools.TestsConstants.PATH_TO_ADD_RLS_ROLE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.sql.SQLException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.dbbuilder.DatabaseObjectException;

// [itest->dsn~add-a-new-role~1]
@Tag("integration")
public class AddRlsRoleIT extends AbstractAdminScriptIT {
    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        intitializeScript("ADD_RLS_ROLE", PATH_TO_ADD_RLS_ROLE);
    }

    @AfterEach
    void afterEach() throws SQLException {
        getStatement().execute("DELETE FROM " + EXA_ROLES_MAPPING_TABLE_NAME);
    }

    // [itest->dsn~add-rls-role-creates-a-table~1]
    @Test
    void testAddRlsRole() throws SQLException {
        script.execute("Sales", 1);
        script.execute("Development", 2);
        script.execute("Finance", 3);
        assertThat(query("SELECT * FROM " + EXA_ROLES_MAPPING_TABLE_NAME), //
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
        final Throwable thrown = assertThrows(DatabaseObjectException.class, () -> script.execute("Finance", 1))
                .getCause();
        assertThat(thrown.getMessage(), containsString("Role id 1 already exists (role name \"Sales\")."));
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @ParameterizedTest
    @ValueSource(strings = { "SALES", "Sales", "sales" })
    void testAddRlsRoleExistingNameException(final String role_name) throws SQLException {
        script.execute("Sales", 1);
        final Throwable thrown = assertThrows(DatabaseObjectException.class,
                () -> script.execute(script.execute(role_name, 2))).getCause();
        assertThat(thrown.getMessage(), containsString("Role name \"" + role_name + "\" already exists (role id 1)."));
    }

    // [itest->dsn~add-rls-roles-checks-parameters~1]
    @ParameterizedTest
    @ValueSource(ints = { -5, 0, 64, 70 })
    void testAddRlsRoleInvalidRoleIdException(final int rlsRole) throws SQLException {
        final Throwable thrown = assertThrows(DatabaseObjectException.class, () -> script.execute("Sales", rlsRole))
                .getCause();
        assertThat(thrown.getMessage(), containsString("Invalid role id. Role id must be between 1 and 63."));
    }
}