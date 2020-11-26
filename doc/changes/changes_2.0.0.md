# Exasol Row Level Security 2.0.0, released 2020-11-26

Code name: Fixed credentials exposure in EXA connection

## Summary

Row Level Security is based on the [`exasol-virtual-schema`](https://github.com/exasol/exasol-virtual-schema) and thus
inherited a credential exposure that has been fixed in Exasol VS 4.0.0 and is now fixed in RLS too.

If you used `IMPORT FROM EXA` in a previous version, you need to remove the old `EXA_CONNECTION_STRING` property,
create a named connection definition of type EXA with `CREATE CONNECTION` and provide the name of that definition in the
new `EXA_CONNECTION` property.

The old variant is intentionally not supported anymore to tighten security.

## Refactoring

* #6: Updated to the latest version of Exasol dialect containing a security update.

## Runtime Dependency updates
 
* Updated `com.exasol:exasol-virtual-schema:3.1.0` to `4.0.0`

## Test Dependency updates

* Added `org.jacoco:org.jacoco.agent:0.8.6`
* Updated `com.exasol:hamcrest-resultset-matcher:1.2.1` to `1.2.2`
* Updated `org.mockito:mockito-junit-jupiter:3.6.0` to `3.6.28`

## Plugin Updates

* Added `com.exasol:project-keeper-plugin:0.4.0`
* Added `org.apache.maven.plugins:maven-dependency-plugin:3.1.2`
