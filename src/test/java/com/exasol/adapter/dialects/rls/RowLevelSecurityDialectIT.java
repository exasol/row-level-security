package com.exasol.adapter.dialects.rls;

import static com.exasol.matcher.ResultSetMatcher.matchesResultSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

@Tag("integration")
@Testcontainers
class RowLevelSecurityDialectIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static final String ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION = "row-level-security-0.2.0-all-dependencies.jar";
    private static Statement statement;

    @BeforeAll
    static void beforeAll() throws SQLException, BucketAccessException, InterruptedException {
        final Bucket bucket = container.getDefaultBucket();
        final Path pathToRls = Path.of("target/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        bucket.uploadFile(pathToRls, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        TimeUnit.SECONDS.sleep(20);
        final Connection connection = container.createConnectionForUser(container.getUsername(),
                container.getPassword());
        statement = connection.createStatement();
        createTestSchema();
        createUnprotectedTableTestTable();
        createTenantsTestTable();
        createRolesTestTables();
        createRolesAndTenantsTestTable();
        createVirtualSchema();
        createUser("RLS_USR_1");
        createUser("RLS_USR_2");
        createUser("RLS_USR_3");
        createUser("RLS_USR_4");
    }

    private static void createVirtualSchema() throws SQLException, InterruptedException {
        statement.execute("CREATE CONNECTION JDBC_EXASOL_CONNECTION " //
                + "TO 'jdbc:exa:localhost:8888' " //
                + "USER '" + container.getUsername() + "' " //
                + "IDENTIFIED BY '" + container.getPassword() + "'");
        statement.execute("CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.ADAPTER_SCRIPT_EXASOL_RLS AS " //
                + "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n" //
                + "/");
        TimeUnit.SECONDS.sleep(20); // FIXME: need to be fixed in the container
        statement.execute("CREATE VIRTUAL SCHEMA virtual_schema_rls USING RLS_SCHEMA.ADAPTER_SCRIPT_EXASOL_RLS " //
                + "WITH " //
                + "SQL_DIALECT     = 'EXASOL_RLS' " //
                + "CONNECTION_NAME = 'JDBC_EXASOL_CONNECTION' " //
                + "SCHEMA_NAME     = 'RLS_SCHEMA'");
    }

    private static void createTestSchema() throws SQLException {
        statement.execute("CREATE SCHEMA RLS_SCHEMA");
    }

    private static void createUnprotectedTableTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.RLS_SALES_UNPROTECTED " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.RLS_SALES_UNPROTECTED VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
    }

    private static void createTenantsTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.RLS_SALES_TENANTS " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0), " //
                + "EXA_ROW_TENANT VARCHAR(128))");
        statement.execute("INSERT INTO RLS_SCHEMA.RLS_SALES_TENANTS VALUES " //
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
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.RLS_SALES_ROLES " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0), " //
                + "EXA_ROW_ROLES DECIMAL(20,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.RLS_SALES_ROLES VALUES " //
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
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.EXA_RLS_USERS " //
                + "(EXA_USER_NAME VARCHAR(200), " //
                + "EXA_ROLE_MASK DECIMAL(20,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.EXA_RLS_USERS VALUES " //
                + "('RLS_USR_1', NULL), " //
                + "('RLS_USR_2', 1), " //
                + "('RLS_USR_3', 3), " //
                + "('RLS_USR_4', 15), " //
                + "('SYS', 15)");
    }

    private static void createRolesAndTenantsTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.RLS_SALES_ROLES_AND_TENANTS " //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0), " //
                + "EXA_ROW_ROLES DECIMAL(20,0), " //
                + "EXA_ROW_TENANT VARCHAR(128))");
        statement.execute("INSERT INTO RLS_SCHEMA.RLS_SALES_ROLES_AND_TENANTS VALUES " //
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

    private static void createUser(final String userName) throws SQLException {
        statement.execute("CREATE USER " + userName + " IDENTIFIED BY \"" + userName + "\"");
        statement.execute("GRANT ALL PRIVILEGES TO " + userName + "");
    }

    @Test
    void testSelectFromExaRlsUsersThrowsException() {
        assertThrows(SQLException.class, () -> statement.execute("SELECT * FROM EXA_RLS_USERS"));
    }

    @Test
    void testUnprotectedTableWithAdmin() throws SQLException {
        createExpectedTable("EXPECTED_UNPROTECTED_ADMIN");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_UNPROTECTED_ADMIN VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        assertThat(statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_UNPROTECTED"),
                matchesResultSet(statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_UNPROTECTED_ADMIN")));
    }

    private void createExpectedTable(final String tableName) throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA." + tableName //
                + "(ORDER_ID DECIMAL(18,0), " //
                + "CUSTOMER VARCHAR(50), " //
                + "PRODUCT VARCHAR(100), " //
                + "QUANTITY DECIMAL(18,0))");
    }

    @Test
    void testUnprotectedTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        createExpectedTable("EXPECTED_UNPROTECTED_USER_1");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_UNPROTECTED_USER_1 VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_UNPROTECTED_USER_1");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_UNPROTECTED");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testUnprotectedTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_UNPROTECTED_USER_2");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_UNPROTECTED_USER_2 VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_UNPROTECTED_USER_2");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_UNPROTECTED");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testTenantsTableWithAdmin() throws SQLException {
        createExpectedTable("EXPECTED_TENANTS_ADMIN");
        assertThat(statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS"),
                matchesResultSet(statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_TENANTS_ADMIN")));

    }

    @Test
    void testTenantsTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        createExpectedTable("EXPECTED_TENANTS_USER_1");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_TENANTS_USER_1 VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(10, 'Donkey Inc', 'Carrot', 2)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_TENANTS_USER_1");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testTenantsTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_TENANTS_USER_2");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_TENANTS_USER_2 VALUES " //
                + "(4, 'Chicken Inc', 'Wheat', 4), " //
                + "(9, 'Chicken Inc', 'Wheat', 64)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_TENANTS_USER_2");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testTenantsTableWithUser3() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        createExpectedTable("EXPECTED_TENANTS_USER_3");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_TENANTS_USER_3 VALUES " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(8, 'Chicken Inc', 'Wheat', 44)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_TENANTS_USER_3");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testTenantsTableWithUser4() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        createExpectedTable("EXPECTED_TENANTS_USER_4");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_TENANTS_USER_4 VALUES " //
                + "(6, 'Donkey Inc', 'Carrot', 67), " //
                + "(7, 'Goat Inc', 'Grass', 84)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_TENANTS_USER_4");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testTenantsTableWithUser1SelectAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        final ResultSet resultSet = statement.executeQuery("SELECT ORDER_ID FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(1)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getInt("order_id"), equalTo(3)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getInt("order_id"), equalTo(10)),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @Test
    void testTenantsTableWithUser2SelectAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        final ResultSet resultSet = statement
                .executeQuery("SELECT ORDER_ID, PRODUCT FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS WHERE ORDER_ID = 4");
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(2)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getString("product"), equalTo("Wheat")),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @Test
    void testTenantsTableWithUser3SelectNotAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        assertThrows(SQLException.class,
                () -> statement.execute("SELECT EXA_ROW_TENANT FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS"));
        statement.execute("IMPERSONATE SYS");
    }

    @Test
    void testTenantsTableWithUser4WhereNotAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        assertThrows(SQLException.class, () -> statement
                .execute("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_TENANTS WHERE EXA_ROW_TENANT = 'RLS_USR_2'"));
        statement.execute("IMPERSONATE SYS");
    }

    @Test
    void testRolesTableWithAdmin() throws SQLException {
        createExpectedTable("EXPECTED_ROLES_ADMIN");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_ADMIN VALUES " //
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
        assertThat(statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES"),
                matchesResultSet(statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_ADMIN")));

    }

    @Test
    void testRolesTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        createExpectedTable("EXPECTED_ROLES_USER_1");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_USER_1 VALUES " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_USER_1");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_ROLES_USER_2");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_USER_2 VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(15, 'Chicken Inc', 'Wheat', 3), " //
                + "(17, 'Donkey Inc', 'Carrot', 58), " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_USER_2");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesTableWithUser3() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        createExpectedTable("EXPECTED_ROLES_USER_3");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_USER_3 VALUES " //
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
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_USER_3");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesTableWithUser4() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        createExpectedTable("EXPECTED_ROLES_USER_4");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_USER_4 VALUES " //
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
        final ResultSet expectedResultSet = statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_USER_4");
        final ResultSet actualResultSet = statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesTableWithUser1SelectAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_1");
        final ResultSet resultSet = statement.executeQuery("SELECT ORDER_ID FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES");
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(1)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getInt("order_id"), equalTo(18)),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @Test
    void testRolesTableWithUser2SelectAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        final ResultSet resultSet = statement
                .executeQuery("SELECT ORDER_ID, PRODUCT FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES WHERE ORDER_ID > 15");
        statement.execute("IMPERSONATE SYS");
        assertAll(() -> assertThat(resultSet.getMetaData().getColumnCount(), equalTo(2)),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getString("product"), equalTo("Carrot")),
                () -> assertThat(resultSet.next(), equalTo(true)),
                () -> assertThat(resultSet.getString("product"), equalTo("Wheat")),
                () -> assertThat(resultSet.next(), equalTo(false)));
    }

    @Test
    void testRolesTableWithUser3SelectNotAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        assertThrows(SQLException.class,
                () -> statement.execute("SELECT RLS_SALES_ROLES FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES"));
        statement.execute("IMPERSONATE SYS");
    }

    @Test
    void testRolesTableWithUser4WhereNotAllowedColumns() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        assertThrows(SQLException.class,
                () -> statement.execute("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES WHERE RLS_SALES_ROLES = 5"));
        statement.execute("IMPERSONATE SYS");
    }

    @Test
    void testRolesAndTenantsTableWithAdmin() throws SQLException {
        createExpectedTable("EXPECTED_ROLES_TENANTS_ADMIN");
        assertThat(statement.executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES_AND_TENANTS"),
                matchesResultSet(statement.executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_TENANTS_ADMIN")));

    }

    @Test
    void testRolesAndTenantsTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_1");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_1 VALUES " //
                + "(19, 'Donkey Inc', 'Wheat', 50)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_1");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesAndTenantsTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_2");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_2");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_2 VALUES " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_2");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesAndTenantsTableWithUser3() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_3");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_3");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_3 VALUES " //
                + "(8, 'Chicken Inc', 'Wheat', 44), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_3");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }

    @Test
    void testRolesAndTenantsTableWithUser4() throws SQLException {
        statement.execute("IMPERSONATE RLS_USR_4");
        createExpectedTable("EXPECTED_ROLES_TENANTS_USER_4");
        statement.execute("INSERT INTO RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_4 VALUES " //
                + "(12, 'Chicken Inc', 'Wheat', 44), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(14, 'Donkey Inc', 'Carrot', 89), " //
                + "(15, 'Chicken Inc', 'Wheat', 3)");
        final ResultSet expectedResultSet = statement
                .executeQuery("SELECT * FROM RLS_SCHEMA.EXPECTED_ROLES_TENANTS_USER_4");
        final ResultSet actualResultSet = statement
                .executeQuery("SELECT * FROM VIRTUAL_SCHEMA_RLS.RLS_SALES_ROLES_AND_TENANTS");
        statement.execute("IMPERSONATE SYS");
        assertThat(actualResultSet, matchesResultSet(expectedResultSet));
    }
}