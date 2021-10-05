# Exasol Row Level Security 3.0.1, released 2021-10-05

Code name: Support TLS connections

## Summary

This release updates the JDBC driver and thus supports TLS encrypted connections.

## Bug Fixes

* #86: Added error for users without group.
* #87: Added error for role + group on the same table.
* #107: Added support for TLS.

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:exasol-jdbc:7.0.11` to `7.1.1`
* Updated `com.exasol:exasol-virtual-schema:5.0.3` to `5.0.4`

### Runtime Dependency Updates

* Removed `org.jacoco:org.jacoco.agent:0.8.7`

### Test Dependency Updates

* Updated `com.exasol:exasol-testcontainers:4.0.0` to `5.1.0`
* Updated `com.exasol:hamcrest-resultset-matcher:1.4.1` to `1.5.0`
* Updated `com.exasol:test-db-builder-java:3.2.0` to `3.2.1`
* Updated `com.exasol:udf-debugging-java:0.4.0` to `0.4.1`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.7` to `3.7.1`
* Added `org.jacoco:org.jacoco.agent:0.8.7`
* Updated `org.mockito:mockito-junit-jupiter:3.11.2` to `3.12.4`

### Plugin Dependency Updates

* Updated `com.exasol:artifact-reference-checker-maven-plugin:0.3.1` to `0.4.0`
* Updated `com.exasol:error-code-crawler-maven-plugin:0.5.1` to `0.6.0`
* Updated `com.exasol:project-keeper-maven-plugin:0.10.0` to `1.2.0`
* Updated `org.apache.maven.plugins:maven-dependency-plugin:3.1.2` to `3.2.0`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.0.0-M3` to `3.0.0`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.6` to `0.8.7`
