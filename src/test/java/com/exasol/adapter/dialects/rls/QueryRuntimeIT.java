package com.exasol.adapter.dialects.rls;

import static com.exasol.dbbuilder.dialects.exasol.AdapterScript.Language.JAVA;
import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.HostIpProvider;
import com.exasol.dbbuilder.dialects.exasol.*;
import com.exasol.udfdebugging.UdfTestSetup;

@Tag("integration")
@Tag("virtual-schema")
@Tag("slow")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true") // CI is usually to slow for a realistic result
@Testcontainers
class QueryRuntimeIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>().withReuse(true);
    private static ExasolObjectFactory objectFactory;

    @BeforeAll
    static void beforeAll() throws Exception {
        final UdfTestSetup udfTestSetup = new UdfTestSetup(HostIpProvider.getHostIpFromContainer(EXASOL),
                EXASOL.getDefaultBucket());
        objectFactory = new ExasolObjectFactory(EXASOL.createConnection(""),
                ExasolObjectConfiguration.builder().withJvmOptions(udfTestSetup.getJvmOptions()).build());
        final ExasolSchema sourceSchema = createSourceSchema();
        uploadRlsAdapter();
        createRowLevelSecuritySchema(sourceSchema);
    }

    private static ExasolSchema createSourceSchema() throws NoDriverFoundException, SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("SIMPLE_SALES");
        sourceSchema.createTableBuilder("ORDER_ITEM") //
                .column("ORDER_ID", "DECIMAL(18,0)") //
                .column("CUSTOMER", "VARCHAR(50)") //
                .column("PRODUCT", "VARCHAR(100)") //
                .column("QUANTITY", "DECIMAL(18,0)") //
                .column("EXA_ROW_ROLES", "DECIMAL(20,0)") //
                .build() //
                .insert(1, "John Smith", "Pen", 3, 1) //
                .insert(1, "John Smith", "Paper", 100, 3) //
                .insert(1, "John Smith", "Eraser", 1, 7) //
                .insert(2, "Jane Doe", "Pen", 2, 2) //
                .insert(2, "Jane Doe", "Paper", 200, 1);
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20,0)")
                .insert("SALES", 1) //
                .insert("DEVELOPMENT", 2) //
                .insert("FINANCE", 4);
        return sourceSchema;
    }

    private static void uploadRlsAdapter() throws InterruptedException, BucketAccessException, TimeoutException {
        final Path localAdapterPath = Path.of("target", ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION).toAbsolutePath();
        EXASOL.getDefaultBucket().uploadFile(localAdapterPath, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
    }

    private static void createRowLevelSecuritySchema(final ExasolSchema sourceSchema) throws SQLException {
        final Bucket bucket = EXASOL.getDefaultBucket();
        final ExasolSchema rlsSchema = objectFactory.createSchema("RLS_SCHEMA");
        final String scriptContent = "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/" + bucket.getBucketFsName() + "/" + bucket.getBucketName() //
                + "/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";";
        final AdapterScript adapterScript = rlsSchema.createAdapterScript("RLS_VS_ADAPTER", JAVA, scriptContent);
        final ConnectionDefinition connectionDefinition = objectFactory.createConnectionDefinition(
                "EXASOL_JDBC_CONNECTION", "jdbc:exa:localhost:" + EXASOL.getExposedPorts().get(0), EXASOL.getUsername(),
                EXASOL.getPassword());
        objectFactory.createVirtualSchemaBuilder("RLS_VS") //
                .adapterScript(adapterScript) //
                .dialectName("EXASOL_RLS") //
                .connectionDefinition(connectionDefinition) //
                .sourceSchema(sourceSchema) //
                .properties(Map.of("IS_LOCAL", "true")) //
                .build();
    }

    // [itest->qs~total-runtime-of-secured-simple-query~1]
    @Test
    void testSimpleQueryRuntime() throws NoDriverFoundException, SQLException {
        final Connection connection = EXASOL.createConnection("");
        final long originalRuntime = executeTimedQuery(connection, "SELECT * FROM SIMPLE_SALES.ORDER_ITEM");
        final long rlsRuntime = executeTimedQuery(connection, "SELECT * FROM RLS_VS.ORDER_ITEM");
        final long maxRelativeMillis = Math.round(originalRuntime * 1.1);
        final long maxAbsoluteMillis = originalRuntime + 2000;
        assertThat(rlsRuntime, either(lessThanOrEqualTo(maxRelativeMillis)).or(lessThanOrEqualTo(maxAbsoluteMillis)));
    }

    private long executeTimedQuery(final Connection connection, final String sql) throws SQLException {
        final Statement statement = connection.createStatement();
        final long millisBeforeQuery = System.currentTimeMillis();
        statement.execute(sql);
        return System.currentTimeMillis() - millisBeforeQuery;
    }
}