package com.exasol.adapter.dialects.rls;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.containers.ExasolContainerConstants;

@Testcontainers
class QueryRuntimeIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(RowLevelSecurityDialectIT.class);
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>(
            ExasolContainerConstants.EXASOL_DOCKER_IMAGE_REFERENCE) //
                    .withLogConsumer(new Slf4jLogConsumer(LOGGER));

    @BeforeAll
    static void beforeAll() throws Exception {
        createRowLevelSecuritySchema();
        uploadRlsAdapter();
    }

    private static void createRowLevelSecuritySchema() throws SQLException {
        final Connection connection = container.createConnection(null);
        final Bucket bucket = container.getDefaultBucket();
        final Statement statement = connection.createStatement();
        statement.execute("CREATE SCHEMA RLS_SCHEMA");
        statement.execute("CREATE OR REPLACE JAVA ADAPTER SCRIPT RLS_SCHEMA.RLS_VS_ADAPTER AS\n" //
                + "    %scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "    %jar /buckets/" + bucket.getBucketFsName() + "/" + bucket.getBucketName()
                + "/row-level-security-dist-0.2.1.jar;\n" //
                + "/");
        statement.execute("CREATE CONNECTION EXASOL_JDBC_CONNECTION \n" + "TO 'jdbc:exa:localhost:"
                + container.getFirstMappedPort() + "' USER '" + container.getUsername() + " IDENTIFIED BY "
                + container.getPassword());
        statement.execute("CREATE VIRTUAL SCHEMA <virtual schema name> \n" //
                + "    USING RLS_SCHEMA.RLS_VS_ADAPTER\n" //
                + "    WITH\n" //
                + "    SQL_DIALECT     = 'EXASOL_RLS'\n" //
                + "    CONNECTION_NAME = 'EXASOL_JDBC_CONNECTION'\n" //
                + "    SCHEMA_NAME     = 'RLS_SCHEMA'\n" //
                + "    IS_LOCAL = 'true'");
    }

    private static void uploadRlsAdapter() throws InterruptedException, BucketAccessException, TimeoutException {
        final String adapterFileName = "row-level-security-dist-0.2.1.jar";
        container.getDefaultBucket().uploadFile(Path.of("target", adapterFileName), adapterFileName);
    }

    @Test
    void test() {
        fail("Not yet implemented");
    }
}