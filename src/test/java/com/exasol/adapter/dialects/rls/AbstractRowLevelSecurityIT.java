package com.exasol.adapter.dialects.rls;

import static com.exasol.dbbuilder.dialects.exasol.ExasolObjectPrivilege.SELECT;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.exasol.bucketfs.Bucket;
import com.exasol.bucketfs.BucketAccessException;
import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.dialects.Table;
import com.exasol.dbbuilder.dialects.User;
import com.exasol.dbbuilder.dialects.exasol.*;
import com.exasol.matcher.ResultSetStructureMatcher.Builder;

@Tag("integration")
@Tag("virtual-schema")
@Testcontainers
abstract class AbstractRowLevelSecurityIT {
    @Container
    private static final ExasolContainer<? extends ExasolContainer<?>> container = new ExasolContainer<>();
    private static AdapterScript adapterScript = null;
    private static ConnectionDefinition connectionDefinition = null;
    private static ExasolObjectFactory factory = null;

    /**
     * Define the properties with which the Virtual Schema under test is created.
     * <p>
     * All derived classes must implement this in order to parameterize the execution of the test cases listed in this
     * abstract class.
     * </p>
     *
     * @return properties with which the Virtual Schema is created
     */
    protected abstract Map<String, String> getVirtualSchemaProperties();

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
        } catch (final InterruptedException | BucketAccessException | TimeoutException exception) {
            throw new AssertionError(
                    "Unable to prepare test: upload of adapter script \"" + adapterScriptPath + " failed.", exception);
        }
    }

    private static void registerAdapterScript() {
        final ExasolSchema schema = factory.createSchema("SCHEMA_FOR_RLS_ADAPTER_SCRIPT");
        final String content = "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n";
        adapterScript = schema.createAdapterScript("RLS_ADAPTER", AdapterScript.Language.JAVA, content);
    }

    private static void createConnectionDefinition() {
        connectionDefinition = factory.createConnectionDefinition("RLS_CONNECTION", "jdbc:exa:localhost:8888",
                container.getUsername(), container.getPassword());
    }

    // [itest->dsn~query-rewriter-adds-row-filter-for-tenants~1]
    @Test
    void testTenantRestrictedTable() {
        final ExasolSchema sourceSchema = factory.createSchema("TENANT_PROTECTED_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_TENANT", "VARCHAR(128)") //
                .insert("Paris", "USER_T_A") //
                .insert("New York", "USER_T_A") //
                .insert("Rio", "USER_T_B");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_TENANT", sourceSchema);
        final User userA = factory.createLoginUser("USER_T_A").grant(virtualSchema, SELECT);
        final User userB = factory.createLoginUser("USER_T_B").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE";
        assertAll(() -> assertThat(queryForUser(sql, userA), table().row("Paris").row("New York").matches()),
                () -> assertThat(queryForUser(sql, userB), table().row("Rio").matches()));
    }

    private ResultSet queryForUser(final String sql, final User user) throws SQLException {
        try (final Connection connection = container.createConnectionForUser(user.getName(), user.getPassword());
                final Statement statement = connection.createStatement();
                final ResultSet result = statement.executeQuery(sql)) {
            return result;
        }
    }

    private VirtualSchema installVirtualSchema(final String name, final ExasolSchema sourceSchema) {
        return factory.createVirtualSchemaBuilder(name) //
                .adapterScript(adapterScript) //
                .dialectName("EXASOL_RLS") //
                .connectionDefinition(connectionDefinition) //
                .properties(getVirtualSchemaProperties()) //
                .sourceSchema(sourceSchema) //
                .build();
    }

    // [itest->dsn~query-rewriter-adds-row-filter-for-group~1]
    @Test
    void testGroupRestictedTable() throws SQLException {
        final ExasolSchema sourceSchema = factory.createSchema("GROUP_PROTECTED_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)") //
                .insert("Stockholm", "COLD") //
                .insert("Moskow", "COLD") //
                .insert("Horta", "MODERATE") //
                .insert("Rio", "HOT");
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_G", "COLD") //
                .insert("USER_G", "MODERATE");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP", sourceSchema);
        final User user = factory.createLoginUser("USER_G").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        assertThat(queryForUser(sql, user), table().row("Horta").row("Moskow").row("Stockholm").matches());
    }

    // [itest->dsn~query-rewriter-treats-protected-tables-with-group-and-tenant-restrictions~1]
    @Test
    void testGroupAndTenantRestrictedTable() throws SQLException {
        final ExasolSchema sourceSchema = factory.createSchema("GROUP_AND_TENANT_PROTECTED_SCHEMA");
        sourceSchema
                .createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_TENANT", "VARCHAR(128)", "EXA_ROW_GROUP",
                        "VARCHAR(128)") //
                .insert("Helsinki", null, "COLD") //
                .insert("Horta", "USER_GT_T", "MODERATE") //
                .insert("Lhasa", "USER_GT_GT", "COLD") //
                .insert("Moskow", "NOONE", "COLD") //
                .insert("Rio", null, "HOT") //
                .insert("Stockholm", "USER_GT_T", "COLD") //
                .insert("Vienna", null, null);
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_GT_G", "COLD") //
                .insert("USER_GT_G", "MODERATE") //
                .insert("USER_GT_GT", "HOT");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP_AND_TENANT", sourceSchema);
        final User userTenantOnly = factory.createLoginUser("USER_GT_T").grant(virtualSchema, SELECT);
        final User userGroupOnly = factory.createLoginUser("USER_GT_G").grant(virtualSchema, SELECT);
        final User userGroupAndTenant = factory.createLoginUser("USER_GT_GT").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        final Builder expectedForTenantOnlyUser = table().row("Horta").row("Stockholm");
        final Builder expectedForGroupOnlyUser = table().row("Helsinki").row("Horta").row("Lhasa").row("Moskow")
                .row("Stockholm");
        final Builder expectedForGroupAndTenantUser = table().row("Lhasa").row("Rio");
        assertAll(() -> assertThat(queryForUser(sql, userTenantOnly), expectedForTenantOnlyUser.matches()),
                () -> assertThat(queryForUser(sql, userGroupOnly), expectedForGroupOnlyUser.matches()),
                () -> assertThat(queryForUser(sql, userGroupAndTenant), expectedForGroupAndTenantUser.matches()));
    }

    // [itest->dsn~all-users-have-the-public-access-role~1]
    @Test
    void testRoleRestrictedTable() {
        final ExasolSchema sourceSchema = factory.createSchema("ROLE_PROTECTED_SCHEMA");
        sourceSchema
                .createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "ZIP", "DECIMAL(5,0)", "EXA_ROW_ROLES",
                        "VARCHAR(128)") //
                .insert("Traunreut", 83301, 1) //
                .insert("Inzell", 83334, 3) //
                .insert("Nuremberg", 90411, 2) //
                .insert("Eilsbrunn", 93161, Long.toUnsignedString(RowLevelSecurityDialectConstants.DEFAULT_ROLE_MASK));
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("USER_R_A", 1) //
                .insert("USER_R_B", 2);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE", sourceSchema);
        final User userA = factory.createLoginUser("USER_R_A").grant(virtualSchema, SELECT);
        final User userB = factory.createLoginUser("USER_R_B").grant(virtualSchema, SELECT);
        final User userPublic = factory.createLoginUser("USER_R_PUBLIC").grant(virtualSchema, SELECT);
        final String sql = "SELECT ZIP FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY ZIP";
        assertAll(() -> assertThat(queryForUser(sql, userA), table().row(83301).row(83334).row(93161).matchesFuzzily()),
                () -> assertThat(queryForUser(sql, userB), table().row(83334).row(90411).row(93161).matchesFuzzily()),
                () -> assertThat(queryForUser(sql, userPublic), table().row(93161).matchesFuzzily()));
    }

    // [itest->dsn~query-rewriter-treats-protected-tables-with-roles-and-tenant-restrictions~1]
    @Test
    void testRoleAndTenantRestrictedTable() {
        final ExasolSchema sourceSchema = factory.createSchema("ROLE_AND_TENANT_PROTECTED_SCHEMA");
        sourceSchema
                .createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_ROLES", "VARCHAR(128)", "EXA_ROW_TENANT",
                        "VARCHAR(128)") //
                .insert("Traunreut", 1, "USER_RT_A") //
                .insert("Inzell", 3, null) //
                .insert("Nuremberg", 2, "USER_RT_A") //
                .insert("Eilsbrunn", Long.toUnsignedString(RowLevelSecurityDialectConstants.DEFAULT_ROLE_MASK),
                        "USER_RT_A") //
                .insert("Tennenlohe", 1, "USER_RT_B");
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("USER_RT_A", 1) //
                .insert("USER_RT_B", 1);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE_AND_TENANT", sourceSchema);
        final User userA = factory.createLoginUser("USER_RT_A").grant(virtualSchema, SELECT);
        final User userB = factory.createLoginUser("USER_RT_B").grant(virtualSchema, SELECT);
        final String sql = "SELECT CITY FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        assertAll(() -> assertThat(queryForUser(sql, userA), table().row("Eilsbrunn").row("Traunreut").matches()),
                () -> assertThat(queryForUser(sql, userB), table().row("Tennenlohe").matches()));
    }

    // [itest->dsn~null-values-in-role-ids-and-masks~1]
    @Test
    void testNullValueInRoleIsTreatedAsZero() throws SQLException {
        final ExasolSchema sourceSchema = factory.createSchema("NULL_IN_ROLE_SCHEMA");
        sourceSchema.createTable("FOODS", "FOOD", "VARCHAR(40)", "EXA_ROW_ROLES", "VARCHAR(128)") //
                .insert("meat", 2) //
                .insert("vegetable", 0) //
                .insert("fruit", null);
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("BEN", 10);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE_NULL", sourceSchema);
        final User user = factory.createLoginUser("BEN").grant(virtualSchema, SELECT);
        final String sql = "SELECT FOOD FROM " + virtualSchema.getFullyQualifiedName() + ".FOODS";
        assertThat(queryForUser(sql, user), table().row("meat").matches());
    }

    @Test
    void testUnprotectedTable() throws SQLException {
        final ExasolSchema sourceSchema = factory.createSchema("UNPROTECTED_SCHEMA");
        sourceSchema.createTable("FRUITS", "NAME", "VARCHAR(20)").insert("Apple").insert("Pear").insert("Orange");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_UNPROTECTED", sourceSchema);
        final User user = factory.createLoginUser("USER_FOR_UNPROTECTED_TABLE").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".FRUITS";
        assertThat(queryForUser(sql, user), table().row("Apple").row("Pear").row("Orange").matches());
    }

    @Test
    void testTablesHiddenThroughVirtualSchema() throws SQLException {
        final ExasolSchema sourceSchema = factory.createSchema("HIDDEN_TABLES_SCHEMA");
        final Table groupTable = sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)",
                "EXA_GROUP", "VARCHAR(128)");
        final Table userTable = sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)",
                "EXA_ROLE_MASK", "DECIMAL(20)");
        final VirtualSchema virtualSchema = installVirtualSchema("HIDDEN_TABLES_VS", sourceSchema);
        final User user = factory.createLoginUser("USER_FOR_HIDDEN_TABLE_CHECK").grant(virtualSchema, SELECT);
        assertUserCanNotAccessTables(user, userTable, groupTable);
    }

    private void assertUserCanNotAccessTables(final User user, final Table... tables) throws SQLException {
        final Connection rlsConnection = container.createConnectionForUser(user.getName(), user.getPassword());
        final List<Table> hiddenTables = new ArrayList<>();
        for (final Table table : tables) {
            try {
                final Statement statement = rlsConnection.createStatement();
                statement.executeQuery("SELECT * FROM " + table.getName());
            } catch (final SQLException exception) {
                hiddenTables.add(table);
            }
        }
        if (!hiddenTables.containsAll(Arrays.asList(tables))) {
            throw new AssertionError(
                    "Excepted tables to be hidden: " + tables + ". But the following were: " + hiddenTables);
        }
    }

    // [itest->dsn~query-rewriter-adds-row-filter-for-roles~1]
    // [itest->dsn~public-access-role-id~1]
    @Test
    void testFilterAttached() throws SQLException {
        final ExasolSchema sourceSchema = factory.createSchema("ROLES_FILTER_SCHEMA");
        final Table sourceTable = sourceSchema.createTable("CITIES", "CITY", "VARCHAR(40)", "EXA_ROW_ROLES",
                "VARCHAR(128)");
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("ULF", 0);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE_FILTER", sourceSchema);
        final User user = factory.createLoginUser("ULF") //
                .grant(virtualSchema, SELECT) //
                .grant(sourceSchema, SELECT);
        final String virtualTableName = virtualSchema.getName() + "." + sourceTable.getName();
        try (final ResultSet result = queryForUser(
                "SELECT PUSHDOWN_SQL FROM (EXPLAIN VIRTUAL SELECT * FROM " + virtualTableName + ")", user)) {
            result.next();
            final String pushdownSQL = result.getString(1);
            assertThat(pushdownSQL, containsString("WHERE BIT_AND(\"EXA_ROW_ROLES\", "
                    + Long.toUnsignedString(RowLevelSecurityDialectConstants.DEFAULT_ROLE_MASK) + ") <> 0"));
        }
    }
}