package com.exasol.tools;

import java.nio.file.Path;

public final class TestsConstants {
    public static final Path PATH_TO_EXA_RLS_BASE = Path.of("src/main/sql/exa_rls_base.sql");
    public static final Path PATH_TO_ADD_RLS_ROLE = Path.of("src/main/sql/add_rls_role.sql");
    public static final Path PATH_TO_DELETE_RLS_ROLE = Path.of("src/main/sql/delete_rls_role.sql");
    public static final Path PATH_TO_ROLES_MASK = Path.of("src/main/sql/roles_mask.sql");
    public static final Path PATH_TO_ASSIGN_ROLES_TO_USER = Path.of("src/main/sql/assign_roles_to_user.sql");
    public static final String RLS_SCHEMA_NAME = "RLS_SCHEMA";
    public static final String ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION = "row-level-security-dist-1.0.2.jar";

    private TestsConstants() {
        // intentionally left blank
    }
}