## Administration

In this section we will go through the administrative steps required to prepare a schema and its tables so that you can then protect it by an RLS Virtual Schema.

As an example we are going to create a small set of tables and contents in a schema called `SIMPLE_SALES`.

There is no need to be the database administrator (DBA) to setup an RLS-protected Virtual Schema. A DBA needs to assign a couple of privileges to you though before you can start.

To follow the steps in the examples below you need a database user account with at least the following  [system privileges](https://docs.exasol.com/db/latest/database_concepts/privileges/details_rights_management.htm?Highlight=rights#System_Privileges): `CREATE CONNECTION`, `CREATE SCHEMA`, `CREATE SESSION` (i.e. log in), `CREATE SCRIPT`, `CREATE TABLE`.

On the created objects you need the following  [object privileges](https://docs.exasol.com/db/latest/database_concepts/privileges/details_rights_management.htm?Highlight=rights#Object_Privileges) (which you automatically have if you create them yourself): `INSERT`, `SELECT`, `EXECUTE`. `UPDATE` and `DROP` are necessary if you want to be able to correct mistakes.

### Installing the Administration Scripts

RLS provides functions that make administration of RLS more user-friendly.

**Important:** all the scripts must be created in the same schema as the tables that you plan to protect with row-level-security.

To install the administration scripts, run the SQL batch file `administration-sql-scripts-<last-version>.sql`. You can find the file on the GitHub release page, the script is released together with the `.jar` file.

### Role-based security

Role-based security is a way to secure a table by assigning one or more roles to each user and specifying the roles which are allowed to see a row for each row in the table.

#### Roles


RLS supports up to 63 general-purpose roles. Those can be freely named by you.

You assign roles to users and data rows and RLS matches the assigned roles to determine if a user is allowed to access a row or not.

There is one additional reserved role that always exists. The *public role*. If you assign this role to a row in RLS, every user with access to the schema can see this row, independently of that user's own roles. So even if a user has no roles assigned at all, that user can still see all rows that have the public role set.

Assigning the public role to a user has no effect since implicitly all users have that role anyway.

### Role Masks

For performance reasons, RLS internally translates assigned roles into bit masks. Both for roles assigned to users and to rows.

The function `ROLE_MASK` returns the bit mask for an individual role ID.

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

#### Listing Roles

The following statement shows a list of existing roles and their IDs.

```sql
EXECUTE SCRIPT LIST_ALL_ROLES();
```

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

This script checks that the user name and the role names that are given are valid identifiers, to prevent SQL injection.

What it does not check is whether the user or roles exist. The reason is that this script is likely to be used in batch jobs and a check with every call would be too expensive.

If you try to assign a role that does not exist to a user, that non-existent role is ignored. That means that you could by accident assign too few roles, but never too many.

If you want to make sure check that the roles exist before calling this script.

#### Getting Users With Assigned Roles

The following statement shows a list of existing users and their roles:

```sql
EXECUTE SCRIPT LIST_USERS_AND_ROLES();
```

If you only want to see the list of all roles assigned to a single user, use the following statement:

```sql
EXECUTE SCRIPT LIST_USER_ROLES('RLS_USR_1');
```

#### Protecting a Table With Role-based RLS

In case you want to use role-based security, add a column called `EXA_ROW_ROLES DECIMAL(20,0)` to all the tables you want to protect.

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

Assigning the right role to a row requires calculating the role bit mask. Here is an example that shows how to calculate that mask from a list of role names.

```sql
SELECT SUM(DISTINCT ROLE_MASK(ROLE_ID)) FROM RLS_SOURCE.EXA_ROLES_MAPPING WHERE ROLE_NAME IN ('ACCOUNTING', 'HR', 'SALES'");
```

What that code does is creating one individual bit mask per given role and then merging them into one &mdash; simply by summing them up.

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

`NULL` values in the `EXA_ROW_ROLES` column are treated like a role mask with all roles unset, making the row effectively inaccessible.

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

`NULL` or an empty value in the `EXA_ROW_TENANT` column make the row inaccessible.

### Group-based security

If you apply group-based security, each row in a protected table can be associated with exactly one group. Users can be members of multiple groups though. This is very similar to the user group concept of a typical unix-style filesystem.

#### Creating and Deleting Groups

You don't need to explicitly create or delete a group. A group comes into existence when the first user is assigned to it and ceases to exist, when the last user is removed from it.

#### Listing Groups

The following statement shows a list of groups and the number of their members:

```sql
EXECUTE SCRIPT LIST_ALL_GROUPS();
```

If you want to list the groups user called `ALICE` is a member of, type the following:

```sql
EXECUTE SCRIPT LIST_USER_GROUPS('ALICE');
```

#### Adding a User to a Group

To add a user named `BOB` to the RLS group `COWORKERS`, run the following command:

```sql
EXECUTE SCRIPT ADD_USER_TO_GROUP('BOB', ARRAY('COWORKERS'));
```

Thanks to the array, you can also add the same user to multiple groups at the same time.

```sql
EXECUTE SCRIPT ADD_USER_TO_GROUP('BOB', ARRAY('COWORKERS', 'DEVELOPERS'));
```

#### Removing a User From a Group

To remove the user `BOB` from the RLS group `COWORKERS`) run:

```sql
EXECUTE SCRIPT REMOVE_USER_FROM_GROUP('BOB', ARRAY('COWORKERS'));
```

#### Protecting a Table With Group-based RLS

In case you want to use group-based security, add a column called `EXA_ROW_GROUP VARCHAR(128)` to all the tables you want to protect.

For our example we will create very simple order item list as shown below.

```sql
CREATE OR REPLACE TABLE MY_SCHEMA.ORDER_ITEM
(
    ORDER_ID DECIMAL(18,0),
    CUSTOMER VARCHAR(50),
    PRODUCT VARCHAR(100),
    QUANTITY DECIMAL(18,0),
    EXA_ROW_GROUP VARCHAR(128)
);
```

When inserting records into this table, provide the name of the group in the column `EXA_ROW_GROUP`.

`NULL` or blank values prohibit access to the row.

### Protection Scheme Combinations

In this section we discuss which combinations of protection schemes are supported and what their combined effects are. Combinations that are not listed are forbidden.

### Tenant- Plus Role-Security

If a table is protected with tenant- and role-security, a user must be the tenant *and* have the right role to access a row.

### Tenant- Plus Group-Security

In case you combine tenant- and group-security, a user must either be the tenant or be in the group stated in a row to access it.

## Creating the Virtual Schema

The next step is to create the RLS Virtual Schema. That Virtual Schema is the "portal" through which regular users access an RLS-protected schema.

### Installing the RLS Virtual Schema Package

Upload the latest available release of [Row Level Security](https://github.com/exasol/row-level-security/releases) to BucketFS.

Check out our [Virtual Schema user guide](https://docs.exasol.com/database_concepts/virtual_schemas.htm) for general information about adapter script installation.

### Creating the Virtual Schema Adapter Script

Create a schema or use an existing one to hold the adapter script.

```sql
CREATE SCHEMA RLS_SCHEMA;
```

The SQL statement below creates the adapter script, defines the Java class that serves as entry point and tells the UDF framework where to find the libraries (JAR files) for Virtual Schema and database driver.

```sql
CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.RLS_VS_ADAPTER AS
    %scriptclass com.exasol.adapter.RequestDispatcher;
    %jar /buckets/<BFS service>/<bucket>/row-level-security-dist-3.0.5.jar;
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

In the following sections we explain how to create the Virtual Schema that RLS is based on depending on the connection type. If you want to learn more about your options and how the differ, read ["Connection Types"](connection_types.md).

### Creating a Virtual Schema Where RLS and Protected Data are on the Same Instance or Cluster

Recommended for all RLS scenarios where RLS and the protected data share the same instance or cluster.

```sql
CREATE VIRTUAL SCHEMA <virtual schema name>
    USING RLS_SCHEMA.RLS_VS_ADAPTER
    WITH
    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'
    SCHEMA_NAME     = '<schema name>'
    IS_LOCAL = 'true'
```

### Creating a Virtual Schema with Import From Exa

Recommended when protecting data with RLS where the data is on a remote instance or cluster.

```sql
CREATE CONNECTION EXA_CONNECTION
TO '<host-or-list>:<port>'
USER '<user>'
PASSWORD '<password>'
```

```sql
CREATE VIRTUAL SCHEMA <virtual schema name>
    USING RLS_SCHEMA.RLS_VS_ADAPTER
    WITH
    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'
    SCHEMA_NAME     = '<schema name>'
    IMPORT_FROM_EXA = 'true'
    EXA_CONNECTION  = 'EXA_CONNECTION';
```

### Creating a Virtual Schema with Import From JDBC

This option is here for completeness, we recommend that you use [IMPORT FROM EXA](#creating-a-virtual-schema-with-import-from-exa) instead.

```sql
CREATE VIRTUAL SCHEMA <virtual schema name>
    USING RLS_SCHEMA.RLS_VS_ADAPTER
    WITH
    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'
    SCHEMA_NAME     = '<schema name>';
```


### Additional optional properties

Property                  | Value
------------------------- | -----------
`EXCLUDED_CAPABILITIES`   | A comma-separated list of capabilities that you want to deactivate (although the adapter might support them).

### Granting Access to the Virtual Schema

Remember that RLS is an additional layer of access control _on top_ of the measures built into the core database. So in order to read columns in an RLS Virtual Schema, users first need to be allowed to access that schema.

A word or warning before you start granting permissions. Make sure you grant only access to the RLS Virtual Schema to regular users and _not to the orignial_ schema. Otherwise those user can simply bypass RLS protection by going to the source.

Here is an example for allowing `SELECT` statements to a user.

```sql
GRANT SELECT ON SCHEMA <virtual schema name> TO <user>;
```

Please refer the the documentation of the [`GRANT`](https://docs.exasol.com/sql/grant.htm) statement for further details.

The minimum requirements for a regular user in order to be able to access the RLS are:

* User must exist (`CREATE USER`)
* User is allowed to create sessions (`GRANT CREATE SESSION`)
* User can execute `SELECT` statements on the Virtual Schema (`GRANT SELECT`)
