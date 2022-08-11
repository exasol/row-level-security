<!-- @formatter:off -->
# Dependencies

## Compile Dependencies

| Dependency                                 | License                |
| ------------------------------------------ | ---------------------- |
| [Exasol Database fundamentals for Java][0] | [MIT][1]               |
| [Exasol Virtual Schema][2]                 | [MIT License][3]       |
| [EXASolution JDBC Driver][4]               | [EXAClient License][5] |
| [error-reporting-java][6]                  | [MIT][1]               |
| [Project Lombok][7]                        | [The MIT License][8]   |

## Test Dependencies

| Dependency                                      | License                           |
| ----------------------------------------------- | --------------------------------- |
| [Test containers for Exasol on Docker][9]       | [MIT][1]                          |
| [Testcontainers :: JUnit Jupiter Extension][10] | [MIT][11]                         |
| [Hamcrest][12]                                  | [BSD License 3][13]               |
| [Matcher for SQL Result Sets][14]               | [MIT][1]                          |
| [JUnit Jupiter (Aggregator)][15]                | [Eclipse Public License v2.0][16] |
| [JUnit Jupiter API][15]                         | [Eclipse Public License v2.0][16] |
| [mockito-junit-jupiter][17]                     | [The MIT License][18]             |
| [SLF4J JDK14 Binding][19]                       | [MIT License][20]                 |
| [Test Database Builder for Java][21]            | [MIT License][22]                 |
| [EqualsVerifier | release normal jar][23]       | [Apache License, Version 2.0][24] |
| [JaCoCo :: Agent][25]                           | [Eclipse Public License 2.0][26]  |
| [udf-debugging-java][27]                        | [MIT][1]                          |

## Plugin Dependencies

| Dependency                                              | License                                        |
| ------------------------------------------------------- | ---------------------------------------------- |
| [SonarQube Scanner for Maven][28]                       | [GNU LGPL 3][29]                               |
| [Apache Maven Compiler Plugin][30]                      | [Apache License, Version 2.0][24]              |
| [Apache Maven Enforcer Plugin][31]                      | [Apache License, Version 2.0][24]              |
| [Maven Flatten Plugin][32]                              | [Apache Software Licenese][33]                 |
| [org.sonatype.ossindex.maven:ossindex-maven-plugin][34] | [ASL2][33]                                     |
| [Maven Surefire Plugin][35]                             | [Apache License, Version 2.0][24]              |
| [Versions Maven Plugin][36]                             | [Apache License, Version 2.0][24]              |
| [Apache Maven Assembly Plugin][37]                      | [Apache License, Version 2.0][24]              |
| [Apache Maven JAR Plugin][38]                           | [Apache License, Version 2.0][24]              |
| [Artifact reference checker and unifier][39]            | [MIT][1]                                       |
| [Apache Maven Dependency Plugin][40]                    | [Apache License, Version 2.0][24]              |
| [Lombok Maven Plugin][41]                               | [The MIT License][1]                           |
| [Project keeper maven plugin][42]                       | [The MIT License][43]                          |
| [Exec Maven Plugin][44]                                 | [Apache License 2][33]                         |
| [OpenFastTrace Maven Plugin][45]                        | [GNU General Public License v3.0][46]          |
| [Build Helper Maven Plugin][47]                         | [The MIT License][48]                          |
| [Maven Failsafe Plugin][49]                             | [Apache License, Version 2.0][24]              |
| [JaCoCo :: Maven Plugin][50]                            | [Eclipse Public License 2.0][26]               |
| [error-code-crawler-maven-plugin][51]                   | [MIT License][52]                              |
| [Reproducible Build Maven Plugin][53]                   | [Apache 2.0][33]                               |
| [Maven Clean Plugin][54]                                | [The Apache Software License, Version 2.0][33] |
| [Maven Resources Plugin][55]                            | [The Apache Software License, Version 2.0][33] |
| [Maven Install Plugin][56]                              | [The Apache Software License, Version 2.0][33] |
| [Maven Deploy Plugin][57]                               | [The Apache Software License, Version 2.0][33] |
| [Maven Site Plugin 3][58]                               | [The Apache Software License, Version 2.0][33] |

