package com.exasol.rls.administration.scripts;

import java.nio.file.Path;

public final class IntegrationTestsConstants {
    protected static final Path PATH_TO_EXA_RLS_BASE = Path.of("src/main/sql/exa_rls_base.sql");
    protected static final Path PATH_TO_ADD_RLS_ROLE = Path.of("src/main/sql/add_rls_role.sql");
    protected static final Path PATH_TO_DELETE_RLS_ROLE = Path.of("src/main/sql/delete_rls_role.sql");
    protected static final Path PATH_TO_ROLES_MASK = Path.of("src/main/sql/roles_mask.sql");
    protected static final Path PATH_TO_ASSIGN_ROLES_TO_USER = Path.of("src/main/sql/assign_roles_to_user.sql");
    protected static final String RLS_SCHEMA_NAME = "RLS_SCHEMA";

    private IntegrationTestsConstants() {
        // intentionally left blank
    }
}