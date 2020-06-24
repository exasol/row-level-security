Exasol Row Level Security 1.1.0, released 2020-06-??.

## Summary

Group-based security is a new variant for protecting rows with RLS that we introduced in this release. You can define a group that a row belongs to and assign each user to one or more group. If a user is a member of the group, the row belongs to, that user can read the rows contents.

Group-based security can be used alone or in combination with tenant-based security. In this case a user must either be the tenant who owns the row or a member of the row's group.

Additionally we updated the dependencies and introduced dependency scanning.
 
## Features / Enhancements
 
* #42: Added Group based security
* #45: Ported all tests to use the [test-db-builder](https://github.com/exasol/test-db-builder-java) 
* #48: Updated dependencies, introduced dependency scan
 
## Dependency updates
 
* Added `org.sonatype.ossindex.maven` - `ossindex-maven-plugin` `3.1.0`
* Added `org.itsallcode` - `openfasttrace-maven-plugin` `0.1.0`
* Added `org.codehaus.mojo` - `build-helper-maven-plugin` `3.2.0`
* Updated `org.junit.jupiter` - `junit-jupiter-engine` from `5.6.1` to `5.6.2`
* Updated `org.junit.jupiter` - `junit-jupiter-params` from `5.6.1` to `5.6.2`
* Updated `org.junit.platform` - `junit-platform-runner` from `1.6.1` to `1.6.2`
* Updated `org.maven` - `maven-assembly-plugin` from `3.2.0` to `3.3.0`
* Updated `org.codehaus.mojo` - `exec-maven-plugin` from `1.6.0` to `3.0.0`
* Removed `com.exasol` - `virtual-schema-common-jdbc` (is transitive in `exasol-virtual-schema`)
* Removed `com.exasol` - `virtual-schema-common-java` (is transitive in `exasol-virtual-schema`)