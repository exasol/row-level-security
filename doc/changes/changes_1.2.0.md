# Exasol Row Level Security 1.2.0, released 2020-09-30

Code name: Single group optimization

We added an optimization for group-based security in cases where the user belongs to only one group.

## Features

* #60: Fixed bug in combination of tenant-security and group-security

## Refactoring

* #60: Replaced `version.sh` by `artifact-reference-checker-maven-plugin`

## Documentation

* #59: Clarify overview of Row-Level Security variants
       (contribution from GitHub user @jwarlander)

## Dependency updates
 
* Added `com.exasol:artifact-reference-checker-maven-plugin` 0.3.1
* Updated `com.exasol:hamcrest-resultset-matcher` from 1.1.1 to 1.2.0