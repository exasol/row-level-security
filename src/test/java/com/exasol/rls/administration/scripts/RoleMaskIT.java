package com.exasol.rls.administration.scripts;

import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.matcher.TypeMatchMode.NO_JAVA_TYPE_CHECK;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
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
class RoleMaskIT extends AbstractAdminScriptIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    private static Table table;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        initialize(EXASOL, "ROLES_MASK", TestsConstants.PATH_TO_ROLE_MASK);
        table = schema.createTable(EXA_ROLES_MAPPING, "ROLE_NAME", "VARCHAR(128)", "ROLE_ID", "DECIMAL(2,0)");
        for (int roleId = 1; roleId < 64; ++roleId) {
            table.insert("role_" + roleId, roleId);
        }
    }

    @AfterAll
    static void afterAll() throws SQLException {
        final Connection connection = EXASOL.createConnection();
        final Statement statement = connection.createStatement();
        statement.execute("DROP FUNCTION IF EXISTS " + schema.getName() + ".ROLE_NAME");
    }

    @Override
    protected Connection getConnection() throws NoDriverFoundException, SQLException {
        return EXASOL.createConnection("");
    }

    // [itest->dsn~get-a-role-mask~1]
    @ParameterizedTest
    @MethodSource("provideValuesForTestRolesMask")
    void testRolesMask(final String roles, final long maskValue) throws SQLException {
        final String sql = "SELECT SUM(" + schema.getName() + ".ROLE_MASK(ROLE_ID)) from "
                + table.getFullyQualifiedName() + " WHERE ROLE_NAME IN (" + roles + ")";
        assertThat(query(sql), table().row(maskValue).matches(NO_JAVA_TYPE_CHECK));
    }

    private static Stream<Arguments> provideValuesForTestRolesMask() {
        return Stream.of(Arguments.of("'role_1'", 1), //
                Arguments.of("'role_1', 'role_2'", 3), //
                Arguments.of("'role_1', 'role_4'", 9), //
                Arguments.of("'role_1', 'role_2', 'role_3', 'role_4'", 15), //
                Arguments.of("'role_3', 'role_31', 'role_63'", 4611686019501129732L));
    }
}