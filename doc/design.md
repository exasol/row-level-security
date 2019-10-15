<head><link href="oft_spec.css" rel="stylesheet"></link></head>

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

Row-level security is not part of the Exasol core database. However with the [Virtual Schema](https://github.com/exasol/virtual-schemas) interface lends itself nicely to what we need to implement RLS as a plug-in.

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

# Runtime

## `RowLevelSecurityAdapter` Reads Custom Properties
`dsn~rls-adapter-reads-custom-properties~1`

The `RowLevelSecurityAdapter` reads the following custom properties:

1. `ROLE_ASSIGNMENT_TABLE`: name of the table that contains the mapping of users to the roles the have

Covers:

* `req~user-roles~1`

Needs: impl, utest, itest

## `UserInformation` Read User Roles
`dsn~user-information-reads-user-roles`

The `UserInformation` reads the current user's roles from a table with the following layout:

* `exa_user_name VARCHAR(128)` (see [https://docs.exasol.com/sql_references/basiclanguageelements.htm#SQL_Identifier]("SQL Identifier") in the Exasol documentation)
* `exa_role_mask DECIMAL(20,0)`

Covers:

* `req~user-roles~1`

Needs: impl, utest, itest

## `UserInformation` Caches User Roles
`dsn~user-information-caches-user-roles`

The `UserInformation` caches the roles a user has.

Rationale:

This speeds up subsequent row access checks because the users roles need to be determined only once.

Covers:

* `req~user-roles~1`

Needs: impl, utest

## Query Rewriter Determines User Roles

## Query Rewriter Identifies Protected Tables
`dsn~query-rewriter-identifies-protected-tables~1`

The Query Rewriter identifies a table as protected with row-level security, if that table has a column named `exa_row_roles`.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl, utest, itest

## Query Rewriter Replaces Tables
`dsn~query-rewriter-replaces-tables~1`

If a table is protected with row level security, the Query Rewriter replaces this table with sub-select that only yields columns the user is allowed to read.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl, utest, itest

## Query Rewriter Adds Row Filter
`dsn~query-rewriter-adds-row-filter~1`

The Query Rewriter adds a row filter to the injected sub-query that uses bitwise-`AND` against the users role mask and checks whether the result is not zero.

Covers:

* `req~rows-users-are-allowed-to-read~1`

Needs: impl, utest, itest

# Cross-cutting Concerns

# Design Decisions

## How do we Implement Role Checking

Users have roles. A user's roles decide which rows / columns this user is allowed to see.

This decision is architecture-relevant because it impacts:

* Performance (query speed)
* Resource Usage (memory and storage consumption)
* Scaleability

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

RCLS treats all users as if the public access role was assigned to them automatically. This means there is no need for data owners to assign this role to users.

Covers:

* `req~public-rows~1`

Needs: impl, utest, itest

### Null Values in Role IDs / Masks
`dsn~null-values-in-role-ids-and-masks~1`

If the content of a role ID or role mask cell is the `NULL` value, it must be treated as if it were a zero.

Comment:

While those columns should never be `NULL` this is the safe default because it prevents user from reading data where access is unspecified.

Covers:

* `req~user-roles~1`

Needs: impl, utest, itest


# Quality Scenarios

# Risks