package com.exasol.adapter.dialects.rls;

import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;
import com.exasol.dbbuilder.*;

@Testcontainers
class RowLevelSecurityDialectIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(RowLevelSecurityDialectIT.class);
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE) //
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER));
    private static DatabaseObjectFactory factory;

    @BeforeAll
    static void beforeAll() throws SQLException, BucketAccessException, InterruptedException, TimeoutException {
        factory = new ExasolObjectFactory(container.createConnection(""));
    }

    @Test
    void testGroupRestrictedTable()
            throws NoDriverFoundException, SQLException, InterruptedException, BucketAccessException, TimeoutException {
        final Schema sourceSchema = factory.createSchema("SOURCE_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_TENANT", "VARCHAR(128)") //
                .insert("PARIS", "FRED") //
                .insert("New York", "FRED") //
                .insert("RIO", "MARIA");
        final VirtualSchema virtualSchema = installVirtualSchema(sourceSchema);
        final User rlsUser = factory.createLoginUser("FRED").grant(virtualSchema, ObjectPrivilege.SELECT);
        final Connection rlsConnection = container.createConnectionForUser(rlsUser.getName(), rlsUser.getPassword());
        final Statement statement = rlsConnection.createStatement();
        final ResultSet result = statement
                .executeQuery("SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE");
        assertAll(() -> assertThat("Result has row 1", result.next(), equalTo(true)),
                () -> assertThat("Row 1 content", result.getString(1), equalTo("PARIS")),
                () -> assertThat("Result has row 2", result.next(), equalTo(true)),
                () -> assertThat("Row 2 content", result.getString(1), equalTo("New York")));

    }

    private static void installAdapterScript() throws InterruptedException, BucketAccessException, TimeoutException {
        final Bucket bucket = container.getDefaultBucket();
        final Path pathToRls = Path.of("target/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        bucket.uploadFile(pathToRls, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
    }

    private VirtualSchema installVirtualSchema(final Schema sourceSchema)
            throws InterruptedException, BucketAccessException, TimeoutException {
        installAdapterScript();
        final Schema schema = factory.createSchema("SCHEMA_FOR_RLS_ADAPTER_SCRIPT");
        final String content = "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n";
        final AdapterScript script = schema.createAdapterScript("RLS_ADAPTER", AdapterScript.Language.JAVA, content);
        final ConnectionDefinition connectionDefinition = factory.createConnectionDefinition("RLS_CONNECTION",
                "jdbc:exa:localhost:8888", container.getUsername(), container.getPassword());
        return factory.createVirtualSchemaBuilder("RLS_VS") //
                .adapterScript(script) //
                .dialectName("EXASOL_RLS") //
                .connectionDefinition(connectionDefinition) //
                .properties(Map.of("IS_LOCAL", "true", "LOG_LEVEL", "ALL", "DEBUG_ADDRESS", "10.0.2.15:3000")) //
                .sourceSchema(sourceSchema) //
                .build();
    }
}