# Exasol Row Level Security 1.2.1, released 2020-11-17

Code name: Security Update

## Summary

Classification: High

Please update your adapters as soon as possible!

This release fixes several SQL injection vulnerabilities on the remote database of the virtual schema. 
The local Exasol database defining the virtual schema is not affected.

## Refactoring

* #67: Updated to the latest version of Exasol dialect containing a security update.

## Dependency updates
 
 * Added org.junit.jupiter:junit-jupiter:5.7.0
 * Updated com.exasol:exasol-jdbc:6.2.5 to version 7.0.3
 * Updated com.exasol:hamcrest-resultset-matcher:1.2.0 to version 1.2.1
 * Updated org.mockito:mockito-junit-jupiter:3.3.3 to version 3.6.0
 * Updated org.testcontainers:junit-jupiter:1.14.3 to version 1.15.0
 * Updated com.exasol:exasol-virtual-schema:3.0.2 to version 3.1.0
 * Updated com.exasol:exasol-testcontainers:2.0.3 to version 3.3.1
 * Updated com.exasol:test-db-builder-java:1.0.1 to version 2.0.0
 * Updated nl.jqno.equalsverifier:equalsverifier:3.4.1 to version 3.5
 * Updated org.codehaus.mojo:versions-maven-plugin:2.7 to version 2.8.1
 * Updated org.jacoco:jacoco-maven-plugin:0.8.5 to version 0.8.6
 * Removed org.junit.jupiter:junit-jupiter-engine
 * Removed org.junit.jupiter:junit-jupiter-params
 * Removed org.junit.platform:junit-platform-runner