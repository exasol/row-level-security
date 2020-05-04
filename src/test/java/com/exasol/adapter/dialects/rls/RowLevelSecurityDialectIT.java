package com.exasol.adapter.dialects.rls;

import static com.exasol.dbbuilder.ObjectPrivilege.SELECT;
import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;
import com.exasol.dbbuilder.*;

@Tag("integration")
@Testcontainers
class RowLevelSecurityDialectIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(RowLevelSecurityDialectIT.class);
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE) //
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER));
    private static AdapterScript adapterScript = null;
    private static ConnectionDefinition connectionDefinition = null;
    private static DatabaseObjectFactory factory = null;

    @BeforeAll
    static void beforeAll() throws SQLException, BucketAccessException, InterruptedException, TimeoutException {
        factory = new ExasolObjectFactory(container.createConnection(""));
        uploadAdapterScript();
        registerAdapterScript();
        createConnectionDefinition();
    }

    private static void uploadAdapterScript() {
        final String adapterScriptPath = "target/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
        try {
            final Bucket bucket = container.getDefaultBucket();
            final Path pathToRls = Path.of(adapterScriptPath);
            bucket.uploadFile(pathToRls, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        } catch (InterruptedException | BucketAccessException | TimeoutException exception) {
            throw new AssertionError(
                    "Unable to prepare test: upload of adapter script \"" + adapterScriptPath + " failed.", exception);
        }
    }

    private static void registerAdapterScript() {
        final Schema schema = factory.createSchema("SCHEMA_FOR_RLS_ADAPTER_SCRIPT");
        final String content = "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n";
        adapterScript = schema.createAdapterScript("RLS_ADAPTER", AdapterScript.Language.JAVA, content);
    }

    private static void createConnectionDefinition() {
        connectionDefinition = factory.createConnectionDefinition("RLS_CONNECTION", "jdbc:exa:localhost:8888",
                container.getUsername(), container.getPassword());
    }

    @BeforeEach
    void beforeEach() {
    }

    @Test
    void testTenantRestrictedTable() {
        final Schema sourceSchema = factory.createSchema("TENANT_PROTECTED_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_TENANT", "VARCHAR(128)") //
                .insert("Paris", "USER_T") //
                .insert("New York", "USER_T") //
                .insert("Rio", "SOMEONE_ELSE");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_TENANT", sourceSchema);
        final User user = factory.createLoginUser("USER_T") //
                .grant(virtualSchema, SELECT);
        final Connection rlsConnection = logIn(user);
        assertQueryResults(rlsConnection, "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE",
                new Object[][] { { "Paris" }, { "New York" } });
    }

    private VirtualSchema installVirtualSchema(final String name, final Schema sourceSchema) {
        return factory.createVirtualSchemaBuilder(name) //
                .adapterScript(adapterScript) //
                .dialectName("EXASOL_RLS") //
                .connectionDefinition(connectionDefinition) //
                .properties(Map.of("IS_LOCAL", "true", "LOG_LEVEL", "ALL", "DEBUG_ADDRESS", "10.0.2.15:3000")) //
                .sourceSchema(sourceSchema) //
                .build();
    }

    private Connection logIn(final User user) {
        try {
            return container.createConnectionForUser(user.getName(), user.getPassword());
        } catch (final SQLException exception) {
            throw new AssertionError("Unable to prepare test. Logging in user \"" + user.getName() + "\" failed.",
                    exception);
        }
    }

    private void assertQueryResults(final Connection connection, final String sql, final Object[][] expectedResults) {
        try (final Statement statement = connection.createStatement()) {
            final ResultSet result = statement.executeQuery(sql);
            assertColumnCount(result, expectedResults);
            assertRowsMatch(expectedResults, result);
        } catch (final SQLException exception) {
            throw new AssertionError("Unable to execute statement to assert query result: " + sql, exception);
        }
    }

    private void assertRowsMatch(final Object[][] expectedResults, final ResultSet result)
            throws SQLException, AssertionError {
        int rowIndex = 1;
        for (final Object[] expectedRow : expectedResults) {
            if (result.next()) {
                assertValuesInRowMatch(result, rowIndex, expectedRow);
                ++rowIndex;
            } else {
                throw new AssertionError(
                        "Expected result set ot have " + expectedResults + " rows, but it only had " + rowIndex + ".");
            }
        }
        if (result.next()) {
            throw new AssertionError("Result table has more than the expected " + expectedResults.length + "rows.");
        }
    }

    private void assertValuesInRowMatch(final ResultSet result, final int rowIndex, final Object[] expectedRow)
            throws SQLException, AssertionError {
        int columnIndex = 1;
        for (final Object expectedValue : expectedRow) {
            final Object value = result.getObject(columnIndex);
            if (!value.equals(expectedValue)) {
                throw new AssertionError("Result deviates in row " + rowIndex + ", column " + columnIndex
                        + ". Expected: '" + expectedValue + "' But was: '" + value + "'.");
            }
            ++columnIndex;
        }
    }

    private void assertColumnCount(final ResultSet result, final Object[][] expectedResults)
            throws SQLException, AssertionError {
        final int resultColumnCount = result.getMetaData().getColumnCount();
        final int expectedColumnCount = expectedResults[0].length;
        if (resultColumnCount != expectedColumnCount) {
            throw new AssertionError(
                    "Result set has " + resultColumnCount + " columns, but " + expectedColumnCount + " were expected.");
        }
    }

    @Test
    void testGroupRestictedTable() {
        final Schema sourceSchema = factory.createSchema("GROUP_PROTECTED_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)") //
                .insert("Stockholm", "COLD") //
                .insert("Moskow", "COLD") //
                .insert("Horta", "MODERATE") //
                .insert("Rio", "HOT");
        sourceSchema.createTable("EXA_USER_GROUPS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_G", "COLD") //
                .insert("USER_G", "MODERATE");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP", sourceSchema);
        final User user = factory.createLoginUser("USER_G") //
                .grant(virtualSchema, SELECT);
        final Connection rlsConnection = logIn(user);
        assertQueryResults(rlsConnection,
                "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY",
                new Object[][] { { "Horta" }, { "Moskow" }, { "Stockholm" } });
    }

    @Test
    void testRoleRestrictedTable() {
        final Schema sourceSchema = factory.createSchema("ROLE_PROTECTED_SCHEMA");
        sourceSchema
                .createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "ZIP", "DECIMAL(5,0)", "EXA_ROW_ROLES",
                        "VARCHAR(128)") //
                .insert("Traunreut", 83301, 1) //
                .insert("Inzell", 83334, 3) //
                .insert("Eilsbrunn", 93161, Long.toUnsignedString(RowLevelSecurityDialectConstants.DEFAULT_ROLE_MASK)) //
                .insert("Nuremberg", 90411, 2);
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("USER_R_A", 1) //
                .insert("USER_R_B", 2);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE", sourceSchema);
        final User user = factory.createLoginUser("USER_R_A") //
                .grant(virtualSchema, SELECT);
        final Connection rlsConnection = logIn(user);
        assertQueryResults(rlsConnection,
                "SELECT ZIP FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY ZIP",
                new Object[][] { { 83301 }, { 83334 }, { 93161 } });
    }

    @Test
    void testUnprotectedTable() {
        final Schema sourceSchema = factory.createSchema("UNPROTECTED_SCHEMA");
        sourceSchema.createTable("FRUITS", "NAME", "VARCHAR(20)").insert("Apple").insert("Pear").insert("Orange");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_UNPROTECTED", sourceSchema);
        final User user = factory.createLoginUser("USER_R_A") //
                .grant(virtualSchema, SELECT);
        final Connection rlsConnection = logIn(user);
        assertQueryResults(rlsConnection, "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE",
                new Object[][] { { "Apple" }, { "Pear" }, { "Orange" } });
    }
}