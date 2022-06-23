<!-- @formatter:off -->
# Dependencies

## Compile Dependencies

| Dependency                                 | License                |
| ------------------------------------------ | ---------------------- |
| [Exasol Database fundamentals for Java][0] | [MIT][1]               |
| [Exasol Virtual Schema][2]                 | [MIT][1]               |
| [EXASolution JDBC Driver][4]               | [EXAClient License][5] |
| [error-reporting-java][6]                  | [MIT][1]               |
| [Project Lombok][8]                        | [The MIT License][9]   |

## Test Dependencies

| Dependency                                      | License                           |
| ----------------------------------------------- | --------------------------------- |
| [Test containers for Exasol on Docker][10]      | [MIT][1]                          |
| [Testcontainers :: JUnit Jupiter Extension][12] | [MIT][13]                         |
| [Hamcrest][14]                                  | [BSD License 3][15]               |
| [Matcher for SQL Result Sets][16]               | [MIT][1]                          |
| [JUnit Jupiter (Aggregator)][18]                | [Eclipse Public License v2.0][19] |
| [mockito-junit-jupiter][20]                     | [The MIT License][21]             |
| [SLF4J JDK14 Binding][22]                       | [MIT License][23]                 |
| [Test Database Builder for Java][24]            | [MIT License][25]                 |
| [EqualsVerifier | release normal jar][26]       | [Apache License, Version 2.0][27] |
| [JaCoCo :: Agent][28]                           | [Eclipse Public License 2.0][29]  |
| [udf-debugging-java][30]                        | [MIT][1]                          |

## Plugin Dependencies

| Dependency                                              | License                                        |
| ------------------------------------------------------- | ---------------------------------------------- |
| [SonarQube Scanner for Maven][32]                       | [GNU LGPL 3][33]                               |
| [Project keeper maven plugin][34]                       | [The MIT License][35]                          |
| [Apache Maven Compiler Plugin][36]                      | [Apache License, Version 2.0][27]              |
| [Apache Maven Enforcer Plugin][38]                      | [Apache License, Version 2.0][27]              |
| [Maven Flatten Plugin][40]                              | [Apache Software Licenese][41]                 |
| [org.sonatype.ossindex.maven:ossindex-maven-plugin][42] | [ASL2][41]                                     |
| [Reproducible Build Maven Plugin][44]                   | [Apache 2.0][41]                               |
| [Maven Surefire Plugin][46]                             | [Apache License, Version 2.0][27]              |
| [Versions Maven Plugin][48]                             | [Apache License, Version 2.0][27]              |
| [Apache Maven Assembly Plugin][50]                      | [Apache License, Version 2.0][27]              |
| [Apache Maven JAR Plugin][52]                           | [Apache License, Version 2.0][27]              |
| [Artifact reference checker and unifier][54]            | [MIT][1]                                       |
| [Apache Maven Dependency Plugin][56]                    | [Apache License, Version 2.0][27]              |
| [Lombok Maven Plugin][58]                               | [The MIT License][1]                           |
| [Maven Failsafe Plugin][60]                             | [Apache License, Version 2.0][27]              |
| [JaCoCo :: Maven Plugin][62]                            | [Eclipse Public License 2.0][29]               |
| [error-code-crawler-maven-plugin][64]                   | [MIT][1]                                       |
| [Maven Clean Plugin][66]                                | [The Apache Software License, Version 2.0][41] |
| [Maven Resources Plugin][68]                            | [The Apache Software License, Version 2.0][41] |
| [Maven Install Plugin][70]                              | [The Apache Software License, Version 2.0][41] |
| [Maven Deploy Plugin][72]                               | [The Apache Software License, Version 2.0][41] |
| [Maven Site Plugin 3][74]                               | [The Apache Software License, Version 2.0][41] |

[28]: https://www.eclemma.org/jacoco/index.html
[6]: https://github.com/exasol/error-reporting-java
[41]: http://www.apache.org/licenses/LICENSE-2.0.txt
[8]: https://projectlombok.org
[46]: https://maven.apache.org/surefire/maven-surefire-plugin/
[66]: http://maven.apache.org/plugins/maven-clean-plugin/
[1]: https://opensource.org/licenses/MIT
[20]: https://github.com/mockito/mockito
[40]: https://www.mojohaus.org/flatten-maven-plugin/
[34]: https://github.com/exasol/project-keeper/
[48]: http://www.mojohaus.org/versions-maven-plugin/
[15]: http://opensource.org/licenses/BSD-3-Clause
[36]: https://maven.apache.org/plugins/maven-compiler-plugin/
[25]: https://github.com/exasol/test-db-builder-java/blob/main/LICENSE
[29]: https://www.eclipse.org/legal/epl-2.0/
[33]: http://www.gnu.org/licenses/lgpl.txt
[2]: https://github.com/exasol/exasol-virtual-schema
[62]: https://www.jacoco.org/jacoco/trunk/doc/maven.html
[21]: https://github.com/mockito/mockito/blob/main/LICENSE
[9]: https://projectlombok.org/LICENSE
[16]: https://github.com/exasol/hamcrest-resultset-matcher
[44]: http://zlika.github.io/reproducible-build-maven-plugin
[23]: http://www.opensource.org/licenses/mit-license.php
[32]: http://sonarsource.github.io/sonar-scanner-maven/
[30]: https://github.com/exasol/udf-debugging-java/
[18]: https://junit.org/junit5/
[14]: http://hamcrest.org/JavaHamcrest/
[22]: http://www.slf4j.org
[68]: http://maven.apache.org/plugins/maven-resources-plugin/
[54]: https://github.com/exasol/artifact-reference-checker-maven-plugin
[52]: https://maven.apache.org/plugins/maven-jar-plugin/
[0]: https://github.com/exasol/db-fundamentals-java
[24]: https://github.com/exasol/test-db-builder-java/
[60]: https://maven.apache.org/surefire/maven-failsafe-plugin/
[13]: http://opensource.org/licenses/MIT
[5]: https://docs.exasol.com/db/latest/connect_exasol/drivers/jdbc.htm#License
[10]: https://github.com/exasol/exasol-testcontainers
[35]: https://github.com/exasol/project-keeper/blob/main/LICENSE
[56]: https://maven.apache.org/plugins/maven-dependency-plugin/
[27]: https://www.apache.org/licenses/LICENSE-2.0.txt
[26]: https://www.jqno.nl/equalsverifier
[38]: https://maven.apache.org/enforcer/maven-enforcer-plugin/
[4]: http://www.exasol.com
[19]: https://www.eclipse.org/legal/epl-v20.html
[70]: http://maven.apache.org/plugins/maven-install-plugin/
[42]: https://sonatype.github.io/ossindex-maven/maven-plugin/
[12]: https://testcontainers.org
[58]: https://anthonywhitford.com/lombok.maven/lombok-maven-plugin/
[72]: http://maven.apache.org/plugins/maven-deploy-plugin/
[74]: http://maven.apache.org/plugins/maven-site-plugin/
[64]: https://github.com/exasol/error-code-crawler-maven-plugin
[50]: https://maven.apache.org/plugins/maven-assembly-plugin/
