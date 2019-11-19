package com.exasol.adapter.dialects.rls;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Testcontainers
class RowLevelSecurityQueryRewriterIT {
    @Container
    private static ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
    private static Statement statement;

    @BeforeAll
    static void beforeAll() throws SQLException, BucketAccessException, InterruptedException {
        Bucket bucket = container.getDefaultBucket();
        Path pathToDriver = Path.of("src/test/resources/exajdbc.jar");
        bucket.uploadFile(pathToDriver, "exasol-jdbc.jar");
        TimeUnit.SECONDS.sleep(10);
        Path pathToRls = Path.of("target/row-level-security-0.1.0-all-dependencies.jar");
        bucket.uploadFile(pathToRls, "row-level-security-0.1.0-all-dependencies.jar");
        Connection connection = container.createConnectionForUser(container.getUsername(), container.getPassword());
        statement = connection.createStatement();
        //Clean-up before tests
        statement.execute("DROP FORCE VIRTUAL SCHEMA IF EXISTS VIRTUAL_SCHEMA_RLS CASCADE");
        statement.execute("DROP SCHEMA IF EXISTS row_level_security_test_schema CASCADE");
        statement.execute("DROP CONNECTION IF EXISTS jdbc_exasol_connection");
        //Create test schema
        statement.execute("CREATE SCHEMA row_level_security_test_schema");
        //Create test table
        createUnprotectedTableTestTable();
        //Create Virtual Schema
        statement.execute("CREATE CONNECTION jdbc_exasol_connection " +
                "TO 'jdbc:exa:localhost:8888' " +
                "USER '" + container.getUsername().toLowerCase() + "' " +
                "IDENTIFIED BY '" + container.getPassword() + "'");
        statement.execute("CREATE OR REPLACE JAVA ADAPTER SCRIPT row_level_security_test_schema.adapter_script_exasol_rls AS\n" +
                "    %scriptclass com.exasol.adapter.RequestDispatcher;\n" +
                "    %jar /buckets/bfsdefault/default/row-level-security-0.1.0-all-dependencies.jar;\n" +
                "    %jar /buckets/bfsdefault/default/exasol-jdbc.jar;\n" +
                "/");
        TimeUnit.SECONDS.sleep(20);
        statement.execute("CREATE VIRTUAL SCHEMA virtual_schema_rls USING row_level_security_test_schema.adapter_script_exasol_rls WITH\n" +
                "  SQL_DIALECT     = 'EXASOL_RLS'\n" +
                "  CONNECTION_NAME = 'jdbc_exasol_connection'\n" +
                "  SCHEMA_NAME     = 'ROW_LEVEL_SECURITY_TEST_SCHEMA'");
        //Create a script for comparing tables
        statement.execute("CREATE OR REPLACE SCRIPT row_level_security_test_schema.compare_table_contents (table_a, table_b) RETURNS TABLE AS\n" +
                "    exit(\n" +
                "       query([[\n" +
                "           (SELECT '<<<', A.*\n" +
                "           FROM\n" +
                "                (SELECT * FROM ::a\n" +
                "                EXCEPT\n" +
                "                SELECT * FROM ::b\n" +
                "                ) A\n" +
                "            )\n" +
                "            UNION ALL\n" +
                "            (SELECT '>>>', A.*\n" +
                "            FROM\n" +
                "                (SELECT * FROM ::b\n" +
                "                EXCEPT\n" +
                "                SELECT * FROM ::a\n" +
                "                ) A\n" +
                "            )\n" +
                "        ]], {a = table_a, b = table_b}\n" +
                "        )\n" +
                "    )");
        //Create users
        createUser("RLS_USR_1");
        createUser("RLS_USR_2");
    }

    private static void createUser(String userName) throws SQLException {
        statement.execute("DROP USER IF EXISTS " + userName + " CASCADE");
        statement.execute("CREATE USER " + userName + " IDENTIFIED BY \"" + userName + "\"");
        statement.execute("GRANT ALL PRIVILEGES TO " + userName + "");
    }

    private static void createUnprotectedTableTestTable() throws SQLException {
        statement.execute("CREATE OR REPLACE TABLE row_level_security_test_schema.rls_sales_unprotected\n" +
                "    (\n" +
                "    order_id DECIMAL(18,0),\n" +
                "    customer VARCHAR(50),\n" +
                "    product VARCHAR(100),\n" +
                "    quantity DECIMAL(18,0)\n" +
                "    )");
        statement.execute("INSERT INTO row_level_security_test_schema.rls_sales_unprotected VALUES\n" +
                "(1, 'Chicken Inc', 'Wheat', 100),\n" +
                "(2, 'Goat Inc', 'Carrot', 10),\n" +
                "(3, 'Donkey Inc', 'Carrot', 33),\n" +
                "(4, 'Chicken Inc', 'Wheat', 4)");
    }

    @Test
    void testUnprotectedTableWithAdmin() throws SQLException {
        statement.execute("CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_admin(order_id, customer, product, quantity) AS\n" +
                "SELECT * FROM VALUES\n" +
                "(1, 'Chicken Inc', 'Wheat', 100),\n" +
                "(2, 'Goat Inc', 'Carrot', 10),\n" +
                "(3, 'Donkey Inc', 'Carrot', 33),\n" +
                "(4, 'Chicken Inc', 'Wheat', 4)");
        ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales_unprotected', 'row_level_security_test_schema.rls_sales_user_admin')");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testUnprotectedTableWithUser1() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_1");
        statement.execute("CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_1(order_id, customer, product, quantity) AS\n" +
                "SELECT * FROM VALUES\n" +
                "(1, 'Chicken Inc', 'Wheat', 100),\n" +
                "(2, 'Goat Inc', 'Carrot', 10),\n" +
                "(3, 'Donkey Inc', 'Carrot', 33),\n" +
                "(4, 'Chicken Inc', 'Wheat', 4)");
        ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales_unprotected', 'row_level_security_test_schema.rls_sales_user_1')");
        assertThat(resultSet.next(), equalTo(false));
    }

    @Test
    void testUnprotectedTableWithUser2() throws SQLException {
        statement.execute("IMPERSONATE rls_usr_2");
        statement.execute("CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_2(order_id, customer, product, quantity) AS\n" +
                "SELECT * FROM VALUES\n" +
                "(1, 'Chicken Inc', 'Wheat', 100),\n" +
                "(2, 'Goat Inc', 'Carrot', 10),\n" +
                "(3, 'Donkey Inc', 'Carrot', 33),\n" +
                "(4, 'Chicken Inc', 'Wheat', 4)");
        ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales_unprotected', 'row_level_security_test_schema.rls_sales_user_2')");
        assertThat(resultSet.next(), equalTo(false));
    }
}