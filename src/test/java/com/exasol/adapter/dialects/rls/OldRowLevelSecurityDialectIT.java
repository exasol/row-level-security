package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_RLS_USERS_TABLE_NAME;
import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.EXA_ROLES_MAPPING_TABLE_NAME;
import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static com.exasol.tools.TestsConstants.RLS_SCHEMA_NAME;
import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;
import com.exasol.tools.SqlTestSetupManager;

@Tag("integration")
@Testcontainers
class OldRowLevelSecurityDialectIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldRowLevelSecurityDialectIT.class);
    private static final String VIRTUAL_SCHEMA_RLS_JDBC_NAME = "VIRTUAL_SCHEMA_RLS_JDBC";
    private static final String VIRTUAL_SCHEMA_RLS_JDBC_LOCAL_NAME = "VIRTUAL_SCHEMA_RLS_JDBC_LOCAL";
    private static final String VIRTUAL_SCHEMA_RLS_EXA_NAME = "VIRTUAL_SCHEMA_RLS_EXA";
    private static final String VIRTUAL_SCHEMA_RLS_EXA_LOCAL_NAME = "VIRTUAL_SCHEMA_RLS_EXA_LOCAL";
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE) //
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER));
    public static final String RLS_SALES_UNPROTECTED = "RLS_SALES_UNPROTECTED";
    public static final String RLS_SALES_TENANTS = "RLS_SALES_TENANTS";
    public static final String RLS_SALES_ROLES = "RLS_SALES_ROLES";
    private static Statement statement;
    private static SqlTestSetupManager sqlTestSetupManager;

    @BeforeAll
    static void beforeAll() throws SQLException, BucketAccessException, InterruptedException, TimeoutException {
        final Bucket bucket = container.getDefaultBucket();
        final Path pathToRls = Path.of("target/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        bucket.uploadFile(pathToRls, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        sqlTestSetupManager = new SqlTestSetupManager(statement);
        sqlTestSetupManager.createTestSchema(RLS_SCHEMA_NAME);
        createUnprotectedTableTestTable();
        createTenantsTestTable();
        createRolesTestTables();
        createRolesAndTenantsTestTable();
        createConnection();
        createAdapterScript();
        createVirtualSchema(VIRTUAL_SCHEMA_RLS_JDBC_NAME, Optional.empty());
        createVirtualSchema(VIRTUAL_SCHEMA_RLS_JDBC_LOCAL_NAME, Optional.of("IS_LOCAL = 'true'"));
        createVirtualSchema(VIRTUAL_SCHEMA_RLS_EXA_NAME,
                Optional.of("IMPORT_FROM_EXA = 'true' EXA_CONNECTION_STRING = 'localhost:8888'"));
        createVirtualSchema(VIRTUAL_SCHEMA_RLS_EXA_LOCAL_NAME,
                Optional.of("IMPORT_FROM_EXA = 'true' EXA_CONNECTION_STRING = 'localhost:8888' IS_LOCAL = 'true'"));
        createUsers(List.of("RLS_USR_1", "RLS_USR_2", "RLS_USR_3", "RLS_USR_4"));
    }

    private static void createUnprotectedTableTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + RLS_SCHEMA_NAME + "." + RLS_SALES_UNPROTECTED //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0))");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + "." + RLS_SALES_UNPROTECTED + " VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
    }

    private static void createTenantsTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + RLS_SCHEMA_NAME + "." + RLS_SALES_TENANTS + " " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0), " //
                + "EXA_ROW_TENANT VARCHAR(128))");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + "." + RLS_SALES_TENANTS + " VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100, NULL), " //
                + "(2, 'Goat Inc', 'Carrot', 10, 'NOBODY'), " //
                + "(3, 'Donkey Inc', 'Carrot', 33, 'RLS_USR_1'), " //
                + "(4, 'Chicken Inc', 'Wheat', 4, 'RLS_USR_2'), " //
                + "(5, 'Chicken Inc', 'Wheat', 45, 'RLS_USR_3'), " //
                + "(6, 'Donkey Inc', 'Carrot', 67, 'RLS_USR_4'), " //
                + "(7, 'Goat Inc', 'Grass', 84, 'RLS_USR_4'), " //
                + "(8, 'Chicken Inc', 'Wheat', 44, 'RLS_USR_3'), " //
                + "(9, 'Chicken Inc', 'Wheat', 64, 'RLS_USR_2'), " //
                + "(10, 'Donkey Inc', 'Carrot', 2, 'RLS_USR_1')");
    }

    private static void createRolesTestTables() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + RLS_SCHEMA_NAME + "." + RLS_SALES_ROLES + " " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0), " //
                + "EXA_ROW_ROLES DECIMAL(20,0))");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + "." + RLS_SALES_ROLES + " VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100, NULL), " //
                + "(2, 'Goat Inc', 'Grass', 2, 0), " //
                + "(3, 'Donkey Inc', 'Carrot', 33, 1), " //
                + "(4, 'Chicken Inc', 'Wheat', 4, 2), " //
                + "(5, 'Chicken Inc', 'Wheat', 45, 3), " //
                + "(6, 'Donkey Inc', 'Carrot', 67, 4), " + "(7, 'Goat Inc', 'Grass', 84, 5), " //
                + "(8, 'Chicken Inc', 'Wheat', 44, 6), " //
                + "(9, 'Chicken Inc', 'Wheat', 64, 7), " //
                + "(10, 'Donkey Inc', 'Carrot', 2, 8), " //
                + "(11, 'Goat Inc', 'Grass', 54, 9), " //
                + "(12, 'Chicken Inc', 'Wheat', 44, 10), " //
                + "(13, 'Chicken Inc', 'Wheat', 65, 11), " //
                + "(14, 'Donkey Inc', 'Carrot', 89, 12), " //
                + "(15, 'Chicken Inc', 'Wheat', 3, 13), " //
                + "(16, 'Goat Inc', 'Grass', 34, 14), " //
                + "(17, 'Donkey Inc', 'Carrot', 58, 15), " //
                + "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
        sqlTestSetupManager.createExaRlsUsersProjection(EXA_RLS_USERS_TABLE_NAME, //
                "('RLS_USR_1', NULL), " //
                        + "('RLS_USR_2', 1), " //
                        + "('RLS_USR_3', 3), " //
                        + "('RLS_USR_4', 15), " //
                        + "('SYS', 15)");
    }

    private static void createRolesAndTenantsTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + RLS_SCHEMA_NAME + "." + RLS_SALES_ROLES + "_AND_TENANTS " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0), " //
                + "EXA_ROW_ROLES DECIMAL(20,0), " //
                + "EXA_ROW_TENANT VARCHAR(128))");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + "." + RLS_SALES_ROLES + "_AND_TENANTS VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100, NULL, 'RLS_USR_1'), " //
                + "(2, 'Goat Inc', 'Grass', 2, 0, 'RLS_USR_1'), " //
                + "(3, 'Donkey Inc', 'Carrot', 33, 1, 'RLS_USR_1'), " //
                + "(4, 'Chicken Inc', 'Wheat', 4, 2, 'RLS_USR_1'), " //
                + "(5, 'Chicken Inc', 'Wheat', 45, 3, 'RLS_USR_2'), " //
                + "(6, 'Donkey Inc', 'Carrot', 67, 4, 'RLS_USR_2'), " //
                + "(7, 'Goat Inc', 'Grass', 84, 5, 'RLS_USR_2'), " //
                + "(8, 'Chicken Inc', 'Wheat', 44, 6, 'RLS_USR_3'), " //
                + "(9, 'Chicken Inc', 'Wheat', 64, 7, 'RLS_USR_3'), " //
                + "(10, 'Donkey Inc', 'Carrot', 2, 8, 'RLS_USR_3'), " //
                + "(11, 'Goat Inc', 'Grass', 54, 9, 'RLS_USR_3'), " //
                + "(12, 'Chicken Inc', 'Wheat', 44, 10, 'RLS_USR_4'), " //
                + "(13, 'Chicken Inc', 'Wheat', 65, 11, 'RLS_USR_4'), " //
                + "(14, 'Donkey Inc', 'Carrot', 89, 12, 'RLS_USR_4'), " //
                + "(15, 'Chicken Inc', 'Wheat', 3, 13, 'RLS_USR_4'), " //
                + "(16, 'Goat Inc', 'Grass', 34, 14, 'NOBODY'), " //
                + "(17, 'Donkey Inc', 'Carrot', 58, 15, NULL), " //
                + "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808, 'NOBODY'), " //
                + "(19, 'Donkey Inc', 'Wheat', 50, 9223372036854775808, 'RLS_USR_1')");
    }

    private static void createConnection() throws SQLException {
        statement.execute("CREATE CONNECTION JDBC_EXASOL_CONNECTION " //
                + "TO 'jdbc:exa:localhost:8888' " //
                + "USER '" + container.getUsername() + "' " //
                + "IDENTIFIED BY '" + container.getPassword() + "'");
    }

    private static void createAdapterScript() throws SQLException {
        statement.execute("CREATE OR REPLACE JAVA ADAPTER SCRIPT " + RLS_SCHEMA_NAME + ".ADAPTER_SCRIPT_EXASOL_RLS AS " //
                + "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n" //
                + "/");
    }

    private static void createVirtualSchema(final String virtualSchemaName, final Optional<String> additionalParameters)
            throws SQLException {
        final StringBuilder builder = new StringBuilder();
        builder.append("CREATE VIRTUAL SCHEMA ");
        builder.append(virtualSchemaName);
        builder.append(" USING " + RLS_SCHEMA_NAME + ".ADAPTER_SCRIPT_EXASOL_RLS WITH ");
        builder.append("SQL_DIALECT     = 'EXASOL_RLS' ");
        builder.append("CONNECTION_NAME = 'JDBC_EXASOL_CONNECTION' ");
        builder.append("SCHEMA_NAME     = '" + RLS_SCHEMA_NAME + "' ");
        additionalParameters.ifPresent(builder::append);
        statement.execute(builder.toString());
    }

    private static void createUsers(final List<String> userNames) throws SQLException {
        for (final String userName : userNames) {
            statement.execute("CREATE USER " + userName + " IDENTIFIED BY \"" + userName + "\"");
            statement.execute("GRANT ALL PRIVILEGES TO " + userName + "");
        }
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testExaRlsUsersTableIsFilteredOut(final String virtualSchemaName) {
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("SELECT * FROM " + virtualSchemaName + "." + EXA_RLS_USERS_TABLE_NAME));
        assertThat(thrown.getMessage(),
                containsString("object " + virtualSchemaName + "." + EXA_RLS_USERS_TABLE_NAME + " not found"));
    }

    private static Stream<String> getVirtualSchemaVariants() {
        return Stream.of(VIRTUAL_SCHEMA_RLS_JDBC_NAME, VIRTUAL_SCHEMA_RLS_JDBC_LOCAL_NAME, VIRTUAL_SCHEMA_RLS_EXA_NAME,
                VIRTUAL_SCHEMA_RLS_EXA_LOCAL_NAME);
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testSelectFromExaRolesMappingTableIsFilteredOut(final String virtualSchemaName) throws SQLException {
        sqlTestSetupManager.createExaRolesMappingProjection(RLS_SCHEMA_NAME + "." + EXA_ROLES_MAPPING_TABLE_NAME,
                "('Sales', 1)");
        final SQLException thrown = assertThrows(SQLException.class,
                () -> statement.execute("SELECT * FROM " + virtualSchemaName + "." + EXA_ROLES_MAPPING_TABLE_NAME));
        assertThat(thrown.getMessage(),
                containsString("object " + virtualSchemaName + "." + EXA_ROLES_MAPPING_TABLE_NAME + " not found"));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testUnprotectedTableWithAdmin(final String virtualSchemaName) throws SQLException {
        createExpectedTable("EXPECTED_UNPROTECTED_ADMIN");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_UNPROTECTED_ADMIN VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        assertThat(statement.executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_UNPROTECTED),
                matchesResultSet(
                        statement.executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_UNPROTECTED_ADMIN")));
    }

    private void createExpectedTable(final String tableName) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE " + RLS_SCHEMA_NAME + "." + tableName //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0))");
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testUnprotectedTableWithUser1(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        createExpectedTable("EXPECTED_UNPROTECTED_USER_1");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_UNPROTECTED_USER_1 VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_UNPROTECTED_USER_1");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_UNPROTECTED);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testUnprotectedTableWithUser2(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_UNPROTECTED_USER_2");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_UNPROTECTED_USER_2 VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_UNPROTECTED_USER_2");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_UNPROTECTED);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithAdmin(final String virtualSchemaName) throws SQLException {
        createExpectedTable("EXPECTED_TENANTS_ADMIN");
        assertThat(statement.executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS),
                matchesResultSet(
                        statement.executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_ADMIN")));

    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser1(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        createExpectedTable("EXPECTED_TENANTS_USER_1");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_1 VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(10, 'Donkey Inc', 'Carrot', 2)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_1");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser2(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_TENANTS_USER_2");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_2 VALUES " //
                + "(4, 'Chicken Inc', 'Wheat', 4), " //
                + "(9, 'Chicken Inc', 'Wheat', 64)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_2");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser3(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        createExpectedTable("EXPECTED_TENANTS_USER_3");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_3 VALUES " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(8, 'Chicken Inc', 'Wheat', 44)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_3");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser4(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        createExpectedTable("EXPECTED_TENANTS_USER_4");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_4 VALUES " //
                + "(6, 'Donkey Inc', 'Carrot', 67), " //
                + "(7, 'Goat Inc', 'Grass', 84)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_TENANTS_USER_4");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser1SelectAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        final ResultSet resultSet = statement
                .executeQuery("SELECT ORDER_ID FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS);
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(1)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getInt("order_id"), equalTo(3)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getInt("order_id"), equalTo(10)),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser2SelectAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        final ResultSet resultSet = statement.executeQuery(
                "SELECT ORDER_ID, PRODUCT FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS + " WHERE ORDER_ID = 4");
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(2)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getString("product"), equalTo("Wheat")),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser3SelectNotAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        assertThrows(SQLException.class,
                () -> statement.execute("SELECT EXA_ROW_TENANT FROM " + virtualSchemaName + "." + RLS_SALES_TENANTS));
        statement.execute("IMPERSONATE SYS");
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testTenantsTableWithUser4WhereNotAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        assertThrows(SQLException.class, () -> statement.execute("SELECT * FROM " + virtualSchemaName + "."
                + RLS_SALES_TENANTS + " WHERE EXA_ROW_TENANT = 'RLS_USR_2'"));
        statement.execute("IMPERSONATE SYS");
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithAdmin(final String virtualSchemaName) throws SQLException {
        createExpectedTable("EXPECTED_ROLES_ADMIN");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_ADMIN VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4), " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(6, 'Donkey Inc', 'Carrot', 67), " //
                + "(7, 'Goat Inc', 'Grass', 84), " //
                + "(8, 'Chicken Inc', 'Wheat', 44), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(10, 'Donkey Inc', 'Carrot', 2), " //
                + "(11, 'Goat Inc', 'Grass', 54), " //
                + "(12, 'Chicken Inc', 'Wheat', 44), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(14, 'Donkey Inc', 'Carrot', 89), " //
                + "(15, 'Chicken Inc', 'Wheat', 3), " //
                + "(16, 'Goat Inc', 'Grass', 34), " //
                + "(17, 'Donkey Inc', 'Carrot', 58), " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        assertThat(statement.executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES),
                matchesResultSet(statement.executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_ADMIN")));

    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser1(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        createExpectedTable("EXPECTED_ROLES_USER_1");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_1 VALUES " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_1");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser2(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_ROLES_USER_2");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_2 VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(15, 'Chicken Inc', 'Wheat', 3), " //
                + "(17, 'Donkey Inc', 'Carrot', 58), " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_2");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser3(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        createExpectedTable("EXPECTED_ROLES_USER_3");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_3 VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4), " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84), " //
                + "(8, 'Chicken Inc', 'Wheat', 44), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54), " //
                + "(12, 'Chicken Inc', 'Wheat', 44), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(15, 'Chicken Inc', 'Wheat', 3), " //
                + "(16, 'Goat Inc', 'Grass', 34), " //
                + "(17, 'Donkey Inc', 'Carrot', 58), " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_3");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser4(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        createExpectedTable("EXPECTED_ROLES_USER_4");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_4 VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4), " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(6, 'Donkey Inc', 'Carrot', 67), " //
                + "(7, 'Goat Inc', 'Grass', 84), " //
                + "(8, 'Chicken Inc', 'Wheat', 44), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(10, 'Donkey Inc', 'Carrot', 2), " //
                + "(11, 'Goat Inc', 'Grass', 54), " //
                + "(12, 'Chicken Inc', 'Wheat', 44), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(14, 'Donkey Inc', 'Carrot', 89), " //
                + "(15, 'Chicken Inc', 'Wheat', 3), " //
                + "(16, 'Goat Inc', 'Grass', 34), " //
                + "(17, 'Donkey Inc', 'Carrot', 58), " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_USER_4");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES);
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser1SelectAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        final ResultSet resultSet = statement
                .executeQuery("SELECT ORDER_ID FROM " + virtualSchemaName + "." + RLS_SALES_ROLES);
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(1)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getInt("order_id"), equalTo(18)),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser2SelectAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        final ResultSet resultSet = statement.executeQuery(
                "SELECT ORDER_ID, PRODUCT FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + " WHERE ORDER_ID > 15");
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(2)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getString("product"), equalTo("Carrot")),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getString("product"), equalTo("Wheat")),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser3SelectNotAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        assertThrows(SQLException.class, () -> statement
                .execute("SELECT " + RLS_SALES_ROLES + " FROM " + virtualSchemaName + "." + RLS_SALES_ROLES));
        statement.execute("IMPERSONATE SYS");
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesTableWithUser4WhereNotAllowedColumns(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        assertThrows(SQLException.class, () -> statement
                .execute("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + " WHERE RLS_SALES_ROLES = 5"));
        statement.execute("IMPERSONATE SYS");
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesAndTenantsTableWithAdmin(final String virtualSchemaName) throws SQLException {
        createExpectedTable("EXPECTED_ROLES_TENANTS_ADMIN");
        assertThat(
                statement.executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + "_AND_TENANTS"),
                matchesResultSet(
                        statement.executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_ADMIN")));

    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesAndTenantsTableWithUser1(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_1");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_1 VALUES " //
                + "(19, 'Donkey Inc', 'Wheat', 50)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_1");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + "_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesAndTenantsTableWithUser2(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_2");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_2 VALUES " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_2");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + "_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesAndTenantsTableWithUser3(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_3");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_3 VALUES " //
                + "(8, 'Chicken Inc', 'Wheat', 44), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_3");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + "_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @ParameterizedTest
    @MethodSource("getVirtualSchemaVariants")
    void testRolesAndTenantsTableWithUser4(final String virtualSchemaName) throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_4");
        statement.execute("INSERT INTO " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_4 VALUES " //
                + "(12, 'Chicken Inc', 'Wheat', 44), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(14, 'Donkey Inc', 'Carrot', 89), " //
                + "(15, 'Chicken Inc', 'Wheat', 3)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM " + RLS_SCHEMA_NAME + ".EXPECTED_ROLES_TENANTS_USER_4");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM " + virtualSchemaName + "." + RLS_SALES_ROLES + "_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }
}