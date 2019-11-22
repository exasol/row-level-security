# Row Level Security

Row-Level Security (short "RLS") is a security mechanism based on Exasol's Virtual Schemas. It allows database administrators to set an access to a table's row basing on user's roles and user's names.

RLS only supports Exasol databases. That means you cannot use RLS between Exasol and a 3rd-party data source.

The RLS installation package contains everything you need to extend an existing Exasol installation with row-level security.

## Row Level Security Tables Preparations

### Role-based security

Role-based security is a way to secure a table assigning roles to each user and specifying the roles which are allowed to see a row for each row in the table. 

In case you want to use Role-based security, you must add an additional column `EXA_ROW_ROLES DECIMAL(20,0)` to the tables you want to secure. 

Example:

```sql
CREATE OR REPLACE TABLE ROW_LEVEL_SECURITY_TEST_SCHEMA.RLS_SALES 
    (  
    ORDER_ID DECIMAL(18,0),  
    CUSTOMER VARCHAR(50),  
    PRODUCT VARCHAR(100),  
    QUANTITY DECIMAL(18,0),
    EXA_ROW_ROLES DECIMAL(20,0)  
    ); 
```

Then fill the `EXA_ROW_ROLES` column with the roles. 
Later versions of RLS will provide UDF functions that make administration of RLS more user-friendly.

Create a configure table with user roles and fill it with the roles:

```sql
CREATE OR REPLACE TABLE ROW_LEVEL_SECURITY_TEST_SCHEMA.EXA_RLS_USERS  
    (  
    EXA_USER_NAME VARCHAR(200),
    EXA_ROLE_MASK DECIMAL(20,0)  
    );  

INSERT INTO ROW_LEVEL_SECURITY_TEST_SCHEMA.EXA_RLS_USERS VALUES 
('RLS_USR_1', NULL),
('RLS_USR_2', 1),
('RLS_USR_3', 3),
('RLS_USR_4', 15),
('SYS', 15);
```

### Tenant-based security

Tenant-based security is a way to secure a table assigning each row to only one user. 

If you want to use tenant security, you must add an additional column `EXA_ROW_TENANTS VARCHAR(128)` to the tables you want to secure. 

Example:

```sql
CREATE OR REPLACE TABLE ROW_LEVEL_SECURITY_TEST_SCHEMA.RLS_SALES 
    (  
    ORDER_ID DECIMAL(18,0),  
    CUSTOMER VARCHAR(50),  
    PRODUCT VARCHAR(100),  
    QUANTITY DECIMAL(18,0),
    EXA_ROW_TENANTS VARCHAR(128)  
    );     
```

For each row define which tenant it belongs to.

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

Define the connection to the other Exasol instance as shown below.

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

- RLS only works with Exasol as source and destination of the Virtual Schema.
- RLS Virtual Schema do not support JOIN capabilities.