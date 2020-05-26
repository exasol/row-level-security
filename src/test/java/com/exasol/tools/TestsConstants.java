package com.exasol.tools;

import java.nio.file.Path;

public final class TestsConstants {
    private static final Path ADMIN_SCRIPT_BASE_PATH = Path.of("src/main/sql/");
    public static final Path PATH_TO_EXA_RLS_BASE = ADMIN_SCRIPT_BASE_PATH.resolve("exa_rls_base.sql");
    public static final Path PATH_TO_ADD_RLS_ROLE = ADMIN_SCRIPT_BASE_PATH.resolve("add_rls_role.sql");
    public static final Path PATH_TO_DELETE_RLS_ROLE = ADMIN_SCRIPT_BASE_PATH.resolve("delete_rls_role.sql");
    public static final Path PATH_TO_ROLES_MASK = ADMIN_SCRIPT_BASE_PATH.resolve("oles_mask.sql");
    public static final Path PATH_TO_ASSIGN_ROLES_TO_USER = ADMIN_SCRIPT_BASE_PATH.resolve("assign_roles_to_user.sql");
    public static final Path PATH_TO_ADD_GROUP_MEMBER = ADMIN_SCRIPT_BASE_PATH.resolve("add_user_to_group.sql");
    public static final String RLS_SCHEMA_NAME = "RLS_SCHEMA";
    public static final String ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION = "row-level-security-dist-1.0.2.jar";

    private TestsConstants() {
        // prevent instantiation
    }
}