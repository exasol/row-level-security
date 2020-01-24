<head><link href="oft_spec.css" rel="stylesheet"></head>

# Introduction

## Acknowledgements

This document's section structure is derived from the "[arc42](https://arc42.org/)" architectural template by Dr. Gernot Starke, Dr. Peter Hruschka.

# Constraints

This section introduces technical system constraints.

## Bit-wise Functions Limited to 64 Bits
`const~bit-wise-functions-limited-to-64-bits~1`

In Exasol bit-wise functions (like `BIT_AND` or `BIT_OR`) are limited to 64 bit.

See also: [BIT_AND](https://docs.exasol.com/sql_references/functions/alphabeticallistfunctions/bit_and.htm#BIT_AND) (Exasol documentation)

Needs: dsn

# Solution Strategy

Row-level security is not part of the Exasol core database. However with the [Virtual Schema](https://github.com/exasol/virtual-schemas/blob/master/doc/development/virtual_schema_api.md) interface lends itself nicely to what we need to implement RLS as a plug-in.

At its core Exasol Virtual Schema's are a query rewriter, not unlike views. You put in a query and it passes a modified query back that is then immediately executed by the database core instead of the original one. This is exactly what we need to implement RLS.

## Requirement Overview

Please refer to the [System Requirement Specification](system_requirements.md) for user-level requirements.

# Building Blocks

## Row Level Security Adapter
`dsn~row-level-security-adapter~1`

The `RowLevelSecurityAdapter` is a Virtual Schema Adapter that modifies queries it receives so that they only return data the current user is allowed to see.

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

The `UserInformation` keeps details about the users role assignments.

Covers:

* `req~user-roles~1`


## `TableProtectionStatus`

The `TableProtectionStatus` provides information about which tables are protected by which RLS variant.

Covers:

* `req~tables-with-row-restrictions~1`
* `req~tables-with-tenants-restrictions~1`

# Runtime

## `RowLevelSecurityDialect` Reads Custom Properties
`dsn~rls-dialect-reads-custom-properties~1`

The `RowLevelSecurityDialect` reads the following custom properties:

1. `SCHEMA_NAME`: name of the schema where tables are placed

Covers:

* `req~user-roles~1`

Needs: impl, utest, itest

## `UserInformation` Reads User Roles
`dsn~user-information-reads-user-roles`

The `UserInformation` reads the current user's roles from a table with the following layout:

* `exa_user_name VARCHAR(128)` (see [https://docs.exasol.com/sql_references/basiclanguageelements.htm#SQL_Identifier]("SQL Identifier") in the Exasol documentation)
* `exa_role_mask DECIMAL(20,0)`

Covers:

* `req~user-roles~1`

Needs: impl, utest, itest

## `QueryRewriter` Determines User Roles

## `QueryRewriter` Identifies Protected Tables
`dsn~query-rewriter-identifies-protected-tables~1`

The `QueryRewriter` identifies a table as protected with row-level security, if that table has a column named `exa_row_roles` or a column named  `exa_row_tenant` or both.

Covers:

* `req~rows-users-are-allowed-to-read~1`
* `req~tables-with-tenants-restrictions~1`

Needs: impl, utest, itest

## `QueryRewriter` Identifies Unprotected Tables
`dsn~query-rewriter-identifies-unprotected-tables~1`

The `QueryRewriter` identifies a table as unprotected, if that table does not have a column named `exa_row_roles` or `exa_row_tenant`.
The `QueryRewriter` does not modify an unprotected table.

Covers:

* `req~unprotected-tables~1`

Needs: impl, utest, itest

## `QueryRewriter` Treats Protected Tables with Both Roles and Tenants Restrictions
`query-rewriter-treats-protected-tables-with-both-roles-and-tenants-restrictions~1`

If a table contains both `exa_row_roles` and `exa_row_tenant`columns, then the `QueryRewriter` applies both security schemes. 
That means a user has to be marked as a tenant and have the right role in due to see a row's content.

Covers:

* `req~tables-with-both-roles-and-tenants-restrictions~1`

Needs: impl, utest, itest

## `QueryRewriter` Replaces Tables
`dsn~query-rewriter-replaces-tables~1`

If a table is protected with row level security, the `QueryRewriter` replaces this table with sub-select that only yields columns the user is allowed to read.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl, utest, itest

## `QueryRewriter` Adds Row Filter for Roles
`dsn~query-rewriter-adds-row-filter-for-roles~1`

The `QueryRewriter` adds a row filter to the injected sub-query that uses bitwise-`AND` against the users role mask and checks whether the result is not zero.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl, utest, itest

## `TableProtectionStatus` Cache
`dsn~table-protection-status-cache`

The `TableProectionStatus` holds a cache of protected tables with the following attributes

1. Table identifier
1. Protection type: role / tenant / both

Covers:

* `qr~rls-protected-query-execution-time~1`
* `req~tables-with-row-restrictions~1`
* `req~tables-with-tenants-restrictions~1`

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

Needs: impl, utest

### Public Access Role ID 
`dsn~public-access-role-id~1`

The role with the ID 2^63 is reserved to represent public access.

Covers:

* `req~user-roles~1`

Needs: impl, utest

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

Needs: impl, utest, itest

## Administration Convenience Scripts
 
The RLS project provides scripts which make administration of RLS more user-friendly.

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

Needs: impl, itest

### Get a role mask
`dsn~get-a-role-mask~1`

Administrators get role masks using `ROLES_MASK` with the following parameters:

* List of role names

`ROLES_MASK` returns a decimal value.

### Assign roles to a user
`dsn~assign-roles-to-a-user~1`

Administrators assign roles to users using `ASSIGN_ROLES_TO_USER` with the following parameters:

* Name of the user
* List of role names

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `ASSIGN_ROLES_TO_USER` creates a table
`dsn~assign-roles-to-user-creates-a-table~1`

`ASSIGN_ROLES_TO_USER` creates a table `EXA_RLS_USERS (EXA_USER_NAME VARCHAR(128), EXA_ROLE_MASK DECIMAL(18,0)` if it does not exist.

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `ASSIGN_ROLES_TO_USER` creates a role
`dsn~assign-roles-to-user-creates-a-role~1`

`ASSIGN_ROLES_TO_USER` creates a new row with the user name and the role mask if the user name is not in the `EXA_RLS_USERS` table yet. 
Otherwise it updates `EXA_ROLE_MASK` value. 

Covers:

* `req~user-roles~1`

Needs: impl, itest

### Delete a role
`dsn~delete-a-role~1`

Administrators delete existing roles using `DELETE_RLS_ROLE` with the following parameters:

* Name of the role

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `DELETE_RLS_ROLE` removes a role from administrative tables
`dsn~delete-rls-role-removes-a-role-from-administrative-tables~1`

`DELETE_RLS_ROLE` removes a role from `EXA_ROLES_MAPPING` and `EXA_RLS_USERS` tables if the role exists.

Covers:

* `req~user-roles~1`

Needs: impl, itest

#### `DELETE_RLS_ROLE` removes a role from roles-secured tables
`dsn~delete-rls-role-removes-a-role-from-roles-secured-tables~1`

`DELETE_RLS_ROLE` removes a deleted role from all tables that contain `EXA_ROW_ROLES` column.

Covers:

* `req~user-roles~1`

Needs: impl, itest

# Quality Scenarios

# Risks