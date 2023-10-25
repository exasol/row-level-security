# Exasol Row Level Security 3.0.7, released 2023-10-25

Code name: Dependency Upgrade

## Summary

This release fixes vulnerability CVE-2023-42503 in transitive test dependency to `org.apache.commons:commons-compress` via `exasol-testcontainers` by updating dependencies.

## Security

* #128: Fixed vulnerability CVE-2023-42503 in test dependency `org.apache.commons:commons-compress`

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:1.0.0` to `1.0.1`
* Updated `com.exasol:exasol-jdbc:7.1.17` to `7.1.20`
* Updated `com.exasol:exasol-virtual-schema:7.0.2` to `7.1.5`

### Test Dependency Updates

* Updated `com.exasol:exasol-testcontainers:6.5.0` to `6.6.2`
* Updated `com.exasol:hamcrest-resultset-matcher:1.5.2` to `1.6.1`
* Updated `com.exasol:test-db-builder-java:3.4.2` to `3.5.1`
* Updated `com.exasol:udf-debugging-java:0.6.6` to `0.6.11`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.12.3` to `3.15.2`
* Updated `org.jacoco:org.jacoco.agent:0.8.8` to `0.8.11`
* Updated `org.junit.jupiter:junit-jupiter-api:5.9.2` to `5.10.0`
* Updated `org.junit.jupiter:junit-jupiter:5.9.2` to `5.10.0`
* Updated `org.mockito:mockito-junit-jupiter:5.0.0` to `5.6.0`
* Updated `org.slf4j:slf4j-jdk14:2.0.6` to `2.0.9`
* Updated `org.testcontainers:junit-jupiter:1.17.6` to `1.19.1`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:1.2.1` to `1.3.1`
* Updated `com.exasol:project-keeper-maven-plugin:2.9.1` to `2.9.14`
* Updated `org.apache.maven.plugins:maven-assembly-plugin:3.4.2` to `3.6.0`
* Updated `org.apache.maven.plugins:maven-compiler-plugin:3.10.1` to `3.11.0`
* Updated `org.apache.maven.plugins:maven-dependency-plugin:3.3.0` to `3.6.0`
* Updated `org.apache.maven.plugins:maven-enforcer-plugin:3.1.0` to `3.4.1`
* Updated `org.apache.maven.plugins:maven-failsafe-plugin:3.0.0-M7` to `3.1.2`
* Updated `org.apache.maven.plugins:maven-surefire-plugin:3.0.0-M7` to `3.1.2`
* Added `org.basepom.maven:duplicate-finder-maven-plugin:2.0.1`
* Updated `org.codehaus.mojo:flatten-maven-plugin:1.3.0` to `1.5.0`
* Updated `org.codehaus.mojo:versions-maven-plugin:2.13.0` to `2.16.1`
* Updated `org.jacoco:jacoco-maven-plugin:0.8.8` to `0.8.11`
* Updated `org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184` to `3.10.0.2594`
