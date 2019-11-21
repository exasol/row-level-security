package com.exasol.adapter.dialects.rls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

@Testcontainers
class RowLevelSecurityDialectIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static final String ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION = "row-level-security-0.1.0-all-dependencies.jar";
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
        // Clean-up before tests
        statement.execute("DROP FORCE VIRTUAL SCHEMA IF EXISTS VIRTUAL_SCHEMA_RLS CASCADE");
        statement.execute("DROP SCHEMA IF EXISTS RLS_SCHEMA CASCADE");
        statement.execute("DROP CONNECTION IF EXISTS jdbc_exasol_connection");
        // Create test schema
        statement.execute("CREATE SCHEMA RLS_SCHEMA");
        // Create test tables
        createUnprotectedTableTestTable();
        createTenantsTestTable();
        createRolesTestTables();
        createRolesAndTenantsTestTable();
        // Create Virtual Schema
        statement.execute("CREATE CONNECTION jdbc_exasol_connection " //
                + "TO 'jdbc:exa:localhost:8888' " //
                + "USER '" + container.getUsername().toLowerCase() + "' " //
                + "IDENTIFIED BY '" + container.getPassword() + "'");
        statement.execute("CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.adapter_script_exasol_rls AS " //
                + "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n" //
                + "/");
        TimeUnit.SECONDS.sleep(20); // FIXME: need to be fixed in the container
        statement.execute("CREATE VIRTUAL SCHEMA virtual_schema_rls USING RLS_SCHEMA.adapter_script_exasol_rls " //
                + "WITH " //
                + "SQL_DIALECT     = 'EXASOL_RLS' " //
                + "CONNECTION_NAME = 'jdbc_exasol_connection' " //
                + "SCHEMA_NAME     = 'RLS_SCHEMA'");
        // Create a script for comparing tables
        createComparingScript();
        // Create users
        createUser("RLS_USR_1");
        createUser("RLS_USR_2");
        createUser("RLS_USR_3");
        createUser("RLS_USR_4");
    }

    private static void createUnprotectedTableTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_unprotected " //
                + "(order_id DECIMAL(18,0), " //
                + "customer VARCHAR(50), " //
                + "product VARCHAR(100), " //
                + "quantity DECIMAL(18,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_unprotected VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
    }

    private static void createTenantsTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_tenants " //
                + "(order_id DECIMAL(18,0), " //
                + "customer VARCHAR(50), " //
                + "product VARCHAR(100), " //
                + "quantity DECIMAL(18,0), " //
                + "exa_row_tenants VARCHAR(128))");
        statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_tenants VALUES " //
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
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_roles " //
                + "(order_id DECIMAL(18,0), " //
                + "customer VARCHAR(50), " //
                + "product VARCHAR(100), " //
                + "quantity DECIMAL(18,0), " //
                + "exa_row_roles DECIMAL(20,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_roles VALUES " //
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
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.exa_rls_users " //
                + "(exa_user_name VARCHAR(200), " //
                + "exa_role_mask DECIMAL(20,0))");
        statement.execute("INSERT INTO RLS_SCHEMA.exa_rls_users VALUES " //
                + "('RLS_USR_1', NULL), " //
                + "('RLS_USR_2', 1), " //
                + "('RLS_USR_3', 3), " //
                + "('RLS_USR_4', 15), " //
                + "('SYS', 15)");
    }

    private static void createRolesAndTenantsTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_roles_and_tenants " //
                + "(order_id DECIMAL(18,0), " //
                + "customer VARCHAR(50), " //
                + "product VARCHAR(100), " //
                + "quantity DECIMAL(18,0), " //
                + "exa_row_roles DECIMAL(20,0), " //
                + "exa_row_tenants VARCHAR(128))");
        statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_roles_and_tenants VALUES " //
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

    private static void createComparingScript() throws SQLException {
        statement.execute(
                "CREATE OR REPLACE SCRIPT RLS_SCHEMA.compare_table_contents (table_a, table_b) " + "RETURNS TABLE AS " //
                        + "    exit( " //
                        + "       query([[ " //
                        + "           (SELECT '<<<', A.* " //
                        + "           FROM " //
                        + "                (SELECT * FROM ::a " //
                        + "                EXCEPT " //
                        + "                SELECT * FROM ::b " //
                        + "                ) A " //
                        + "            ) " //
                        + "            UNION ALL " //
                        + "            (SELECT '>>>', A.* " //
                        + "            FROM " //
                        + "                (SELECT * FROM ::b " //
                        + "                EXCEPT " //
                        + "                SELECT * FROM ::a " //
                        + "                ) A " //
                        + "            ) " //
                        + "        ]], {a = table_a, b = table_b} " //
                        + "        ) " //
                        + "    )");
    }

    private static void createUser(final String userName) throws SQLException {
        statement.execute("DROP USER IF EXISTS " + userName + " CASCADE");
        statement.execute("CREATE USER " + userName + " IDENTIFIED BY \"" + userName + "\"");
        statement.execute("GRANT ALL PRIVILEGES TO " + userName + "");
    }

    @Test
    void testUnprotectedTableWithAdmin() throws SQLException {
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_unprotected', 'RLS_SCHEMA.rls_sales_user_admin')");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testUnprotectedTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_unprotected', 'RLS_SCHEMA.rls_sales_user_1')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testUnprotectedTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_2");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2"
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(1, 'Chicken Inc', 'Wheat', 100), " //
                + "(2, 'Goat Inc', 'Carrot', 10), " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(4, 'Chicken Inc', 'Wheat', 4)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_unprotected', 'RLS_SCHEMA.rls_sales_user_2')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testTenantsTableWithAdmin() throws SQLException {
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin" //
                + "(order_id, customer, product, quantity) AS " //
                + "SELECT * FROM virtual_schema_rls.rls_sales_tenants WHERE 1=0");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents" //
                + "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_admin')");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testTenantsTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES" //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(10, 'Donkey Inc', 'Carrot', 2)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_1')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testTenantsTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_2");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES" //
                + "(4, 'Chicken Inc', 'Wheat', 4), " //
                + "(9, 'Chicken Inc', 'Wheat', 64)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_2')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testTenantsTableWithUser3() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_3");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_3" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(8, 'Chicken Inc', 'Wheat', 44)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_3')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testTenantsTableWithUser4() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_4");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_4" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(6, 'Donkey Inc', 'Carrot', 67), " //
                + "(7, 'Goat Inc', 'Grass', 84)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_4')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesTableWithAdmin() throws SQLException {
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
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
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_admin')");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_1')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_2");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2"
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(3, 'Donkey Inc', 'Carrot', 33), " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(15, 'Chicken Inc', 'Wheat', 3), " //
                + "(17, 'Donkey Inc', 'Carrot', 58), " //
                + "(18, 'Donkey Inc', 'Wheat', 56)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_2')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesTableWithUser3() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_3");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_3" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
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
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_3')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesTableWithUser4() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_4");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_4" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
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
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_4')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesAndTenantsTableWithAdmin() throws SQLException {
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin" //
                + "(order_id, customer, product, quantity) AS " //
                + "SELECT * FROM virtual_schema_rls.rls_sales_roles_and_tenants WHERE 1=0");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_admin')");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesAndTenantsTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(19, 'Donkey Inc', 'Wheat', 50)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_1')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesAndTenantsTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_2");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(5, 'Chicken Inc', 'Wheat', 45), " //
                + "(7, 'Goat Inc', 'Grass', 84)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_2')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesAndTenantsTableWithUser3() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_3");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_3" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(8, 'Chicken Inc', 'Wheat', 44), " //
                + "(9, 'Chicken Inc', 'Wheat', 64), " //
                + "(11, 'Goat Inc', 'Grass', 54)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_3')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testRolesAndTenantsTableWithUser4() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_4");
        statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_4" //
                + "(order_id, customer, product, quantity) AS SELECT * FROM VALUES " //
                + "(12, 'Chicken Inc', 'Wheat', 44), " //
                + "(13, 'Chicken Inc', 'Wheat', 65), " //
                + "(14, 'Donkey Inc', 'Carrot', 89), " //
                + "(15, 'Chicken Inc', 'Wheat', 3)");
        final ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
                + "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_4')");
        statement.execute("IMPERSONATE SYS");
        assertThat(resultSet.next(), equalTo(false));
    }
}