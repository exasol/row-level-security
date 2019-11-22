# Row and column level security

-[User guide](doc/user_guide/user_guide.md)

### Run Time Dependencies

Running the Virtual Schema requires a Java Runtime version 11 or later.

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Exasol Virtual Schema Common](https://github.com/exasol/virtual-schema-common-java)| Common module of Exasol Virtual Schemas adapters       | MIT License                   |
| [Exasol Virtual Schema JDBC](https://github.com/exasol/virtual-schema-common-jdbc)  | Common module for JDBC-based Virtual Schemas adapters  | MIT License                   |
| [Exasol JDBC Driver](https://www.exasol.com/portal/display/DOWNLOAD/Exasol+Download+Section)  | JDBC driver fro Exasol database              | MIT License                   |

### Test Dependencies

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)                                  | Checking for conditions in code via matchers           | BSD License                   |
| [JUnit](https://junit.org/junit5)                                                   | Unit testing framework                                 | Eclipse Public License 1.0    |
| [Mockito](http://site.mockito.org/)                                                 | Mocking framework                                      | MIT License                   |
| [Testcontainers](https://www.testcontainers.org/)                                   | Container-based integration tests                      | MIT License                   |
| [SLF4J](http://www.slf4j.org/)                                                      | Logging facade                                         | MIT License                   |

### Build Dependencies

| Dependency                                                                          | Purpose                                                | License                       |
|-------------------------------------------------------------------------------------|--------------------------------------------------------|-------------------------------|
| [Apache Maven](https://maven.apache.org/)                                           | Build tool                                             | Apache License 2.0            |
| [Maven Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/)    | Setting required Java version                          | Apache License 2.0            |
| [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)   | Integration testing                                    | Apache License 2.0            |
| [Maven Jacoco Plugin](https://www.eclemma.org/jacoco/trunk/doc/maven.html)          | Code coverage metering                                 | Eclipse Public License 2.0    |
| [Maven Source Plugin](https://maven.apache.org/plugins/maven-source-plugin/)        | Creating a source code JAR                             | Apache License 2.0            |
| [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)   | Unit testing                                           | Apache License 2.0            |