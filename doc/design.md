<head><link href="oft_spec.css" rel="stylesheet"></head>

# Introduction

## Acknowledgments

This document's section structure is derived from the "[arc42](https://arc42.org/)" architectural template by Dr. Gernot Starke, Dr. Peter Hruschka.

# Constraints

This section introduces technical system constraints.

## Bit-wise Functions Limited to 64 Bits
`constr~bit-wise-functions-limited-to-64-bits~1`

In Exasol bit-wise functions (like `BIT_AND` or `BIT_OR`) are limited to 64 bit.

See also: [BIT_AND](https://docs.exasol.com/sql_references/functions/alphabeticallistfunctions/bit_and.htm#BIT_AND) (Exasol documentation)

Needs: dsn

# Solution Strategy

Row-level security is not part of the Exasol core database. However with the [Virtual Schema](https://github.com/exasol/virtual-schemas/blob/master/doc/development/virtual_schema_api.md) interface lends itself nicely to what we need to implement RLS as a plug-in.

At its core Exasol Virtual Schema's are a query rewriter, not unlike views. You put in a query and it passes a modified query back that is then immediately executed by the database core instead of the original one. This is exactly what we need to implement RLS.

## Requirement Overview

Please refer to the [System Requirement Specification](system_requirements.md) for user-level requirements.

# Building Blocks

## Row Level Security SQL Dialect
`dsn~row-level-security-sql-dialect~1`

The `RowLevelSecuritySqlDialect` is a Virtual Schema SQL Dialect based on the Exasol SQL Dialect that modifies queries it receives so that they only return data the current user is allowed to see.

Comment:

Since this component implements the Virtual Schema Adapter Interface, it is also the entry point for the RLS plug-in.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl

## `QueryRewriter`
`dsn~query-rewriter~1`

The `QueryRewriter` is responsible for modifying the original query in order to inject row filters.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl

## `UserInformation`
`dsn~user-information~2`

The `UserInformation` keeps details about the user's

1. role assignments
1. group memberships

Covers:

* `req~user-roles~1`
* `req~user-groups~1`

Needs: impl


## `TableProtectionStatus`
`dsn~table-protection-status~2`

The `TableProtectionStatus` provides information about which tables are protected by which RLS variant.

Covers:

* `req~tables-with-role-restrictions~1`
* `req~tables-with-tenant-restrictions~1`
* `req~tables-with-group-restrictions~1`

Needs: impl

## `TableProtectionStatusReader`
`dsn~table-protection-status-reader~1`

The `TableProtectionStatusReader` extracts the `TableProtectionStatus` from the metadata of the RLS-protected schema.

* `req~tables-with-role-restrictions~1`
* `req~tables-with-tenant-restrictions~1`
* `req~tables-with-group-restrictions~1`

Needs: impl

# Runtime

## Row-level Data Access Protection

### `UserInformation` Reads User Roles
`dsn~user-information-reads-user-roles`

The `UserInformation` reads the current user's roles from a table with the following layout:

* `exa_user_name VARCHAR(128)` (see [https://docs.exasol.com/sql_references/basiclanguageelements.htm#SQL_Identifier]("SQL Identifier") in the Exasol documentation)
* `exa_role_mask DECIMAL(20,0)`

Covers:

* `req~user-roles~1`

Needs: impl, utest, itest

### `TableProtectionStatusReader` Identifies Protected Tables
`dsn~table-protection-status-reader-identifies-protected-tables~2`

The `QueryRewriter` identifies a table as protected with row-level security, if that table has at least on of the following columns: 

* `exa_row_roles`
* `exa_row_tenant`
* `exa_row_group`

Covers:

* `req~rows-users-are-allowed-to-read~1`
* `req~tables-with-role-restrictions~1`
* `req~tables-with-tenant-restrictions~1`
* `req~tables-with-group-restrictions~1`

Needs: impl, utest

### `TableProtectionStatusReader` Identifies Unprotected Tables
`dsn~table-protection-status-reader-identifies-unprotected-tables~2`

The `QueryRewriter` identifies a table as unprotected, if none of the conditions listed in [`dsn~table-protection-status-reader-identifies-protected-tables~2`](#tableprotectionstatusreader-identifies-protected-tables) apply.

Covers:

* `req~unprotected-tables~1`

Needs: impl, utest

### `QueryRewriter` Treats Protected Tables with Roles and Tenants Restrictions
`dsn~query-rewriter-treats-protected-tables-with-roles-and-tenant-restrictions~1`

If a table contains both `exa_row_roles` and `exa_row_tenant`columns, then the `QueryRewriter` applies both security schemes. 
That means a user has to be marked as a tenant and have the right role in due to see a row's content.

Covers:

* `req~tables-with-role-and-tenant-restrictions~1`

Needs: impl, itest

### `QueryRewriter` Adds Row Filter for Roles
`dsn~query-rewriter-adds-row-filter-for-roles~1`

The `QueryRewriter` adds a row filter to the injected sub-query that uses bitwise-`AND` against the users role mask and checks whether the result is not zero.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl, itest

## RLS Administration
 
The RLS project provides a set of convenience scripts written in Lua to make administration of RLS more user-friendly.

### Add a new role
`dsn~add-a-new-role~1`

Administrators create new roles using `ADD_RLS_ROLE` with the following parameters:

* Name of the role
* ID associated with the role (between 1 and 63)

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `ADD_RLS_ROLE` creates a table
`dsn~add-rls-role-creates-a-table~1`

`ADD_RLS_ROLE` creates a table `EXA_ROLES_MAPPING (EXA_ROLE VARCHAR(128), EXA_ROLE_ID DECIMAL(2, 0)` if it does not exist.

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `ADD_RLS_ROLES` checks parameters
`dsn~add-rls-roles-checks-parameters~1`

`ADD_RLS_ROLES` checks if all of the following criteria are met, otherwise throws an error:

1. Role ID ranges between 1 and 63
2. Role ID is unique in the role mapping
3. Role name is unique independently of its case

Covers:

* `req~user-roles~1`
* `constr~bit-wise-functions-limited-to-64-bits~1`

Needs: impl, itest

### Get a role mask
`dsn~get-a-role-mask~1`

Administrators get role masks using `ROLES_MASK` with the following parameters:

* List of role names

`ROLES_MASK` returns a decimal value.

Needs: impl, itest

### Assign roles to a user
`dsn~assign-roles-to-a-user~1`

Administrators assign roles to users using `ASSIGN_ROLES_TO_USER` with the following parameters:

* Name of the user
* List of role names

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `ASSIGN_ROLES_TO_USER` Creates a Table
`dsn~assign-roles-to-user-creates-a-table~1`

`ASSIGN_ROLES_TO_USER` creates a table `EXA_RLS_USERS (EXA_USER_NAME VARCHAR(128), EXA_ROLE_MASK DECIMAL(18,0)` if it does not exist.

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `ASSIGN_ROLES_TO_USER` Creates a Role
`dsn~assign-roles-to-user-creates-a-role~1`

`ASSIGN_ROLES_TO_USER` creates a new row with the user name and the role mask if the user name is not in the `EXA_RLS_USERS` table yet. 
Otherwise it updates `EXA_ROLE_MASK` value. 

Covers:

* `req~user-roles~1`

Needs: impl, itest

### Delete a Role
`dsn~delete-a-role~1`

Administrators delete existing roles using `DELETE_RLS_ROLE` with the following parameters:

* Name of the role

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `DELETE_RLS_ROLE` Removes a Role From Administrative Tables
`dsn~delete-rls-role-removes-a-role-from-administrative-tables~1`

`DELETE_RLS_ROLE` removes a role from `EXA_ROLES_MAPPING` and `EXA_RLS_USERS` tables if the role exists.

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `DELETE_RLS_ROLE` Removes a Role From Roles-secured Tables
`dsn~delete-rls-role-removes-a-role-from-roles-secured-tables~1`

`DELETE_RLS_ROLE` removes a deleted role from all tables that contain `EXA_ROW_ROLES` column.

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `DELETE_RLS_ROLE` Removes a Role From User Table
`dsn~delete-rls-role-removes-a-role-from-user-table~1`

`DELETE_RLS_ROLE` removes a rolw from the user table `EXA_RLS_USERS`.

Covers:

* `req~user-roles~1`

Needs: impl, itest

### Add a User to a Group

#### `ADD_USER_TO_GROUP` Adds a User to a Group
`dsn~add-user-to-group~1`

`ADD_USER_TO_GROUP` adds a user to one or more given groups.

Covers:

* `req~assigning-users-to-groups ~1`

Needs: impl, itest

#### `ADD_USER_TO_GROUP` Creates Group Member Table
`dsn~adding-user-to-group-creates-member-table~1`

`ADD_USER_TO_GROUP` creates the table `EXA_GROUP_MEMBERS (EXA_USER_NAME VARCHAR(128), EXA_GROUP VARCHAR(128))` if it does not exist.

Covers:

* `req~assigning-users-to-groups ~1`

Needs: impl, itest

#### `ADD_USER_TO_GROUP` Validates User Name
`dsn~add-user-to-group-validates-user-name~1`

`ADD_USER_TO_GROUP` validates that the user name is a valid Exasol identifier before adding the user to groups.

Covers:

* `req~assigning-users-to-groups ~1`

Needs: impl, itest

#### `ADD_USER_TO_GROUP` Validates Group Names
`dsn~add-user-to-group-validates-group-names~1`

`ADD_USER_TO_GROUP` validates that the group names are all valid Exasol identifier before adding the user to groups.

Covers:

* `req~assigning-users-to-groups ~1`

Needs: impl, itest

### Remove a User From a Group

#### `REMOVE_USER_FROM_GROUP` Removes a User From a Group
`dsn~remove-user-from-group~1`

`REMOVE_USER_FROM_GROUP` removes a user to one or more given groups.

Covers:

* `req~removing-users-from-groups~1`

Needs: impl, itest

#### `REMOVE_USER_FROM_GROUP` Validates User Name
`dsn~remove-user-from-group-validates-user-name~1`

`REMOVE_USER_FROM_GROUP` validates that the user name is a valid Exasol identifier before removing the user frp, groups.

Covers:

* `req~removing-users-from-groups~1`

Needs: impl, itest

#### `REMOVE_USER_FROM_GROUP` Validates Group Names
`dsn~remove-user-from-group-validates-group-names~1`

`REMOVE_USER_FROM_GROUP` validates that the group names are all valid Exasol identifier before removing the user from groups.

Covers:

* `req~removing-users-from-groups~1`

Needs: impl, itest

### Other Group Administration Functions

#### `LIST_ALL_GROUPS` Lists all Existing RLS Groups
`dsn~listing-all-groups~1`

`LIST_GROUPS` lists all currently existing groups including the number of members of each group.

Covers:

* `req~listing-all-groups~1`

Needs: impl, itest

#### `LIST_USER_GROUPS` Lists all Groups a User is a Member of
`dsn~listing-a-users-groups~1`

`LIST_GROUPS` lists all groups a user is a member of.

Covers:

* `req~listing-a-users-groups~1`

Needs: impl, itest

# Cross-cutting Concerns

# Design Decisions

## How do we Implement Role Checking

Users have roles. A user's roles decide which rows this user is allowed to see.

This decision is architecture-relevant because it impacts:

* Performance (query speed)
* Resource Usage (memory and storage consumption)
* Scalability

We considered the following alternatives:

1. Comma separated list of role IDs
    
    This allows a large number of roles to exist and a row or column could be assigned to as many roles as IDs fit into a 2 million character `VARCHAR`. The downsides are that especially for large IDs and big numbers of roles this costs a lot of space in row-level security scenarios and role checks need expensive string manipulation and comparison functions.

1. Comma separated list of role Names
    
    While this is human-readable, it also uses up a lot more space and comparisons get even slower.

### Roles are Represented by the Bits of a 64 Bit Integer 
`dsn~roles-are-represented-by-the-bits-of-a-64-bit-integer~1`

Each role has an ID that corresponds to a bit in a 64 bit integer. ID &in; { 2^0, 2^1, ..., 2^63 }.

Comment:

This allows using bit-wise operators in role checks which are very efficient. It also means that in a row-level security scenario the overhead per column is constant (8 byte). The biggest downsides of this decision are that only 64 roles are available in total and that this decision is very hard to revise later.

Covers:

* `req~user-roles~1`
* `constr~bit-wise-functions-limited-to-64-bits~1`

Needs: impl, utest

### Public Access Role ID 
`dsn~public-access-role-id~1`

The role with the ID 2^63 is reserved to represent public access.

Covers:

* `req~user-roles~1`

Needs: impl, itest

### All Users Have the Public Access Role
`dsn~all-users-have-the-public-access-role~1`

RLS treats all users as if the public access role was assigned to them automatically. This means there is no need for data owners to assign this role to users.

Covers:

* `req~public-rows~1`

Needs: impl, utest, itest

### Null Values in Role IDs / Masks
`dsn~null-values-in-role-ids-and-masks~1`

If the content of a role ID or role mask cell is the `NULL` value, it must be treated as if it was a zero.

Comment:

While those columns should never be `NULL` this is the safe default because it prevents user from reading data where access is unspecified.

Covers:

* `req~user-roles~1`

Needs: impl, itest

# Quality Scenarios

## Total Runtime of Secured Simple Query
`qs~total-runtime-of-secured-simple-query~1`

Given

* a database table with 5 datasets
* s<sub>o</sub> as an SQL statement where columns are select from a single table without filter or expressions
* s<sub>s</sub> as the same statement protected by RLS with a role filter

When

* s<sub>o</sub> is executed in a total runtime of t<sub>o</sub> and
* s<sub>s</sub> is executed in a total runtime of t<sub>s</sub>

Then

* t<sub>s</sub> is smaller than either t<sub>o</sub> times 1.1 or t<sub>o</sub> plus two seconds.

Covers:

* `qr~rls-protected-query-execution-time~1`

Needs: itest

# Risks