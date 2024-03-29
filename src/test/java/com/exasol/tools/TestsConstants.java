package com.exasol.tools;

import java.nio.file.Path;

public final class TestsConstants {
    public static final String ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION = "row-level-security-dist-3.0.6.jar";

    // Lua scripts
    private static final Path ADMIN_SCRIPT_BASE_PATH = Path.of("src/main/lua/");
    public static final Path PATH_TO_EXA_RLS_BASE = ADMIN_SCRIPT_BASE_PATH.resolve("exa_rls_base.lua");
    public static final Path PATH_TO_EXA_IDENTIFIER = ADMIN_SCRIPT_BASE_PATH.resolve("exa_identifier.lua");
    public static final Path PATH_TO_ADD_RLS_ROLE = ADMIN_SCRIPT_BASE_PATH.resolve("add_rls_role.lua");
    public static final Path PATH_TO_DELETE_RLS_ROLE = ADMIN_SCRIPT_BASE_PATH.resolve("delete_rls_role.lua");
    public static final Path PATH_TO_ASSIGN_ROLES_TO_USER = ADMIN_SCRIPT_BASE_PATH.resolve("assign_roles_to_user.lua");
    public static final Path PATH_TO_ADD_USER_TO_GROUP = ADMIN_SCRIPT_BASE_PATH.resolve("add_user_to_group.lua");
    public static final Path PATH_TO_REMOVE_USER_FROM_GROUP = ADMIN_SCRIPT_BASE_PATH
            .resolve("remove_user_from_group.lua");
    public static final Path PATH_TO_LIST_ALL_GROUPS = ADMIN_SCRIPT_BASE_PATH.resolve("list_all_groups.lua");
    public static final Path PATH_TO_LIST_ALL_ROLES = ADMIN_SCRIPT_BASE_PATH.resolve("list_all_roles.lua");
    public static final Path PATH_TO_LIST_USER_GROUPS = ADMIN_SCRIPT_BASE_PATH.resolve("list_user_groups.lua");
    public static final Path PATH_TO_LIST_USER_ROLES = ADMIN_SCRIPT_BASE_PATH.resolve("list_user_roles.lua");
    public static final Path PATH_TO_LIST_USERS_AND_ROLES = ADMIN_SCRIPT_BASE_PATH.resolve("list_users_and_roles.lua");
    public static final Path PATH_TO_BIT_POSITIONS = ADMIN_SCRIPT_BASE_PATH.resolve("bit_positions.lua");

    // SQL scripts
    private static final Path SQL_SCRIPT_BASE_PATH = Path.of("src/main/sql/");
    public static final Path PATH_TO_ROLE_MASK = SQL_SCRIPT_BASE_PATH.resolve("role_mask.sql");

    private TestsConstants() {
        // prevent instantiation
    }
}
