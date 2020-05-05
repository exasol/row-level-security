package com.exasol.adapter.dialects.rls;

import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.*;
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

@Tag("integration")
@Testcontainers
class QueryRuntimeIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryRuntimeIT.class);
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE) //
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    @BeforeAll
    static void beforeAll() throws Exception {
        createSourceTables();
        uploadRlsAdapter();
        createRowLevelSecuritySchema();
    }

    private static void createSourceTables() throws NoDriverFoundException, SQLException {
        final Connection connection = container.createConnection("");
        executeQueries(connection, //
                "CREATE SCHEMA SIMPLE_SALES", //
                "CREATE TABLE SIMPLE_SALES.ORDER_ITEM (ORDER_ID DECIMAL(18,0), CUSTOMER VARCHAR(50)," //
                        + " PRODUCT VARCHAR(100), QUANTITY DECIMAL(18,0), EXA_ROW_ROLES DECIMAL(20,0))", //
                "INSERT INTO SIMPLE_SALES.ORDER_ITEM VALUES\n" //
                        + "(1, 'John Smith', 'Pen', 3, 1),\n" //
                        + "(1, 'John Smith', 'Paper', 100, 3),\n" //
                        + "(1, 'John Smith', 'Eraser', 1, 7),\n" //
                        + "(2, 'Jane Doe', 'Pen', 2, 2),\n" //
                        + "(2, 'Jane Doe', 'Paper', 200, 1)", //
                "CREATE TABLE SIMPLE_SALES.EXA_RLS_USERS(EXA_USER_NAME VARCHAR(128), EXA_ROLE_MASK DECIMAL(20,0))",
                "INSERT INTO SIMPLE_SALES.EXA_RLS_USERS VALUES\n" //
                        + "('SALES', 1),\n" //
                        + "('DEVELOPMENT', 2),\n" //
                        + "('FINANCE', 4)");
    }

    private static void uploadRlsAdapter() throws InterruptedException, BucketAccessException, TimeoutException {
        final Path localAdapterPath = Path.of("target", ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION).toAbsolutePath();
        container.getDefaultBucket().uploadFile(localAdapterPath, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
    }

    private static void createRowLevelSecuritySchema() throws SQLException {
        final Connection connection = container.createConnection("");
        final Bucket bucket = container.getDefaultBucket();
        executeQueries(connection, //
                "CREATE SCHEMA RLS_SCHEMA", //
                "CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.RLS_VS_ADAPTER AS\n" //
                        + "    %scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                        + "    %jar /buckets/" + bucket.getBucketFsName() + "/" + bucket.getBucketName() //
                        + "/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n" //
                        + "/", //
                "CREATE CONNECTION EXASOL_JDBC_CONNECTION TO 'jdbc:exa:localhost:" //
                        + container.getExposedPorts().get(0) + "' USER '" + container.getUsername()
                        + "' IDENTIFIED BY '" //
                        + container.getPassword() + "'", //
                "CREATE VIRTUAL SCHEMA RLS_VS \n" //
                        + "    USING RLS_SCHEMA.RLS_VS_ADAPTER\n" //
                        + "    WITH\n" //
                        + "    SQL_DIALECT     = 'EXASOL_RLS'\n" //
                        + "    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'\n" //
                        + "    SCHEMA_NAME     = 'SIMPLE_SALES'\n" //
                        + "    IS_LOCAL        = 'true'");
    }

    private static void executeQueries(final Connection connection, final String... sqls) throws SQLException {
        final Statement statement = connection.createStatement();
        for (final String sql : sqls) {
            LOGGER.info(sql);
            statement.execute(sql);
        }
    }

    // [itest->qs~total-runtime-of-secured-simple-query~1]
    @Test
    void testSimpleQueryRuntime() throws NoDriverFoundException, SQLException {
        final Connection connection = container.createConnection("");
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