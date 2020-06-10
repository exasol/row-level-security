<head><link href="oft_spec.css" rel="stylesheet"></head>

# System Requirement Specification Exasol Row Level Security

## Introduction

Exasol Row Level Security (short "RLS") is a plug-in for Exasol that provides the ability to grant access on individual rows based on contents of the database.

## About This Document

### Target Audience

The target audience are end-users, requirement engineers, software designers and quality assurance. See section ["Stakeholders"](#stakeholders) for more details.

### Goal

The RLS main goal is to provide fine-grained access control below the level of tables.

### Quality Goals

RLS's main quality goals are in descending order of importance:

#### Reliable Security
`qg~reliable-security~1`

RLS reliably secures the access to protected data.

<!-- Needs: qr -->

#### Affordable Performance Hit
`qg~affordable-performance-hit~1`

The Performance degradation caused by and RLS-protected query compared to the same query without RLS is small.

Needs: qr

## Stakeholders

### Data Owners

Data Owners are have full access to the data _before_ row-level security is applied. They also get to decide who is allowed to read it through RLS.

### Regular Users

Regular Users in the context of this project are consumers of row-level security-protected data.

### Terms and Abbreviations

The following list gives you an overview of terms and abbreviations commonly used in OFT documents.

* Column: An Attribute of a defined type shared by all datasets in a table.
* Row: Dataset in the database

In the following subsections central terms are explained in more detail.

## Features

Features are the highest level requirements in this document that describe the main functionality of RLS.

### Row Level Security
`feat~row-level-security~1`

RLS lets administrators grant access to individual table rows based on configurable criteria.

Needs: req

## Functional Requirements

### Row Level Security with Roles

#### User Roles
`req~user-roles~1`

Data Owners can assign between zero and 63 roles to users.

Rationale:

Roles are used to determine whether a user may or may not access data in RLS.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Tables With Role Restrictions
`req~tables-with-role-restrictions~1`

Data Owners can define a set of roles from which a user must have at least one in order to read a row.

Needs: dsn

#### Rows Users are Allowed to Read
`req~rows-users-are-allowed-to-read~1`

A user can read a row if at least one of the roles assigned to the user is also assigned to that row.

Rationale:

We want rows to be potentially readable by multiple roles. If one of the user roles matches that is sufficient. Having multiple roles match means overachieving the criteria. In a role-based security model it also makes no sense to require someone having more than one role to access a row. 

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

### Row Level Security with Tenants 

#### Tables With Tenant Restrictions
`req~tables-with-tenant-restrictions~1`

Data Owners can define for each row in the table if it belongs to only one user (tenant).

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

### Row Level Security with Groups

"Groups" are a functionality that allows shared access to rows based on a user's membership in groups.

#### Assigning Users to Groups
`req~assigning-users-to-groups ~1`

Data Owners can assign zero or more users to a named group.

Rationale:

A group is a set of people sharing access rights on group-protected rows.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Removing Users From Groups
`req~removing-users-from-groups~1`

Data Owners remove users for one or more groups they are currently a member of.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Tables With Group Restrictions
`req~tables-with-group-restrictions~1`

Data Owners can define a single group for each row in the table, so that this group is allowed to read the row.

Rationale:

This allows an arbitrary number of users to share read from the same table.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Listing Groups
`req~listing-groups~1`

Data owners can list all existing groups.

Rationale:

This allows data owner to see if a group already exists.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

### Protection Scheme Combinations

Protection schemes like role-based, tenant-based or group-based security can be used in valid combinations. This section explains what combinations exist and which effect they have.

Combinations not listed in this section are not supported.

#### Tables with Role and Tenant Restrictions
`req~tables-with-role-and-tenant-restrictions~1`

If a table contains role restrictions and tenant restrictions, both of them are applied.
To access the data a user needs: 
    1. To be the right tenant.
    2. To be assigned to at least one of the roles that the data owner granted access to.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Tables with Group and Tenant Restrictions
`req~tables-with-group-and-tenant-restrictions`

If a table contains group restrictions and tenant restrictions, the user needs to fulfill at least one of the two access criteria.
To access the data a user needs: 
    * To be the right tenant _or_
    * To be member of the group the row belongs to

Obviously this means access is also granted if the user is both tenant and member of the group at the same time.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

### Public Access

Even in an RLS-protected Virtual Schema there are cases where users need publicly accessible data. Public access come in two variants, where either a whole table or view is publicly readable or single rows are.

#### Public Rows
`req~public-rows~1`

Data Owners can define rows that all users can read, regardless of those users' roles.

Rationale:

This allows data owners to make non-confidential data public in a table otherwise restricted by row-level security.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Unprotected tables
`req~unprotected-tables~1`

Data Owners can leave a table unprotected. In this case all users can access all data in the table.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

## Quality Requirements

### Quality Tree

    Utility
      |
      |-- Performance
      |-- Modifiability
      '-- Security

### Quality Scenarios

#### Performance

##### RLS-protected Query Execution Time
`qr~rls-protected-query-execution-time~1`

The Performance degradation caused by an RLS-protected query compared to the same query without RLS is below the greater of

* two seconds or
* 10%

on top of the original execution time.

Comment:

This is the complete runtime as the database client experiences it including the involved upstart times for the UDF language container and the contained runtime environment.

Covers:

* [qg~affordable-performance-hit~1](#affordable-performance-hit)

Needs: qs
