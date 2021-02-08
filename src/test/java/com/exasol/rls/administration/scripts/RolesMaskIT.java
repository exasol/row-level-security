package com.exasol.rls.administration.scripts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.dialects.Table;
import com.exasol.tools.TestsConstants;

// [itest->dsn~get-a-role-mask~1]
@Tag("integration")
@Tag("slow")
@Testcontainers
class RolesMaskIT extends AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static Table table;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "ROLES_MASK", TestsConstants.PATH_TO_ROLES_MASK);
        table = schema.createTable(EXA_ROLES_MAPPING, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)") //
                .insert("Sales", 1) //
                .insert("Development", 2) //
                .insert("Finance", 3) //
                .insert("Support", 4);
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~get-a-role-mask~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestRolesMask")
    void testRolesMask(final String roles, final int maskValue) throws SQLException {
        final ResultSet actualResultSet = query("SELECT " + schema.getName() + ".ROLES_MASK(ROLE_ID) from "
                + table.getFullyQualifiedName() + " WHERE ROLE_NAME IN (" + roles + ")");
        actualResultSet.next();
        assertThat(actualResultSet.getInt(1), equalTo(maskValue));
    }

    private static Stream<Arguments> provideValuesForTestRolesMask() {
        return Stream.of(Arguments.of("'Sales'", 1), //
                Arguments.of("'Sales', 'Development'", 3), //
                Arguments.of("'Sales', 'Support'", 9), //
                Arguments.of("'Sales', 'Development', 'Finance', 'Support'", 15));
    }
}