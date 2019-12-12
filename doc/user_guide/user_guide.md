# Row Level Security

Row-Level Security (short "RLS") is a security mechanism based on Exasol's Virtual Schemas. It allows database administrators control access to a table's row depending on a user's roles and username.

RLS only supports Exasol databases. That means you cannot use RLS between Exasol and a 3rd-party data source.

The RLS installation package contains everything you need to extend an existing Exasol installation with row-level security.

## Introduction

### Row-Level Security Works on a Per-Schema Level

As mentioned before, RLS is a specialized implementation of a [Virtual Schema](https://github.com/exasol/virtual-schemas). As the name suggests a database schema is the scope that can be protected. That means if you want to protect multiple schemas, you need to configure multiple RLS Virtual Schemas on top of them.

### RLS Variants

RLS comes in three flavors which can also be used in combination:

1. Role-based security
2. Tenant-based security
3. Public data

The main difference between the two variants is the use case behind them. In a role-based scenario you want multiple people to be able to access the same rows &mdash; based on roles that are assigned to those users. The number of roles in this scenario is small. Tenant security on the other hand assumes that data belongs to a tenant and that other tenants are not allowed see that data.

Public data &mdash; as the name suggests &mdash; is data accessible for all users, independently of roles or whether they own the data.

### Protection Scopes of RLS

First of all, the regular protection mechanisms of Exasol apply. You can for example control who has access to the Virtual Schema that provides RLS. RLS is an additional mechanism on top of that which offers finer control for data visibility.

RLS protects individual rows inside selected tables. Tables inside an Exasol RLS Virtual Schema can either be RLS-protected or public. In a public table all users who can access the Virtual Schema can see the data inside the table.

On the lowest level RLS protects data per row (aka. dataset). To determine whether or not a user is allowed to read a row, RLS checks the existence and contents of special system columns in that table. In the following section we discuss the details.

## Administering Row Protection

In this section we will go through the administrative steps required to prepare a schema and its tables so that you can then protect it by an RLS Virtual Schema.

As an example we are going to create a small set of tables and contents in a schema called `SIMPLE_SALES`.

### Role-based security

Role-based security is a way to secure a table by assigning one or more roles to each user and specifying the roles which are allowed to see a row for each row in the table. 

#### Assigning Roles to Users

Create the following table for the role assignments as shown below inside the schema you want to protect.

```sql
CREATE OR REPLACE TABLE <schema name>.EXA_RLS_USERS  
(  
    EXA_USER_NAME VARCHAR(200),
    EXA_ROLE_MASK DECIMAL(20,0)  
);
```

Since this is a system table RLS relies on, please double-check that you spelled the table and column names correctly and used the right column types.

Now assign one or more roles to each users. User that are not listed or have no roles assigned to them in this table can only see public datasets.

```sql
INSERT INTO <schema name>.EXA_RLS_USERS VALUES ('<username>', '<bitmask as number>');
```

Example:

```sql
INSERT INTO RLS_TEST_SCHEMA.EXA_RLS_USERS VALUES 
('ALICE', NULL),
('BOB', 1),
('CLOE', 3),
('DAN', 15),
('SYS', 15);
```

The first column contains the username of the Exasol user. Make sure to spell it exactly as the original username including the correct case.

The number in the second column is a bit mask representing the role a user has. This mask is 64 bits wide. The most significant bit is reserved and can therefore not be used in role mask. This means users can be assigned to a maximum of 63 different roles or combination of roles.

#### Protecting a Table With Role-based RLS

In case you want to use Role-based security, add a column called `EXA_ROW_ROLES DECIMAL(20,0)` to all the tables you want to protect.

For our example we will create very simple order item list as shown below.

```sql
CREATE OR REPLACE TABLE SIMPLE_SALES.ORDER_ITEM 
(  
    ORDER_ID DECIMAL(18,0),  
    CUSTOMER VARCHAR(50),  
    PRODUCT VARCHAR(100),  
    QUANTITY DECIMAL(18,0),
    EXA_ROW_ROLES DECIMAL(20,0)  
);
```

**Hint:** Later versions of RLS will provide UDF functions that make administration of RLS more user-friendly.

Now we need some data to work on.

```sql
INSERT INTO SIMPLE_SALES.ORDER_ITEM VALUES
(1, 'John Smith', 'Pen', 3, 1),
(1, 'John Smith', 'Paper', 100, 3),
(1, 'John Smith', 'Eraser', 1, 7),
(2, 'Jane Doe', 'Pen', 2, 2),
(2, 'Jane Doe', 'Paper', 200, 1),
(2, 'Jane Doe', 'Paperclip', 50, POWER(2,63));
```

### Tenant-based security

Tenant-based security is a way to secure a table assigning each row to only one user. 

If you want to use tenant security, you must add an additional column `EXA_ROW_TENANT VARCHAR(128)` to the tables you want to secure. 

Example:

```sql
CREATE OR REPLACE TABLE SIMPLE_SALES.ORDER_ITEM_WITH_TENANT 
(
    ORDER_ID DECIMAL(18,0),
    CUSTOMER VARCHAR(50),
    PRODUCT VARCHAR(100),
    QUANTITY DECIMAL(18,0),
    EXA_ROW_TENANT VARCHAR(128)
);
```

For each row define which tenant it belongs to. The tenant is identical to a username in Exasol.

## Creating the Virtual Schema

We prepared the schema and tables we want to protect with RLS in section ["Administering Row Protection"](#administering-row-protection). The next step is to create the RLS Virtual Schema. That Virtual Schema is the "portal" through which regular users access an RLS-protected schema.

### Installing the RLS Virtual Schema Package

Upload the latest available release of [Row Level Security](https://github.com/exasol/row-level-security/releases) to Bucket FS.

Check out our [Virtual Schema deployment guide](https://github.com/exasol/virtual-schemas/blob/master/doc/user-guide/deploying_the_virtual_schema_adapter.md) for detailed information.

### Creating the Virtual Schema Adapter Script

Create a schema or use an existing one to hold the adapter script.

```sql
CREATE SCHEMA RLS_SCHEMA;
```

The SQL statement below creates the adapter script, defines the Java class that serves as entry point and tells the UDF framework where to find the libraries (JAR files) for Virtual Schema and database driver.

```sql
CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.RLS_VS_ADAPTER AS
    %scriptclass com.exasol.adapter.RequestDispatcher;
    %jar /buckets/<BFS service>/<bucket>/row-level-security-0.2.0-all-dependencies.jar;
/
;
```

Please remember to replace the parameters in pointy brackets by the actual values.

### Defining a Named Connection

Named connections are database objects that securely store credentials. You need to [define a `CONNECTION`](https://docs.exasol.com/sql/create_connection.htm) in order to later tell the RLS Virtual Schema how to connect to the source Exasol database.

```sql
CREATE CONNECTION EXASOL_JDBC_CONNECTION 
TO 'jdbc:exa:<host>:<port>' 
USER '<user>' 
IDENTIFIED BY '<password>';
```

### Creating a Virtual Schema with Import From Exa

```sql
CREATE VIRTUAL SCHEMA <virtual schema name> 
    USING RLS_SCHEMA.RLS_VS_ADAPTER
    WITH
    SQL_DIALECT     = 'EXASOL_RLS'
    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'
    SCHEMA_NAME     = '<schema name>'
    IMPORT_FROM_EXA = 'true'
    EXA_CONNECTION_STRING = 'localhost:<port>';
```

### Creating a Virtual Schema with Import From JDBC

```sql
CREATE VIRTUAL SCHEMA <virtual schema name> 
    USING RLS_SCHEMA.RLS_VS_ADAPTER
    WITH
    SQL_DIALECT     = 'EXASOL_RLS'
    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'
    SCHEMA_NAME     = '<schema name>';
```

### Additional optional properties

Property                    | Value
--------------------------- | -----------
**IS_LOCAL**                | Only relevant if your data source is the same Exasol database where you create the Virtual Schema. Either `TRUE` or `FALSE` (default). If `TRUE`, you are connecting to the local Exasol database (e.g. for testing purposes). In this case, the adapter can avoid the `IMPORT FROM JDBC` overhead.
**EXCLUDED_CAPABILITIES**   | A comma-separated list of capabilities that you want to deactivate (although the adapter might support them).

### Granting Access to the Virtual Schema

Remember that RLS is an additional layer of access control _on top_ of the measures built into the core database. So in order to read columns in an RLS Virtual Schema, users first need to be allowed to access that schema.

A word or warning before you start granting permissions. Make sure you grant only access to the RLS Virtual Schema to regular users and _not to the orignial_ schema. Otherwise those user can simply bypass RLS protection by going to the source.

Here is an example for allowing `SELECT` statements to a user.

```sql
GRANT SELECT ON SCHEMA <virtual schema name> TO <user>;
```

Please refer the the documentation of the [`GRANT`](https://docs.exasol.com/sql/grant.htmhttps://docs.exasol.com/sql/grant.htm) statement for further details.

The minimum requirements for a regular user in order to be able to access the RLS are:

* User must exist (`CREATE USER`)
* User is allowed to create sessions (`GRANT CREATE SESSION`)
* User can execute `SELECT` statements on the Virtual Schema (`GRANT SELECT`)

## Limitations

- RLS only works with Exasol as source and destination of the Virtual Schema.
- RLS Virtual Schema do not support JOIN capabilities.