[0]: https://github.com/exasol/db-fundamentals-java
[1]: https://opensource.org/licenses/MIT
[2]: https://github.com/exasol/exasol-virtual-schema/
[3]: https://github.com/exasol/exasol-virtual-schema/blob/main/LICENSE
[4]: http://www.exasol.com
[5]: https://docs.exasol.com/db/latest/connect_exasol/drivers/jdbc.htm#License
[6]: https://github.com/exasol/error-reporting-java
[7]: https://projectlombok.org
[8]: https://projectlombok.org/LICENSE
[9]: https://github.com/exasol/exasol-testcontainers
[10]: https://testcontainers.org
[11]: http://opensource.org/licenses/MIT
[12]: http://hamcrest.org/JavaHamcrest/
[13]: http://opensource.org/licenses/BSD-3-Clause
[14]: https://github.com/exasol/hamcrest-resultset-matcher
[15]: https://junit.org/junit5/
[16]: https://www.eclipse.org/legal/epl-v20.html
[17]: https://github.com/mockito/mockito
[18]: https://github.com/mockito/mockito/blob/main/LICENSE
[19]: http://www.slf4j.org
[20]: http://www.opensource.org/licenses/mit-license.php
[21]: https://github.com/exasol/test-db-builder-java/
[22]: https://github.com/exasol/test-db-builder-java/blob/main/LICENSE
[23]: https://www.jqno.nl/equalsverifier
[24]: https://www.apache.org/licenses/LICENSE-2.0.txt
[25]: https://www.eclemma.org/jacoco/index.html
[26]: https://www.eclipse.org/legal/epl-2.0/
[27]: https://github.com/exasol/udf-debugging-java/
[28]: http://sonarsource.github.io/sonar-scanner-maven/
[29]: http://www.gnu.org/licenses/lgpl.txt
[30]: https://maven.apache.org/plugins/maven-compiler-plugin/
[31]: https://maven.apache.org/enforcer/maven-enforcer-plugin/
[32]: https://www.mojohaus.org/flatten-maven-plugin/
[33]: http://www.apache.org/licenses/LICENSE-2.0.txt
[34]: https://sonatype.github.io/ossindex-maven/maven-plugin/
[35]: https://maven.apache.org/surefire/maven-surefire-plugin/
[36]: http://www.mojohaus.org/versions-maven-plugin/
[37]: https://maven.apache.org/plugins/maven-assembly-plugin/
[38]: https://maven.apache.org/plugins/maven-jar-plugin/
[39]: https://github.com/exasol/artifact-reference-checker-maven-plugin
[40]: https://maven.apache.org/plugins/maven-dependency-plugin/
[41]: https://anthonywhitford.com/lombok.maven/lombok-maven-plugin/
[42]: https://github.com/exasol/project-keeper/
[43]: https://github.com/exasol/project-keeper/blob/main/LICENSE
[44]: http://www.mojohaus.org/exec-maven-plugin
[45]: https://github.com/itsallcode/openfasttrace-maven-plugin
[46]: https://www.gnu.org/licenses/gpl-3.0.html
[47]: http://www.mojohaus.org/build-helper-maven-plugin/
[48]: https://opensource.org/licenses/mit-license.php
[49]: https://maven.apache.org/surefire/maven-failsafe-plugin/
[50]: https://www.jacoco.org/jacoco/trunk/doc/maven.html
[51]: https://github.com/exasol/error-code-crawler-maven-plugin/
[52]: https://github.com/exasol/error-code-crawler-maven-plugin/blob/main/LICENSE
[53]: http://zlika.github.io/reproducible-build-maven-plugin
[54]: http://maven.apache.org/plugins/maven-clean-plugin/
[55]: http://maven.apache.org/plugins/maven-resources-plugin/
[56]: http://maven.apache.org/plugins/maven-install-plugin/
[57]: http://maven.apache.org/plugins/maven-deploy-plugin/
[58]: http://maven.apache.org/plugins/maven-site-plugin/
