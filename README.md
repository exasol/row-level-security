# Row Level Security

<img alt="row-level-security logo" src="doc/images/row-level-security_128x128.png" style="float:left; padding:0px 10px 10px 10px;"/>

[![Build Status](https://api.travis-ci.com/exasol/row-level-security.svg?branch=master)](https://travis-ci.org/exasol/row-level-security)

SonarCloud results:

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)

[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=security_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=sqale_index)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)

[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=code_smells)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=coverage)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=com.exasol%3Arow-level-security&metric=ncloc)](https://sonarcloud.io/dashboard?id=com.exasol%3Arow-level-security)

Protect access to database tables on a per-row level based on roles and / or tenants. 

## Features

Restrict access to rows (datasets) in tables to &hellip;

* set of roles
* tenants (owners)
* group of users
* combination of group and tenant
* combination of group and role

## Table of Contents

### Information for Users

* [User Guide](doc/user_guide/user_guide.md)
* [Tutorial](doc/user_guide/tutorial.md)

### Information for Contributors

Requirement, design documents and coverage tags are written in [OpenFastTrace](https://github.com/itsallcode/openfasttrace) format.

* [System Requirement Specification](doc/system_requirements.md)
* [Design](doc/design.md)

### Run Time Dependencies

Running the Virtual Schema requires a Java Runtime version 11 or later.

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Exasol DB Fundamentals](https://github.com/exasol/db-fundamentals-java)            | Basic database building blocks                         | MIT License                   |
| [Exasol Virtual Schema](https://github.com/exasol/exasol-virtual-schema)            | Virtual Schemas adapter for Exasol sources             | MIT License                   |
| [Exasol Virtual Schema JDBC](https://github.com/exasol/virtual-schema-common-jdbc)  | Common module for JDBC-based Virtual Schemas adapters  | MIT License                   |
| [Exasol JDBC Driver](https://www.exasol.com/portal/display/DOWNLOAD/Exasol+Download+Section)  | JDBC driver for Exasol database              | MIT License                   |

### Test Dependencies

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Exasol Testcontainers](https://github.com/exasol/exasol-testcontainers)            | Integration test Exasol instance on Docker             | MIT License                   |
| [Hamcrest Resultset Matcher](https://github.com/exasol/hamcrest-resultset-matcher)  | Validating JDBC resultsets                             | MIT License                   |
| [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)                                  | Checking for conditions in code via matchers           | BSD License                   |
| [JUnit](https://junit.org/junit5)                                                   | Unit testing framework                                 | Eclipse Public License 1.0    |
| [Mockito](http://site.mockito.org/)                                                 | Mocking framework                                      | MIT License                   |
| [Test Database Builder](https://github.com/exasol/test-db-builder-java)             | Framework for writing database integration tests       | MIT License                   |
| [Testcontainers](https://www.testcontainers.org/)                                   | Container-based integration tests                      | MIT License                   |
| [SLF4J](http://www.slf4j.org/)                                                      | Logging facade                                         | MIT License                   |

### Build Dependencies

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Apache Maven](https://maven.apache.org/)                                           | Build tool                                             | Apache License 2.0            |
| [Build Helper Maven Plugin](http://www.mojohaus.org/build-helper-maven-plugin/)     | Register non-standard source directories (here Lua)    | MIT License                   |
| [Exec Maven Plugin](https://www.mojohaus.org/exec-maven-plugin/)                    | Execute external processes                             | Apache License 2.0            |
| [Maven Assembly Plugin](https://maven.apache.org/plugins/maven-assembly-plugin/)    | Building JAR archives                                  | Apache License 2.0            |
| [Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/)    | Setting required Java version                          | Apache License 2.0            |
| [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)   | Integration testing                                    | Apache License 2.0            |
| [Maven Jacoco Plugin](https://www.eclemma.org/jacoco/trunk/doc/maven.html)          | Code coverage metering                                 | Eclipse Public License 2.0    |
| [Maven Source Plugin](https://maven.apache.org/plugins/maven-source-plugin/)        | Creating a source code JAR                             | Apache License 2.0            |
| [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)   | Unit testing                                           | Apache License 2.0            |
| [OpenFastTrace Maven Plugin](https://github.com/itsallcode/openfasttrace-maven-plugin) |Requirement Tracing                                  | GPL V3                        |
| [OSS Index Maven Plugin](https://sonatype.github.io/ossindex-maven/maven-plugin/)   | Dependency security monitoring                         | Apache License 2.0            |
