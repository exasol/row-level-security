package com.exasol.rls.administration.scripts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

@Tag("integration")
@Testcontainers
public class RolesMaskIT {
    private static final Path PATH_TO_ROLES_MASK = Path.of("src/main/sql/roles_mask.sql");
    private static final String RLS_SCHEMA_NAME = "RLS_SCHEMA";
    private static final String EXA_ROLES_MAPPING = "EXA_ROLES_MAPPING";
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static Statement statement;

    @BeforeAll
    static void beforeAll() throws SQLException, IOException {
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        ScriptsSqlManager.createTestSchema(statement, RLS_SCHEMA_NAME);
        ScriptsSqlManager.createScript(statement, PATH_TO_ROLES_MASK);
        ScriptsSqlManager.createExaRolesMappingProjection(statement, EXA_ROLES_MAPPING,
                "('Sales', 1), ('Development', 2), ('Finance', 3),  ('Support', 4)");
    }

    @ParameterizedTest
    @MethodSource("provideValuesForTestRolesMask")
    void testRolesMask(final String roles, final int maskValue) throws SQLException {
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT ROLES_MASK(ROLE_ID) from EXA_ROLES_MAPPING WHERE ROLE_NAME IN (" + roles + ")");
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
