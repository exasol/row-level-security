# Exasol Row Level Security 2.1.0, released 2021-02-25

Code name: Role-security administration scripts security update

Release 2.1.0 of Exasol's Row-Level-Security contains a fix for a security issue, classification "medium" in an administration script. The issue can lead to giving unintended row access to users.

You are likely affected if:

* you are using role-based row protection and
* modified a role via an administration script

What can you do to resolve the situation:

* Validate all role masks (column "EXA_ROW_ROLES") on all role-protected tables
* Set them again if the roles on that row don't match

Other changes:

* A couple of new administration scripts, that make setting up and maintaining RLS-protected Virtual Schemas easier.
* `ASSIGN_ROLES_TO_USER` now ignores non-existenent roles in oder to be efficient in batch updates.
* All columns in `EXA_ROLES_MAPPING` and `EXA_RLS_USERS` are now generated with `NOT NULL` constraints.
* `ROLE_ID` is now a primary key on `EXA_RLS_USERS` to enforce uniqueness.
* Identifiers (user names, group names and role names) are checked much stricter in administration scripts now.
* We now extract the the code coverage from the tests that run inside a docker container. This coverage already existed before, but did not contribute to the static code analysis metrics. The new metrics now reflect the actual coverage situation.

## Features

* #31: Added more administration scripts for managing roles.

## Refactoring

* #63: Extracted code coverage from docker-based tests

## Bugfixes

* #95: Fixed vulnerability in administration script.

## Documentation

* #55: Fixed the example in the tutorial
* #73: Add missing `Maven Dependency Plugin` dependency in the README.
* #79: Improved documentation of public role.
* #81: Explained the effect of `NULL` or empty value in tenant or role column.
* #82: Added explanation about the difference between database roles and RLS roles.
* #83: Moved section about installing the administration scripts before the section for administering roles.
* #84: Corrected documentation about group creation.

### Runtime Dependency Updates

* Updated `com.exasol:exasol-jdbc:7.0.3` to `7.0.7`

### Test Dependency Updates

* Added `com.exasol:udf-debugging-java:0.3.0`
* Updated `com.exasol:exasol-testcontainers:3.3.1` to `3.5.1`
* Updated `com.exasol:hamcrest-resultset-matcher:1.2.2` to `1.4.0`
* Updated `com.exasol:test-db-builder-java:2.0.0` to `3.1.0`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.5` to `3.5.4`
* Updated `org.junit.jupiter:junit-jupiter:5.7.0` to `5.7.1`
* Updated `org.mockito:mockito-junit-jupiter:3,6,28` to `3.8.0`
* Updated `org.junit.jupiter:junit-jupiter:5.7.0` to `5.7.1`
* Updated `org.mockito:mockito-junit-jupiter:1.15.0` to `1.15.2`

### Plugin Updates

* Updated `com.exasol:project-keeper-maven-plugin:0.4.0` to `0.4.2`
