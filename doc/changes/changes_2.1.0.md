# Exasol Row Level Security 2.1.0, released 2021-02-11

Code name: Coverage

Release 2.1.0 of Exasol's Row-Level-Security brings a couple of new administration scripts, that make setting up and maintaining RLS-protected Virtual Schemas easier.

We also now extract the the code coverage from the tests that run inside a docker container. This coverage already existed before, but did not contribute to the static code analysis metrics. The new metrics now reflect the actual coverage situation.

## Features

* #31: Added more administration scripts for managing roles.

## Refactoring

* #63: Extracted code coverage from docker-based tests

## Documentation

* #55: Fixed the example in the tutorial
* #73: Add missing `Maven Dependency Plugin` dependency in the README.
* #82: Added explanation about the difference between database roles and RLS roles.
* #83: Moved section about installing the administration scripts before the section for administering roles.
* #84: Corrected documentation about group creation.

### Runtime Dependency Updates

* Updated `com.exasol:exasol-jdbc:7.0.3` to `7.0.4`

### Test Dependency Updates

* Added `com.exasol:udf-debugging-java:0.3.0`
* Updated `com.exasol:exasol-testcontainers:3.3.1` to `3.5.0`
* Updated `com.exasol:hamcrest-resultset-matcher:1.2.2` to `1.3.0`
* Updated `com.exasol:test-db-builder-java:2.0.0` to `3.0.0`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.5` to `3.5.4`
* Updated `org.junit.jupiter:junit-jupiter:5.7.0` to `5.7.1`
* Updated `org.mockito:mockito-junit-jupiter:3,6,28` to `3.7.7`
* Updated `org.junit.jupiter:junit-jupiter:5.7.0` to `5.7.1`
* Updated `org.mockito:mockito-junit-jupiter:1.15.0` to `1.15.1`

### Plugin Updates

* Updated `com.exasol:project-keeper-maven-plugin:0.4.0` to `0.4.2`