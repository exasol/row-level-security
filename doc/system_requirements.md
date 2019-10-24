<head><link href="oft_spec.css" rel="stylesheet"></link></head>

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

1. Provide reliable security
1. Work with an affordable performance hit compared to accessing the data without RCLS

## Stakeholders

### Data Owners

TODO

### Regular Users

TODO

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

### Common Requirements

In this section you find requirements that are common for both row- and column-level security. 

#### User Roles
`req~user-roles~1`

Data Owners can assign zero or more roles to users.

Rationale:

Roles are used to determine whether a user may or may not access data in RLS.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

### Row Level Security

#### Tables With Row Restrictions
`req~tables-with-row-restrictions~1`

Data Owners can define for each table whether or not access to individual rows is restricted.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Granting Row Read Access Based on User Role
`req~granting-row-read-access-based-on-user-role~1`

Data Owners can define the role a user must have in order to read a row.

Needs: dsn

#### Public Rows
`req~public-rows~1`

Data Owners can define rows that all users can read, regardless of those users' roles.

Rationale:

This allows data owners to make non-confidential data public in a table otherwise restricted by row-level security.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Rows Users are Allowed to Read
`req~rows-users-are-allowed-to-read~1`

A user can read a row if at least one of the roles assigned to the user is also assigned to that row.

Rationale:

We want rows to be potentially readable by multiple roles. If one of the user roles matches that is sufficient. Having multiple roles match means overachieving the criteria. In a role-based security model it also makes no sense to require someone having more than one role to access a row. 

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn

#### Only Accessible Rows Contribute to Aggregate Functions
`req~only-accessible-rows-contribute-to-aggregate-functions~1`

If row access is controlled by RLS and a user invokes an aggregate function, then only the rows accessible to that user contributed to the result of the aggregate function.

Rationale:

This prevents attackers from gaining information about restricted columns through the side channel of aggregate functions.

Covers:

* [feat~row-level-security~1](#row-level-security)

Needs: dsn
