# Exasol Row Level Security 3.0.0, released 2021-08-11

Code name: Removed `SQL_DIALECT` property

## Summary

The SQL_DIALECT property used when executing a CREATE VIRTUAL SCHEMA from the Exasol database is obsolete from this version. Please, do not provide this property anymore.

In this release we updated the dependencies. By that we fixed transitive CVE-2021-36090.

## Refactoring

* #103: Added error codes.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:db-fundamentals-java:0.1.1` to `0.1.2`
* Added `com.exasol:error-reporting-java:0.4.0`
* Updated `com.exasol:exasol-jdbc:7.0.7` to `7.0.11`
* Updated `com.exasol:exasol-virtual-schema:4.0.0` to `5.0.3`

### Runtime Dependency Updates

* Updated `org.jacoco:org.jacoco.agent:0.8.6` to `0.8.7`

### Test Dependency Updates

* Updated `com.exasol:exasol-testcontainers:3.5.1` to `4.0.0`
* Updated `com.exasol:hamcrest-resultset-matcher:1.4.0` to `1.4.1`
* Updated `com.exasol:test-db-builder-java:3.1.0` to `3.2.0`
* Updated `com.exasol:udf-debugging-java:0.3.0` to `0.4.0`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.5.4` to `3.7`
* Updated `org.junit.jupiter:junit-jupiter:5.7.1` to `5.7.2`
* Updated `org.mockito:mockito-junit-jupiter:3.8.0` to `3.11.2`
* Updated `org.slf4j:slf4j-jdk14:1.7.30` to `1.7.32`
* Updated `org.testcontainers:junit-jupiter:1.15.2` to `1.16.0`

### Plugin Dependency Updates

* Added `com.exasol:error-code-crawler-maven-plugin:0.5.1`
* Updated `com.exasol:project-keeper-maven-plugin:0.4.2` to `0.10.0`
* Added `io.github.zlika:reproducible-build-maven-plugin:0.13`
* Updated `org.apache.maven.plugins:maven-jar-plugin:2.4` to `3.2.0`