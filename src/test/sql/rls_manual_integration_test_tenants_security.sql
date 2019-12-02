DROP FORCE VIRTUAL SCHEMA IF EXISTS VIRTUAL_SCHEMA_RLS CASCADE;
DROP SCHEMA IF EXISTS row_level_security_test_schema CASCADE;
DROP CONNECTION IF EXISTS jdbc_exasol_connection;

--Create test schema
CREATE SCHEMA row_level_security_test_schema;

--Create test table
CREATE OR REPLACE TABLE row_level_security_test_schema.rls_sales
    (
    order_id DECIMAL(18,0),
    customer VARCHAR(50),
    product VARCHAR(100),
    quantity DECIMAL(18,0),
    exa_row_tenant VARCHAR(128)
    );

INSERT INTO row_level_security_test_schema.rls_sales VALUES
(1, 'Chicken Inc', 'Wheat', 100, NULL),
(2, 'Goat Inc', 'Carrot', 10, 'NOBODY'),
(3, 'Donkey Inc', 'Carrot', 33, 'RLS_USR_1'),
(4, 'Chicken Inc', 'Wheat', 4, 'RLS_USR_2'),
(5, 'Chicken Inc', 'Wheat', 45, 'RLS_USR_3'),
(6, 'Donkey Inc', 'Carrot', 67, 'RLS_USR_4'),
(7, 'Goat Inc', 'Grass', 84, 'RLS_USR_4'),
(8, 'Chicken Inc', 'Wheat', 44, 'RLS_USR_3'),
(9, 'Chicken Inc', 'Wheat', 64, 'RLS_USR_2'),
(10, 'Donkey Inc', 'Carrot', 2, 'RLS_USR_1');

--Create users
DROP USER IF EXISTS RLS_USR_1 CASCADE;
CREATE USER RLS_USR_1 IDENTIFIED BY "RLS_USR_1";
GRANT ALL PRIVILEGES TO RLS_USR_1;

DROP USER IF EXISTS RLS_USR_2 CASCADE;
CREATE USER RLS_USR_2 IDENTIFIED BY "RLS_USR_2";
GRANT ALL PRIVILEGES TO RLS_USR_2;

DROP USER IF EXISTS RLS_USR_3 CASCADE;
CREATE USER RLS_USR_3 IDENTIFIED BY "RLS_USR_3";
GRANT ALL PRIVILEGES TO RLS_USR_3;

DROP USER IF EXISTS RLS_USR_4 CASCADE;
CREATE USER RLS_USR_4 IDENTIFIED BY "RLS_USR_4";
GRANT ALL PRIVILEGES TO RLS_USR_4;

--Create Virtual Schema
CREATE CONNECTION jdbc_exasol_connection
TO 'jdbc:exa:host:port'
USER 'sys'
IDENTIFIED BY 'password';

--/
CREATE OR REPLACE JAVA ADAPTER SCRIPT row_level_security_test_schema.adapter_script_exasol_rls AS
    %scriptclass com.exasol.adapter.RequestDispatcher;
    %jar /buckets/bfsdefault/rls/row-level-security-0.1.0.jar;
    %jar /buckets/bfsdefault/rls/exajdbc.jar;
/
;

CREATE VIRTUAL SCHEMA virtual_schema_rls USING row_level_security_test_schema.adapter_script_exasol_rls WITH
  SQL_DIALECT     = 'EXASOL_RLS'
  CONNECTION_NAME = 'jdbc_exasol_connection'
  SCHEMA_NAME     = 'ROW_LEVEL_SECURITY_TEST_SCHEMA'
  DEBUG_ADDRESS = '<host>:<port>'
  LOG_LEVEL = 'ALL';

--Create a script for comparing tables
CREATE OR REPLACE SCRIPT row_level_security_test_schema.compare_table_contents (table_a, table_b) RETURNS TABLE AS
    exit(
       query([[
           (SELECT '<<<', A.*
           FROM
                (SELECT * FROM ::a
                EXCEPT
                SELECT * FROM ::b
                ) A
            )
            UNION ALL
            (SELECT '>>>', A.*
            FROM
                (SELECT * FROM ::b
                EXCEPT
                SELECT * FROM ::a
                ) A
            )
        ]], {a = table_a, b = table_b}
        )
    )
;

--Testing
--Administrator
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_admin(order_id, customer, product, quantity) AS
SELECT * FROM virtual_schema_rls.rls_sales WHERE 1=0;
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_admin');

--User rls_usr_1
IMPERSONATE rls_usr_1;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_1(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33),
(10, 'Donkey Inc', 'Carrot', 2);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_1');

--User rls_usr_2
IMPERSONATE rls_usr_2;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_2(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(4, 'Chicken Inc', 'Wheat', 4),
(9, 'Chicken Inc', 'Wheat', 64);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_2');

--User rls_usr_3
IMPERSONATE rls_usr_3;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_3(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(5, 'Chicken Inc', 'Wheat', 45),
(8, 'Chicken Inc', 'Wheat', 44);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_3');

--User rls_usr_4
IMPERSONATE rls_usr_4;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_4(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(6, 'Donkey Inc', 'Carrot', 67),
(7, 'Goat Inc', 'Grass', 84);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_4');
IMPERSONATE SYS;