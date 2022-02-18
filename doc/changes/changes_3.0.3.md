# Exasol Row Level Security 3.0.3, released 2022-02-??

Code name: Split an improved user guide

## Summary

Release 3.0.3 brings an improved user guide that is split into separate pages. We also reworked the explanations to be clearer and more straight forward.

We removed the tests for Exasol 6.2 since that version is now discontinued. While RLS might still work with that version, we recommend switching to a newer, still supported version.

We also updated dependencies to the latest versions.

## Documentation

* #80: Improved user guide

## Refactoring

* #115: Updated dependencies and removed matrix build of 6.2

## Dependency Updates

### Compile Dependency Updates

* Updated `com.exasol:error-reporting-java:0.4.0` to `0.4.1`
* Updated `com.exasol:exasol-jdbc:7.1.2` to `7.1.4`
* Updated `com.exasol:exasol-virtual-schema:5.0.5` to `6.0.2`

### Test Dependency Updates

* Updated `com.exasol:exasol-testcontainers:5.1.1` to `6.0.0`
* Updated `com.exasol:test-db-builder-java:3.2.1` to `3.3.0`
* Updated `nl.jqno.equalsverifier:equalsverifier:3.7.2` to `3.9`
* Updated `org.junit.jupiter:junit-jupiter:5.8.1` to `5.8.2`
* Updated `org.mockito:mockito-junit-jupiter:4.0.0` to `4.3.1`
* Updated `org.slf4j:slf4j-jdk14:1.7.32` to `1.7.36`
* Updated `org.testcontainers:junit-jupiter:1.16.2` to `1.16.3`

### Plugin Dependency Updates

* Updated `com.exasol:error-code-crawler-maven-plugin:0.6.0` to `0.7.1`
* Updated `com.exasol:project-keeper-maven-plugin:1.2.0` to `1.3.4`
* Updated `io.github.zlika:reproducible-build-maven-plugin:0.13` to `0.14`
