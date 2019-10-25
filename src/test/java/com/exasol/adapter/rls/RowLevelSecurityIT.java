package com.exasol.adapter.rls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class RowLevelSecurityIT {
    private static final String EXASOL_DOCKER_IMAGE_VERSION = "6.2.2-d1";
    private static final String EXASOL_DOCKER_IMAGE_ID = "exasol/docker-db";
    private static final Integer EXASOL_PORT = 8888;
    private static final Integer BUCKETFS_PORT = 2580;
    private static final String DATABASE_USER = "SYS";
    private static final String DATABASE_PWD = "exasol";

    @Test
    void test() throws IOException, SQLException {
        final GenericContainer exasol = new GenericContainer<>(
                EXASOL_DOCKER_IMAGE_ID + ":" + EXASOL_DOCKER_IMAGE_VERSION) //
                        .withExposedPorts(EXASOL_PORT, BUCKETFS_PORT);
        final Connection connection = DriverManager.getConnection("jdbc:exa:localhost:" + EXASOL_PORT, DATABASE_USER,
                DATABASE_PWD);
        assertThat(connection.getMetaData().getDatabaseProductVersion(), equalTo(EXASOL_DOCKER_IMAGE_VERSION));
    }
}