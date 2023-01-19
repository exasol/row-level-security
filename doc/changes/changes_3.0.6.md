# Exasol Row Level Security 3.0.6, released 2023-01-18

Code name: JDBC Driver from Central Repository

## Summary

In this release we removed the reference to the now decommissioned Exasol Artifactory. The driver is now taken from the Central Repository (aka. "Maven Central").

In that course we also updated the driver and other dependencies.

## Features

* #126: Switched to JDBC Driver from Central Repository

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:db-fundamentals-java:0.1.2` to `0.1.3`
* Updated `com.exasol:error-reporting-java:0.4.1` to `1.0.0`
* Updated `com.exasol:exasol-jdbc:7.1.11` to `7.1.17`
* Updated `com.exasol:exasol-virtual-schema:6.0.3` to `7.0.2`

### Test Dependency Updates

* Updated `com.exasol:exasol-testcontainers:6.1.2` to `6.5.0`
* Updated `com.exasol:hamcrest-resultset-matcher:1.5.1` to `1.5.2`
* Updated `com.exasol:test-db-builder-java:3.3.4` to `3.4.2`
* Updated `com.exasol:udf-debugging-java:0.6.4` to `0.6.6`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.10.1` to `3.12.3`
* Updated `org.junit.jupiter:junit-jupiter-api:5.9.0` to `5.9.2`
* Updated `org.junit.jupiter:junit-jupiter:5.9.0` to `5.9.2`
* Updated `org.mockito:mockito-junit-jupiter:4.6.1` to `5.0.0`
* Updated `org.slf4j:slf4j-jdk14:1.7.36` to `2.0.6`
* Updated `org.testcontainers:junit-jupiter:1.17.3` to `1.17.6`

### Plugin Dependency Updates

* Updated `com.exasol:artifact-reference-checker-maven-plugin:0.4.0` to `0.4.2`
* Updated `com.exasol:error-code-crawler-maven-plugin:1.1.2` to `1.2.1`
* Updated `com.exasol:project-keeper-maven-plugin:2.6.1` to `2.9.1`
* Updated `io.github.zlika:reproducible-build-maven-plugin:0.15` to `0.16`
* Updated `org.apache.maven.plugins:maven-assembly-plugin:3.3.0` to `3.4.2`
* Updated `org.apache.maven.plugins:maven-failsafe-plugin:3.0.0-M5` to `3.0.0-M7`
* Updated `org.apache.maven.plugins:maven-jar-plugin:3.2.2` to `3.3.0`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M5` to `3.0.0-M7`
* Updated `org.codehaus.mojo:build-helper-maven-plugin:3.2.0` to `3.3.0`
* Updated `org.codehaus.mojo:exec-maven-plugin:3.0.0` to `3.1.0`
* Updated `org.codehaus.mojo:flatten-maven-plugin:1.2.7` to `1.3.0`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.10.0` to `2.13.0`
* Updated `org.itsallcode:openfasttrace-maven-plugin:1.5.0` to `1.6.1`
* Removed `org.projectlombok:lombok-maven-plugin:1.18.20.0`
