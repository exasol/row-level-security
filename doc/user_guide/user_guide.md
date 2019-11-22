# Row Level Security

Row Level Security is an extension of Virtual Schemas Exasol dialect. It allows database administrators to set an access to a table's row basing on user's roles and user's names.
You don't have to install any JDBC driver, because it is already installed in the Exasol database and also included in the JAR of the JDBC adapter.

## Row Level Security Tables Preparations

### Roles security

1. In case you want to use roles security, you must add an additional column EXA_ROW_ROLES DECIMAL(20,0) to the tables you want to secure. 
Example:
```sql
CREATE OR REPLACE TABLE row_level_security_test_schema.rls_sales 
    (  
    order_id DECIMAL(18,0),  
    customer VARCHAR(50),  
    product VARCHAR(100),  
    quantity DECIMAL(18,0),
    exa_row_roles DECIMAL(20,0)  
    ); 
```

Then fill the EXA_ROW_ROLES column with the roles.

2. Create a configure table with user roles and fill it with the roles:
```sql
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
```

### Tenants security

If you want to use tenant security, you must add an additional column EXA_ROW_TENANTS VARCHAR(128) to the tables you want to secure. 
Example:
```sql
CREATE OR REPLACE TABLE row_level_security_test_schema.rls_sales 
    (  
    order_id DECIMAL(18,0),  
    customer VARCHAR(50),  
    product VARCHAR(100),  
    quantity DECIMAL(18,0),
    exa_row_tenants VARCHAR(128)  
    );     
```

Then fill the EXA_ROW_TENANTS column with the user's names.

## Creating Virtual Schema

Upload the latest available release of [Row Level Security](https://github.com/exasol/row-level-security/releases) to Bucket FS.

Then create a schema or use an existing one to hold the adapter script.

```sql
CREATE SCHEMA TEST_SCHEMA;
```

The SQL statement below creates the adapter script, defines the Java class that serves as entry point and tells the UDF framework where to find the libraries (JAR files) for Virtual Schema and database driver.

```sql
CREATE JAVA ADAPTER SCRIPT TEST_SCHEMA.TEST_ADAPTER_SCRIPT AS
    %scriptclass com.exasol.adapter.RequestDispatcher;
    %jar /buckets/<BFS service>/<bucket>/row-level-security-<version>-all-dependencies.jar;
/
```

### Defining a Named Connection

```sql
CREATE CONNECTION EXASOL_JDBC_CONNECTION 
TO 'jdbc:exa:<host>:<port>' 
USER '<user>' 
IDENTIFIED BY '<password>';
```

### Creating a Virtual Schema

```sql
CREATE VIRTUAL SCHEMA VIRTUAL_SCHEMA_RLS 
    USING TEST_SCHEMA.TEST_ADAPTER_SCRIPT WITH
    SQL_DIALECT     = 'EXASOL_RLS'
    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'
    SCHEMA_NAME     = '<schema name>'
    IMPORT_FROM_EXA = 'true'
    EXA_CONNECTION_STRING = '<host>:<port>';
```

### Defining a Named Connection

Define the connection to the other Exasol cluster as shown below.

```sql
CREATE CONNECTION EXASOL_CONNECTION 
TO 'jdbc:exa:<host>:<port>' 
USER '<user>' 
IDENTIFIED BY '<password>';
```

### Creating a Virtual Schema

```sql
CREATE VIRTUAL SCHEMA <virtual schema name> 
    USING ADAPTER.JDBC_ADAPTER 
    WITH
    SQL_DIALECT     = 'EXASOL_RLS'
    CONNECTION_NAME = 'EXASOL_CONNECTION'
    SCHEMA_NAME     = '<schema name>';
```

### Limitations

- RLS Virtual Schema do not support JOIN capabilities.
