# Exasol Row Level Security 3.0.5, released 2022-08-11

Code name: Administration SQL scripts

## Summary

This release fixes uploading of the Administration SQL script to the GitHub release.

## Features

* #122: Fixed uploading of SQL script

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:exasol-virtual-schema:6.0.2` to `6.0.3`

### Test Dependency Updates

* Updated `com.exasol:test-db-builder-java:3.3.3` to `3.3.4`
* Updated `com.exasol:udf-debugging-java:0.6.2` to `0.6.4`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.10` to `3.10.1`
* Added `org.junit.jupiter:junit-jupiter-api:5.9.0`
* Updated `org.junit.jupiter:junit-jupiter:5.8.2` to `5.9.0`
* Updated `org.testcontainers:junit-jupiter:1.17.2` to `1.17.3`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.1.1` to `1.1.2`
* Updated `com.exasol:project-keeper-maven-plugin:2.4.6` to `2.6.1`
* Updated `org.apache.maven.plugins:maven-compiler-plugin:3.8.1` to `3.10.1`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.0.0` to `3.1.0`
* Added `org.codehaus.mojo:build-helper-maven-plugin:3.2.0`
* Added `org.codehaus.mojo:exec-maven-plugin:3.0.0`
* Added `org.itsallcode:openfasttrace-maven-plugin:1.5.0`
