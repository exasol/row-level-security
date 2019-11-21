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
    exa_row_roles DECIMAL(20,0)
    );

INSERT INTO row_level_security_test_schema.rls_sales VALUES
(1, 'Chicken Inc', 'Wheat', 100, NULL),
(2, 'Goat Inc', 'Grass', 2, 0),
(3, 'Donkey Inc', 'Carrot', 33, 1),
(4, 'Chicken Inc', 'Wheat', 4, 2),
(5, 'Chicken Inc', 'Wheat', 45, 3),
(6, 'Donkey Inc', 'Carrot', 67, 4),
(7, 'Goat Inc', 'Grass', 84, 5),
(8, 'Chicken Inc', 'Wheat', 44, 6),
(9, 'Chicken Inc', 'Wheat', 64, 7),
(10, 'Donkey Inc', 'Carrot', 2, 8),
(11, 'Goat Inc', 'Grass', 54, 9),
(12, 'Chicken Inc', 'Wheat', 44, 10),
(13, 'Chicken Inc', 'Wheat', 65, 11),
(14, 'Donkey Inc', 'Carrot', 89, 12),
(15, 'Chicken Inc', 'Wheat', 3, 13),
(16, 'Goat Inc', 'Grass', 34, 14),
(17, 'Donkey Inc', 'Carrot', 58, 15),
(18, 'Donkey Inc', 'Wheat', 56, 9223372036854775808);

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

--Create rls users configure table
CREATE OR REPLACE TABLE row_level_security_test_schema.exa_rls_users
    (
    exa_user_name VARCHAR(200),
    exa_role_mask DECIMAL(20,0)
    );

INSERT INTO row_level_security_test_schema.exa_rls_users VALUES
('RLS_USR_1', NULL),
('RLS_USR_2', 1),
('RLS_USR_3', 3),
('RLS_USR_4', 15),
('SYS', 15);

--Create Virtual Schema
CREATE CONNECTION jdbc_exasol_connection
TO 'jdbc:exa:host:port'
USER 'user'
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
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33),
(4, 'Chicken Inc', 'Wheat', 4),
(5, 'Chicken Inc', 'Wheat', 45),
(6, 'Donkey Inc', 'Carrot', 67),
(7, 'Goat Inc', 'Grass', 84),
(8, 'Chicken Inc', 'Wheat', 44),
(9, 'Chicken Inc', 'Wheat', 64),
(10, 'Donkey Inc', 'Carrot', 2),
(11, 'Goat Inc', 'Grass', 54),
(12, 'Chicken Inc', 'Wheat', 44),
(13, 'Chicken Inc', 'Wheat', 65),
(14, 'Donkey Inc', 'Carrot', 89),
(15, 'Chicken Inc', 'Wheat', 3),
(16, 'Goat Inc', 'Grass', 34),
(17, 'Donkey Inc', 'Carrot', 58),
(18, 'Donkey Inc', 'Wheat', 56);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_admin');

--User rls_usr_1
IMPERSONATE rls_usr_1;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_1(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(18, 'Donkey Inc', 'Wheat', 56);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_1');

--User rls_usr_2
IMPERSONATE rls_usr_2;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_2(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33),
(5, 'Chicken Inc', 'Wheat', 45),
(7, 'Goat Inc', 'Grass', 84),
(9, 'Chicken Inc', 'Wheat', 64),
(11, 'Goat Inc', 'Grass', 54),
(13, 'Chicken Inc', 'Wheat', 65),
(15, 'Chicken Inc', 'Wheat', 3),
(17, 'Donkey Inc', 'Carrot', 58),
(18, 'Donkey Inc', 'Wheat', 56);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_2');

--User rls_usr_3
IMPERSONATE rls_usr_3;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_3(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33),
(4, 'Chicken Inc', 'Wheat', 4),
(5, 'Chicken Inc', 'Wheat', 45),
(7, 'Goat Inc', 'Grass', 84),
(8, 'Chicken Inc', 'Wheat', 44),
(9, 'Chicken Inc', 'Wheat', 64),
(11, 'Goat Inc', 'Grass', 54),
(12, 'Chicken Inc', 'Wheat', 44),
(13, 'Chicken Inc', 'Wheat', 65),
(15, 'Chicken Inc', 'Wheat', 3),
(16, 'Goat Inc', 'Grass', 34),
(17, 'Donkey Inc', 'Carrot', 58),
(18, 'Donkey Inc', 'Wheat', 56);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_3');

--User rls_usr_4
IMPERSONATE rls_usr_4;
SELECT * FROM virtual_schema_rls.rls_sales;
CREATE OR REPLACE VIEW row_level_security_test_schema.rls_sales_user_4(order_id, customer, product, quantity) AS
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33),
(4, 'Chicken Inc', 'Wheat', 4),
(5, 'Chicken Inc', 'Wheat', 45),
(6, 'Donkey Inc', 'Carrot', 67),
(7, 'Goat Inc', 'Grass', 84),
(8, 'Chicken Inc', 'Wheat', 44),
(9, 'Chicken Inc', 'Wheat', 64),
(10, 'Donkey Inc', 'Carrot', 2),
(11, 'Goat Inc', 'Grass', 54),
(12, 'Chicken Inc', 'Wheat', 44),
(13, 'Chicken Inc', 'Wheat', 65),
(14, 'Donkey Inc', 'Carrot', 89),
(15, 'Chicken Inc', 'Wheat', 3),
(16, 'Goat Inc', 'Grass', 34),
(17, 'Donkey Inc', 'Carrot', 58),
(18, 'Donkey Inc', 'Wheat', 56);
EXECUTE SCRIPT row_level_security_test_schema.compare_table_contents('virtual_schema_rls.rls_sales', 'row_level_security_test_schema.rls_sales_user_4');
IMPERSONATE SYS;