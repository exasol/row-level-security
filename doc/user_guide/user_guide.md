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

#### Installing the Administration Scripts

RLS provides functions that make administration of RLS more user-friendly.

**Important:** all the scripts must be created in the same schema as the tables that you plan to protect with row-level-security.

To install the administration scripts, run the SQL batch file `administration-sql-scripts-<last-version>.sql`.

#### Creating Roles

Create user roles using `ADD_RLS_ROLE(role_name, role_id)` script. 

`role_name` is a unique role name. The check for an existing role is **case-insensitive** that means you can't have roles `sales` and `Sales` at the same time.
`role_id` is a unique role id. It can be in range from 1 to 63.

Examples:

```sql
EXECUTE SCRIPT ADD_RLS_ROLE('Sales', 1);
EXECUTE SCRIPT ADD_RLS_ROLE('Development', 2);
EXECUTE SCRIPT ADD_RLS_ROLE('Finance', 3);
```

#### Getting a List of Created Roles

Example:

```sql 
SELECT * FROM MY_SCHEMA.EXA_ROLES_MAPPING;
```

The script for this command will be provided in a future release.

#### Assigning Roles to Users

Assign roles to users using `ASSIGN_ROLES_TO_USER(user_name, array roles)` script.

`user_name` is a name of user created inside Exasol database.
`roles` is an array of existing roles to assign. This parameter is **case-sensitive**.

Examples:

```sql
EXECUTE SCRIPT ASSIGN_ROLES_TO_USER('RLS_USR_1', ARRAY('Sales', 'Development'));
EXECUTE SCRIPT ASSIGN_ROLES_TO_USER('RLS_USR_2', ARRAY('Development'));
```

**Important:** if you assign roles to the same user several times, the script rewrites user roles each time using a new array. That means that at any time a user has the exact set of roles stated in the _last_ assignment command.

#### Getting a List of Users With Assigned Roles

Example:

```sql 
SELECT * FROM MY_SCHEMA.EXA_RLS_USERS;
```

This script will be added in an upcoming release. 

#### Protecting a Table With Role-based RLS

In case you want to use Role-based security, add a column called `EXA_ROW_ROLES DECIMAL(20,0)` to all the tables you want to protect.

For our example we will create very simple order item list as shown below.

```sql
CREATE OR REPLACE TABLE MY_SCHEMA.ORDER_ITEM 
(  
    ORDER_ID DECIMAL(18,0),  
    CUSTOMER VARCHAR(50),  
    PRODUCT VARCHAR(100),  
    QUANTITY DECIMAL(18,0),
    EXA_ROW_ROLES DECIMAL(20,0)  
);
```

Use `ROLES_MASK`function to generate roles mask for tables or for updating the tables with the masks.

An example of generating a role mask which can be later manually inserted into the `EXA_ROW_ROLES` columns:

```sql 
SELECT MY_SCHEMA.ROLES_MASK(ROLE_ID) from MY_SCHEMA.EXA_ROLES_MAPPING WHERE ROLE_NAME IN ('Sales', 'Development')
```

**Important:** Role names are **case-sensitive**.

You can insert generated masks directly to the table:

```sql
INSERT INTO SIMPLE_SALES.ORDER_ITEM VALUES
(1, 'John Smith', 'Pen', 3, 1),
(1, 'John Smith', 'Paper', 100, 3),
(1, 'John Smith', 'Eraser', 1, 7),
(2, 'Jane Doe', 'Pen', 2, 2),
(2, 'Jane Doe', 'Paper', 200, 1);
```

An example of updating the table using `ROLES_MASK` function:

```sql 
UPDATE LOCATIONS
SET EXA_ROW_ROLES = (SELECT MY_SCHEMA.ROLES_MASK(ROLE_ID) FROM MY_SCHEMA.EXA_ROLES_MAPPING WHERE ROLE_NAME IN ('Sales', 'Development'))
WHERE customer IN ('John Smith', 'Jane Doe');
```

#### Deleting Roles

Delete roles using `DELETE_RLS_ROLE(role_name)` script. The script removes the role from all places where it is mentioned:

1. From the list of existing roles.
2. From users who have the role in the roles mask.
3. From all tables which are roles-secured.

`role_name` is a unique role name. This parameter is **case-insensitive**.

Example:

```sql 
EXECUTE SCRIPT DELETE_RLS_ROLE('Sales');
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
    %jar /buckets/<BFS service>/<bucket>/row-level-security-dist-0.2.1.jar;
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
