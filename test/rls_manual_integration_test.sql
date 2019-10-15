DROP SCHEMA IF EXISTS row_level_security_test_schema CASCADE;

--Create test schema
CREATE SCHEMA row_level_security_test_schema;

--Create test table
CREATE OR REPLACE TABLE row_level_security_test_schema.rls_sales
    (
    order_id DECIMAL(18,0),
    customer VARCHAR(50),
    product VARCHAR(100),
    quantity DECIMAL(18,0),
    exa_row_rules DECIMAL(20,0)
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
(17, 'Donkey Inc', 'Carrot', 58, 15);

SELECT * FROM row_level_security_test_schema.rls_sales;


--Create users and grant privileges
CREATE USER rls_usr_1 IDENTIFIED BY "rls_usr_1";
GRANT CREATE SESSION TO rls_usr_1;
GRANT CREATE VIEW TO rls_usr_1;
GRANT SELECT ON row_level_security_test_schema TO rls_usr_1;
GRANT IMPERSONATE ANY USER TO rls_usr_1;

CREATE USER rls_usr_2 IDENTIFIED BY "rls_usr_2";
GRANT CREATE SESSION TO rls_usr_2;
GRANT CREATE VIEW TO rls_usr_2;
GRANT SELECT ON row_level_security_test_schema TO rls_usr_2;
GRANT IMPERSONATE ANY USER TO rls_usr_2;

CREATE USER rls_usr_3 IDENTIFIED BY "rls_usr_3";
GRANT CREATE SESSION TO rls_usr_3;
GRANT CREATE VIEW TO rls_usr_3;
GRANT SELECT ON row_level_security_test_schema TO rls_usr_3;
GRANT IMPERSONATE ANY USER TO rls_usr_3;

CREATE USER rls_usr_4 IDENTIFIED BY "rls_usr_4";
GRANT CREATE SESSION TO rls_usr_4;
GRANT CREATE VIEW TO rls_usr_4;
GRANT SELECT ON row_level_security_test_schema TO rls_usr_4;
GRANT IMPERSONATE ANY USER TO rls_usr_4;


--Create rls users configuration table
CREATE OR REPLACE TABLE row_level_security_test_schema.rls_users
    (
    exa_user_name VARCHAR(200),
    exa_role_mask DECIMAL(20,0)
    );

INSERT INTO row_level_security_test_schema.rls_users VALUES ('rls_usr_1', NULL);
INSERT INTO row_level_security_test_schema.rls_users VALUES ('rls_usr_2', 1);
INSERT INTO row_level_security_test_schema.rls_users VALUES ('rls_usr_3', 3);
INSERT INTO row_level_security_test_schema.rls_users VALUES ('rls_usr_4', 15);

SELECT * FROM row_level_security_test_schema.rls_users;

--Create a script for comparing tables
CREATE OR REPLACE SCRIPT compare_table_contents (table_a, table_b) RETURNS TABLE AS
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
--User rls_usr_1
IMPERSONATE rls_usr_1;
OPEN SCHEMA row_level_security_test_schema;
SELECT * FROM rls_sales;
CREATE OR REPLACE VIEW rls_sales_user_1 AS SELECT * FROM rls_sales WHERE 1=0;
SELECT * FROM rls_sales_user_1;
EXECUTE SCRIPT compare_table_contents('rls_sales', 'rls_sales_user_1');

--User rls_usr_2
IMPERSONATE rls_usr_2;
OPEN SCHEMA row_level_security_test_schema;
SELECT * FROM rls_sales;
CREATE OR REPLACE VIEW rls_sales_user_2(order_id, customer, product, quantity, exa_row_rules) AS
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33, 1),
(5, 'Chicken Inc', 'Wheat', 45, 3),
(7, 'Goat Inc', 'Grass', 84, 5),
(9, 'Chicken Inc', 'Wheat', 64, 7),
(11, 'Goat Inc', 'Grass', 54, 9),
(13, 'Chicken Inc', 'Wheat', 65, 11),
(15, 'Chicken Inc', 'Wheat', 3, 13),
(17, 'Donkey Inc', 'Carrot', 58, 15);
SELECT * FROM rls_sales_user_2;
EXECUTE SCRIPT compare_table_contents('rls_sales', 'rls_sales_user_2');

--User rls_usr_3
IMPERSONATE rls_usr_3;
OPEN SCHEMA row_level_security_test_schema;
SELECT * FROM rls_sales;
CREATE OR REPLACE VIEW rls_sales_user_3(order_id, customer, product, quantity, exa_row_rules) AS
SELECT * FROM VALUES
(3, 'Donkey Inc', 'Carrot', 33, 1),
(4, 'Chicken Inc', 'Wheat', 4, 2),
(5, 'Chicken Inc', 'Wheat', 45, 3),
(7, 'Goat Inc', 'Grass', 84, 5),
(8, 'Chicken Inc', 'Wheat', 44, 6),
(9, 'Chicken Inc', 'Wheat', 64, 7),
(11, 'Goat Inc', 'Grass', 54, 9),
(12, 'Chicken Inc', 'Wheat', 44, 10),
(13, 'Chicken Inc', 'Wheat', 65, 11),
(15, 'Chicken Inc', 'Wheat', 3, 13),
(16, 'Goat Inc', 'Grass', 34, 14),
(17, 'Donkey Inc', 'Carrot', 58, 15);
SELECT * FROM rls_sales_user_3;
EXECUTE SCRIPT compare_table_contents('rls_sales', 'rls_sales_user_3');


--User rls_usr_4
IMPERSONATE rls_usr_4;
OPEN SCHEMA row_level_security_test_schema;
SELECT * FROM rls_sales;
CREATE OR REPLACE VIEW rls_sales_user_4(order_id, customer, product, quantity, exa_row_rules) AS
SELECT * FROM VALUES
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
(17, 'Donkey Inc', 'Carrot', 58, 15);
SELECT * FROM rls_sales_user_4;
EXECUTE SCRIPT compare_table_contents('rls_sales', 'rls_sales_user_4');