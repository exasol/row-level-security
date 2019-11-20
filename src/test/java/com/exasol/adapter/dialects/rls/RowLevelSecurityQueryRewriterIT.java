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
	private static ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
			ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE);
	private static Statement statement;

	@BeforeAll
	static void beforeAll() throws SQLException, BucketAccessException, InterruptedException {
		Bucket bucket = container.getDefaultBucket();
		Path pathToDriver = Path.of("src/test/resources/exajdbc.jar");
		bucket.uploadFile(pathToDriver, "exasol-jdbc.jar");
		TimeUnit.SECONDS.sleep(10); // FIXME: add the jar file directly to the RLS jar
		Path pathToRls = Path.of("target/row-level-security-0.1.0-all-dependencies.jar");
		bucket.uploadFile(pathToRls, "row-level-security-0.1.0-all-dependencies.jar");
		Connection connection = container.createConnectionForUser(container.getUsername(), container.getPassword());
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
		statement.execute("CREATE CONNECTION jdbc_exasol_connection " + "TO 'jdbc:exa:localhost:8888' " + "USER '"
				+ container.getUsername().toLowerCase() + "' " + "IDENTIFIED BY '" + container.getPassword() + "'");
		statement.execute("CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.adapter_script_exasol_rls AS\n"
				+ "    %scriptclass com.exasol.adapter.RequestDispatcher;\n"
				+ "    %jar /buckets/bfsdefault/default/row-level-security-0.1.0-all-dependencies.jar;\n"
				+ "    %jar /buckets/bfsdefault/default/exasol-jdbc.jar;\n" + "/");
		TimeUnit.SECONDS.sleep(20); // FIXME: need to be fixed in the container
		statement.execute("CREATE VIRTUAL SCHEMA virtual_schema_rls USING RLS_SCHEMA.adapter_script_exasol_rls "
				+ "WITH\n" + "  SQL_DIALECT     = 'EXASOL_RLS'\n" + "  CONNECTION_NAME = 'jdbc_exasol_connection'\n"
				+ "  SCHEMA_NAME     = 'RLS_SCHEMA'");
		// Create a script for comparing tables
		createComparingScript();
		// Create users
		createUser("RLS_USR_1");
		createUser("RLS_USR_2");
		createUser("RLS_USR_3");
		createUser("RLS_USR_4");
	}

	private static void createRolesAndTenantsTestTable() throws SQLException {
		statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_roles_and_tenants\n" + "    (\n"
				+ "    order_id DECIMAL(18,0),\n" + "    customer VARCHAR(50),\n" + "    product VARCHAR(100),\n"
				+ "    quantity DECIMAL(18,0),\n" + "    exa_row_roles DECIMAL(20,0),\n"
				+ "    exa_row_tenants VARCHAR(128)\n" + "    )");
		statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_roles_and_tenants VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100, NULL, 'RLS_USR_1'),\n"
				+ "(2, 'Goat Inc', 'Grass', 2, 0, 'RLS_USR_1'),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 1, 'RLS_USR_1'),\n"
				+ "(4, 'Chicken Inc', 'Wheat', 4, 2, 'RLS_USR_1'),\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 3, 'RLS_USR_2'),\n"
				+ "(6, 'Donkey Inc', 'Carrot', 67, 4, 'RLS_USR_2'),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 5, 'RLS_USR_2'),\n"
				+ "(8, 'Chicken Inc', 'Wheat', 44, 6, 'RLS_USR_3'),\n"
				+ "(9, 'Chicken Inc', 'Wheat', 64, 7, 'RLS_USR_3'),\n"
				+ "(10, 'Donkey Inc', 'Carrot', 2, 8, 'RLS_USR_3'),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9, 'RLS_USR_3'),\n"
				+ "(12, 'Chicken Inc', 'Wheat', 44, 10, 'RLS_USR_4'),\n"
				+ "(13, 'Chicken Inc', 'Wheat', 65, 11, 'RLS_USR_4'),\n"
				+ "(14, 'Donkey Inc', 'Carrot', 89, 12, 'RLS_USR_4'),\n"
				+ "(15, 'Chicken Inc', 'Wheat', 3, 13, 'RLS_USR_4'),\n"
				+ "(16, 'Goat Inc', 'Grass', 34, 14, 'NOBODY'),\n" + "(17, 'Donkey Inc', 'Carrot', 58, 15, NULL),\n"
				+ "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808, 'NOBODY'),\n"
				+ "(19, 'Donkey Inc', 'Wheat', 50, 9223372036854775808, 'RLS_USR_1')");
	}

	private static void createUser(String userName) throws SQLException {
		statement.execute("DROP USER IF EXISTS " + userName + " CASCADE");
		statement.execute("CREATE USER " + userName + " IDENTIFIED BY \"" + userName + "\"");
		statement.execute("GRANT ALL PRIVILEGES TO " + userName + "");
	}

	private static void createRolesTestTables() throws SQLException {
		statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_roles\n" + "    (\n"
				+ "    order_id DECIMAL(18,0),\n" + "    customer VARCHAR(50),\n" + "    product VARCHAR(100),\n"
				+ "    quantity DECIMAL(18,0),\n" + "    exa_row_roles DECIMAL(20,0)\n" + "    )");
		statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_roles VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100, NULL),\n" + "(2, 'Goat Inc', 'Grass', 2, 0),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 1),\n" + "(4, 'Chicken Inc', 'Wheat', 4, 2),\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 3),\n" + "(6, 'Donkey Inc', 'Carrot', 67, 4),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 5),\n" + "(8, 'Chicken Inc', 'Wheat', 44, 6),\n"
				+ "(9, 'Chicken Inc', 'Wheat', 64, 7),\n" + "(10, 'Donkey Inc', 'Carrot', 2, 8),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9),\n" + "(12, 'Chicken Inc', 'Wheat', 44, 10),\n"
				+ "(13, 'Chicken Inc', 'Wheat', 65, 11),\n" + "(14, 'Donkey Inc', 'Carrot', 89, 12),\n"
				+ "(15, 'Chicken Inc', 'Wheat', 3, 13),\n" + "(16, 'Goat Inc', 'Grass', 34, 14),\n"
				+ "(17, 'Donkey Inc', 'Carrot', 58, 15),\n" + "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
		statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.exa_rls_users\n" + "    (\n"
				+ "    exa_user_name VARCHAR(200),\n" + "    exa_role_mask DECIMAL(20,0)\n" + "    )");
		statement.execute("INSERT INTO RLS_SCHEMA.exa_rls_users VALUES \n" + "('RLS_USR_1', NULL),\n"
				+ "('RLS_USR_2', 1),\n" + "('RLS_USR_3', 3),\n" + "('RLS_USR_4', 15),\n" + "('SYS', 15)");
	}

	private static void createComparingScript() throws SQLException {
		statement.execute("CREATE OR REPLACE SCRIPT RLS_SCHEMA.compare_table_contents (table_a, table_b) "
				+ "RETURNS TABLE AS\n" + "    exit(\n" + "       query([[\n" + "           (SELECT '<<<', A.*\n"
				+ "           FROM\n" + "                (SELECT * FROM ::a\n" + "                EXCEPT\n"
				+ "                SELECT * FROM ::b\n" + "                ) A\n" + "            )\n"
				+ "            UNION ALL\n" + "            (SELECT '>>>', A.*\n" + "            FROM\n"
				+ "                (SELECT * FROM ::b\n" + "                EXCEPT\n"
				+ "                SELECT * FROM ::a\n" + "                ) A\n" + "            )\n"
				+ "        ]], {a = table_a, b = table_b}\n" + "        )\n" + "    )");
	}

	private static void createTenantsTestTable() throws SQLException {
		statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_tenants\n" + "    (\n"
				+ "    order_id DECIMAL(18,0),\n" + "    customer VARCHAR(50),\n" + "    product VARCHAR(100),\n"
				+ "    quantity DECIMAL(18,0),\n" + "    exa_row_tenants VARCHAR(128)\n" + "    )");
		statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_tenants VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100, NULL),\n" + "(2, 'Goat Inc', 'Carrot', 10, 'NOBODY'),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 'RLS_USR_1'),\n" + "(4, 'Chicken Inc', 'Wheat', 4, 'RLS_USR_2'),\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 'RLS_USR_3'),\n" + "(6, 'Donkey Inc', 'Carrot', 67, 'RLS_USR_4'),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 'RLS_USR_4'),\n" + "(8, 'Chicken Inc', 'Wheat', 44, 'RLS_USR_3'),\n"
				+ "(9, 'Chicken Inc', 'Wheat', 64, 'RLS_USR_2'),\n" + "(10, 'Donkey Inc', 'Carrot', 2, 'RLS_USR_1')");
	}

	private static void createUnprotectedTableTestTable() throws SQLException {
		statement.execute("CREATE OR REPLACE TABLE RLS_SCHEMA.rls_sales_unprotected\n" + "    (\n"
				+ "    order_id DECIMAL(18,0),\n" + "    customer VARCHAR(50),\n" + "    product VARCHAR(100),\n"
				+ "    quantity DECIMAL(18,0)\n" + "    )");
		statement.execute("INSERT INTO RLS_SCHEMA.rls_sales_unprotected VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100),\n" + "(2, 'Goat Inc', 'Carrot', 10),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33),\n" + "(4, 'Chicken Inc', 'Wheat', 4)");
	}

	@Test
	void testUnprotectedTableWithAdmin() throws SQLException {
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin"
				+ "(order_id, customer, product, quantity) AS\n" + "SELECT * FROM VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100),\n" + "(2, 'Goat Inc', 'Carrot', 10),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33),\n" + "(4, 'Chicken Inc', 'Wheat', 4)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_unprotected', 'RLS_SCHEMA.rls_sales_user_admin')");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testUnprotectedTableWithUser1() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_1");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1"
				+ "(order_id, customer, product, quantity) AS\n" + "SELECT * FROM VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100),\n" + "(2, 'Goat Inc', 'Carrot', 10),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33),\n" + "(4, 'Chicken Inc', 'Wheat', 4)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_unprotected', 'RLS_SCHEMA.rls_sales_user_1')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testUnprotectedTableWithUser2() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_2");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2"
				+ "(order_id, customer, product, quantity) AS\n" + "SELECT * FROM VALUES\n"
				+ "(1, 'Chicken Inc', 'Wheat', 100),\n" + "(2, 'Goat Inc', 'Carrot', 10),\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33),\n" + "(4, 'Chicken Inc', 'Wheat', 4)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_unprotected', 'RLS_SCHEMA.rls_sales_user_2')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testTenantsTableWithAdmin() throws SQLException {
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin"
				+ "(order_id, customer, product, quantity, exa_row_tenants) AS\n"
				+ "SELECT * FROM virtual_schema_rls.rls_sales_tenants WHERE 1=0");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_admin')");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testTenantsTableWithUser1() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_1");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1"
				+ "(order_id, customer, product, quantity, exa_row_tenants) AS\n" + "SELECT * FROM VALUES\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 'RLS_USR_1'),\n" + "(10, 'Donkey Inc', 'Carrot', 2, 'RLS_USR_1')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_1')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testTenantsTableWithUser2() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_2");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2"
				+ "(order_id, customer, product, quantity, exa_row_tenants) AS\n" + "SELECT * FROM VALUES\n"
				+ "(4, 'Chicken Inc', 'Wheat', 4, 'RLS_USR_2'),\n" + "(9, 'Chicken Inc', 'Wheat', 64, 'RLS_USR_2')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_2')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testTenantsTableWithUser3() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_3");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_3"
				+ "(order_id, customer, product, quantity, exa_row_tenants) AS\n" + "SELECT * FROM VALUES\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 'RLS_USR_3'),\n" + "(8, 'Chicken Inc', 'Wheat', 44, 'RLS_USR_3')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_3')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testTenantsTableWithUser4() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_4");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_4"
				+ "(order_id, customer, product, quantity, exa_row_tenants) AS\n" + "SELECT * FROM VALUES\n"
				+ "(6, 'Donkey Inc', 'Carrot', 67, 'RLS_USR_4'),\n" + "(7, 'Goat Inc', 'Grass', 84, 'RLS_USR_4')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_tenants', 'RLS_SCHEMA.rls_sales_user_4')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesTableWithAdmin() throws SQLException {
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin"
				+ "(order_id, customer, product, quantity, exa_row_roles) AS\n" + "SELECT * FROM VALUES\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 1),\n" + "(4, 'Chicken Inc', 'Wheat', 4, 2),\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 3),\n" + "(6, 'Donkey Inc', 'Carrot', 67, 4),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 5),\n" + "(8, 'Chicken Inc', 'Wheat', 44, 6),\n"
				+ "(9, 'Chicken Inc', 'Wheat', 64, 7),\n" + "(10, 'Donkey Inc', 'Carrot', 2, 8),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9),\n" + "(12, 'Chicken Inc', 'Wheat', 44, 10),\n"
				+ "(13, 'Chicken Inc', 'Wheat', 65, 11),\n" + "(14, 'Donkey Inc', 'Carrot', 89, 12),\n"
				+ "(15, 'Chicken Inc', 'Wheat', 3, 13),\n" + "(16, 'Goat Inc', 'Grass', 34, 14),\n"
				+ "(17, 'Donkey Inc', 'Carrot', 58, 15),\n" + "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_admin')");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesTableWithUser1() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_1");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1"
				+ "(order_id, customer, product, quantity, exa_row_roles) AS\n" + "SELECT * FROM VALUES\n"
				+ "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_1')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesTableWithUser2() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_2");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2"
				+ "(order_id, customer, product, quantity, exa_row_roles) AS\n" + "SELECT * FROM VALUES\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 1),\n" + "(5, 'Chicken Inc', 'Wheat', 45, 3),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 5),\n" + "(9, 'Chicken Inc', 'Wheat', 64, 7),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9),\n" + "(13, 'Chicken Inc', 'Wheat', 65, 11),\n"
				+ "(15, 'Chicken Inc', 'Wheat', 3, 13),\n" + "(17, 'Donkey Inc', 'Carrot', 58, 15),\n"
				+ "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_2')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesTableWithUser3() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_3");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_3"
				+ "(order_id, customer, product, quantity, exa_row_roles) AS\n" + "SELECT * FROM VALUES\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 1),\n" + "(4, 'Chicken Inc', 'Wheat', 4, 2),\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 3),\n" + "(7, 'Goat Inc', 'Grass', 84, 5),\n"
				+ "(8, 'Chicken Inc', 'Wheat', 44, 6),\n" + "(9, 'Chicken Inc', 'Wheat', 64, 7),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9),\n" + "(12, 'Chicken Inc', 'Wheat', 44, 10),\n"
				+ "(13, 'Chicken Inc', 'Wheat', 65, 11),\n" + "(15, 'Chicken Inc', 'Wheat', 3, 13),\n"
				+ "(16, 'Goat Inc', 'Grass', 34, 14),\n" + "(17, 'Donkey Inc', 'Carrot', 58, 15),\n"
				+ "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_3')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesTableWithUser4() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_4");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_4"
				+ "(order_id, customer, product, quantity, exa_row_roles) AS\n" + "SELECT * FROM VALUES\n"
				+ "(3, 'Donkey Inc', 'Carrot', 33, 1),\n" + "(4, 'Chicken Inc', 'Wheat', 4, 2),\n"
				+ "(5, 'Chicken Inc', 'Wheat', 45, 3),\n" + "(6, 'Donkey Inc', 'Carrot', 67, 4),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 5),\n" + "(8, 'Chicken Inc', 'Wheat', 44, 6),\n"
				+ "(9, 'Chicken Inc', 'Wheat', 64, 7),\n" + "(10, 'Donkey Inc', 'Carrot', 2, 8),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9),\n" + "(12, 'Chicken Inc', 'Wheat', 44, 10),\n"
				+ "(13, 'Chicken Inc', 'Wheat', 65, 11),\n" + "(14, 'Donkey Inc', 'Carrot', 89, 12),\n"
				+ "(15, 'Chicken Inc', 'Wheat', 3, 13),\n" + "(16, 'Goat Inc', 'Grass', 34, 14),\n"
				+ "(17, 'Donkey Inc', 'Carrot', 58, 15),\n" + "(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808)");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles', 'RLS_SCHEMA.rls_sales_user_4')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesAndTenantsTableWithAdmin() throws SQLException {
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_admin"
				+ "(order_id, customer, product, quantity, exa_row_roles, exa_row_tenants) AS\n"
				+ "SELECT * FROM virtual_schema_rls.rls_sales_roles_and_tenants WHERE 1=0");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_admin')");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesAndTenantsTableWithUser1() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_1");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_1"
				+ "(order_id, customer, product, quantity, exa_row_roles, exa_row_tenants) AS\n"
				+ "SELECT * FROM VALUES\n" + "(19, 'Donkey Inc', 'Wheat', 50, 9223372036854775808, 'RLS_USR_1')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_1')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesAndTenantsTableWithUser2() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_2");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_2"
				+ "(order_id, customer, product, quantity, exa_row_roles, exa_row_tenants) AS\n"
				+ "SELECT * FROM VALUES\n" + "(5, 'Chicken Inc', 'Wheat', 45, 3, 'RLS_USR_2'),\n"
				+ "(7, 'Goat Inc', 'Grass', 84, 5, 'RLS_USR_2')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_2')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesAndTenantsTableWithUser3() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_3");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_3"
				+ "(order_id, customer, product, quantity, exa_row_roles, exa_row_tenants) AS\n"
				+ "SELECT * FROM VALUES\n" + "(8, 'Chicken Inc', 'Wheat', 44, 6, 'RLS_USR_3'),\n"
				+ "(9, 'Chicken Inc', 'Wheat', 64, 7, 'RLS_USR_3'),\n"
				+ "(11, 'Goat Inc', 'Grass', 54, 9, 'RLS_USR_3')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_3')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}

	@Test
	void testRolesAndTenantsTableWithUser4() throws SQLException {
		statement.execute("IMPERSONATE rls_usr_4");
		statement.execute("CREATE OR REPLACE VIEW RLS_SCHEMA.rls_sales_user_4"
				+ "(order_id, customer, product, quantity, exa_row_roles, exa_row_tenants) AS\n"
				+ "SELECT * FROM VALUES\n" + "(12, 'Chicken Inc', 'Wheat', 44, 10, 'RLS_USR_4'),\n"
				+ "(13, 'Chicken Inc', 'Wheat', 65, 11, 'RLS_USR_4'),\n"
				+ "(14, 'Donkey Inc', 'Carrot', 89, 12, 'RLS_USR_4'),\n"
				+ "(15, 'Chicken Inc', 'Wheat', 3, 13, 'RLS_USR_4')");
		ResultSet resultSet = statement.executeQuery("EXECUTE SCRIPT RLS_SCHEMA.compare_table_contents"
				+ "('virtual_schema_rls.rls_sales_roles_and_tenants', 'RLS_SCHEMA.rls_sales_user_4')");
		statement.execute("IMPERSONATE SYS");
		assertThat(resultSet.next(), equalTo(false));
	}
}