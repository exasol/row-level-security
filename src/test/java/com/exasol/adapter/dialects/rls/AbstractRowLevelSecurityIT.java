package com.exasol.adapter.dialects.rls;

import static com.exasol.adapter.dialects.rls.DBHelper.exasolVersionSupportsFingerprintInAddress;
import static com.exasol.dbbuilder.dialects.exasol.ExasolObjectPrivilege.SELECT;
import static com.exasol.matcher.ResultSetStructureMatcher.table;
import static com.exasol.matcher.TypeMatchMode.NO_JAVA_TYPE_CHECK;
import static com.exasol.tools.TestsConstants.ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
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
import com.exasol.udfdebugging.UdfTestSetup;

@Tag("integration")
@Tag("virtual-schema")
@Tag("slow")
@Testcontainers
abstract class AbstractRowLevelSecurityIT {
    @Container
    protected static final ExasolContainer<? extends ExasolContainer<?>> EXASOL = new ExasolContainer<>()
            .withReuse(true);
    private static AdapterScript adapterScript = null;
    private static ConnectionDefinition connectionDefinition = null;
    protected static ExasolObjectFactory objectFactory = null;

    /**
     * Define the properties with which the Virtual Schema under test is created.
     * <p>
     * All derived classes must implement this in order to parameterize the execution of the test cases listed in this
     * abstract class.
     * </p>
     *
     * @return properties with which the Virtual Schema is created
     */
    protected abstract Map<String, String> getConnectionSpecificVirtualSchemaProperties();

    @BeforeAll
    static void beforeAll() throws SQLException, BucketAccessException, InterruptedException, TimeoutException {
        //seems that database reuse/purge isn't called until after execution of all derived test classes, we thus fix it this way
        EXASOL.purgeDatabase();
        final UdfTestSetup udfTestSetup = new UdfTestSetup(EXASOL.getHostIp(), EXASOL.getDefaultBucket());
        objectFactory = new ExasolObjectFactory(EXASOL.createConnection(""),
                ExasolObjectConfiguration.builder().withJvmOptions(udfTestSetup.getJvmOptions()).build());
        uploadAdapterScript();
        registerAdapterScript();
        createConnectionDefinition();
    }

    private static void uploadAdapterScript() {
        final String adapterScriptPath = "target/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION;
        try {
            final Bucket bucket = EXASOL.getDefaultBucket();
            final Path pathToRls = Path.of(adapterScriptPath);
            bucket.uploadFile(pathToRls, ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION);
        } catch (final BucketAccessException | TimeoutException | FileNotFoundException exception) {
            throw new AssertionError(
                    "Unable to prepare test: upload of adapter script \"" + adapterScriptPath + " failed.", exception);
        }
    }

    private static void registerAdapterScript() {
        final ExasolSchema schema = objectFactory.createSchema("SCHEMA_FOR_RLS_ADAPTER_SCRIPT");
        final String content = "%scriptclass com.exasol.adapter.RequestDispatcher;\n" //
                + "%jar /buckets/bfsdefault/default/" + ROW_LEVEL_SECURITY_JAR_NAME_AND_VERSION + ";\n";
        adapterScript = schema.createAdapterScript("RLS_ADAPTER", AdapterScript.Language.JAVA, content);
    }

    private static void createConnectionDefinition() {
        connectionDefinition = objectFactory.createConnectionDefinition("RLS_CONNECTION", getJdbcUrl(),
                EXASOL.getUsername(), EXASOL.getPassword());
    }

    private static String getJdbcUrl() {
        final int port = EXASOL.getDefaultInternalDatabasePort();
        if (exasolVersionSupportsFingerprintInAddress(EXASOL.getDockerImageReference())) {
            final String fingerprint = EXASOL.getTlsCertificateFingerprint().get();
            return "jdbc:exa:localhost:" + port + ";validateservercertificate=1;fingerprint=" + fingerprint;
        }
        return "jdbc:exa:localhost:" + port + ";validateservercertificate=0";
    }

    // [itest->dsn~query-rewriter-adds-row-filter-for-tenants~1]
    @Test
    void testTenantRestrictedTable() {
        final ExasolSchema sourceSchema = objectFactory.createSchema("TENANT_PROTECTED_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_TENANT", "VARCHAR(128)") //
                .insert("Paris", "USER_T_A") //
                .insert("New York", "USER_T_A") //
                .insert("Rio", "USER_T_B");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_TENANT", sourceSchema);
        final User userA = objectFactory.createLoginUser("USER_T_A").grant(virtualSchema, SELECT);
        final User userB = objectFactory.createLoginUser("USER_T_B").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE";
        assertAll(() -> assertThat(queryForUser(sql, userA), table().row("Paris").row("New York").matches()),
                () -> assertThat(queryForUser(sql, userB), table().row("Rio").matches()));
    }

    private ResultSet queryForUser(final String sql, final User user) throws SQLException {
        try (final Connection connection = EXASOL.createConnectionForUser(user.getName(), user.getPassword());
             final Statement statement = connection.createStatement();
             final ResultSet result = statement.executeQuery(sql)) {
            return result;
        }
    }

    private VirtualSchema installVirtualSchema(final String name, final ExasolSchema sourceSchema) {
        return objectFactory.createVirtualSchemaBuilder(name) //
                .adapterScript(adapterScript) //
                .connectionDefinition(connectionDefinition) //
                .properties(getConnectionSpecificVirtualSchemaProperties()) //
                .sourceSchema(sourceSchema) //
                .build();
    }

    // [itest->dsn~query-rewriter-adds-row-filter-for-group~1]
    @Test
    void testGroupRestictedTable() throws SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("GROUP_PROTECTED_SCHEMA");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)") //
                .insert("Stockholm", "COLD") //
                .insert("Moskow", "COLD") //
                .insert("Horta", "MODERATE") //
                .insert("Rio", "HOT");
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_G", "COLD") //
                .insert("USER_G", "MODERATE");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_G").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        assertThat(queryForUser(sql, user), table().row("Horta").row("Moskow").row("Stockholm").matches());
    }

    // [itest->dsn~query-rewriter-adds-row-filter-for-group~1]
    @Test
    void testGroupRestictedTableWithSingleGroupOptimization() throws SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("GROUP_PROTECTED_SCHEMA_WITH_ONE_GROUP");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)") //
                .insert("Stockholm", "ANOTHER_GROUP") //
                .insert("Rio", "THE_GROUP");
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_WITH_SINGLE_GROUP", "THE_GROUP");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_SINGLE_GROUP", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_WITH_SINGLE_GROUP").grant(virtualSchema, SELECT);
        final String sql = "EXPLAIN VIRTUAL SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE";
        assertThat(queryForUser(sql, user), table().row(anything(),
                // Note that depending on whether this is a local or a remote virtual schema, we either expect a
                // standalone SELECT statement or one wrapped into an IMPORT statement. That is why we match
                // against a regular expression here.
                matchesPattern(
                        ".*SELECT \"SOURCE_TABLE\".\"CITY\" FROM \"GROUP_PROTECTED_SCHEMA_WITH_ONE_GROUP\".\"SOURCE_TABLE\"" //
                                + " WHERE \"EXA_ROW_GROUP\" = ''?THE_GROUP'.*"),
                anything(), anything()).matches());
    }

    // [itest->dsn~query-rewriter-treats-protected-tables-with-group-and-tenant-restrictions~1]
    @Test
    void testGroupAndTenantRestrictedTable() throws SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("GROUP_AND_TENANT_PROTECTED_SCHEMA");
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
        final User userTenantOnly = objectFactory.createLoginUser("USER_GT_T").grant(virtualSchema, SELECT);
        final User userGroupOnly = objectFactory.createLoginUser("USER_GT_G").grant(virtualSchema, SELECT);
        final User userGroupAndTenant = objectFactory.createLoginUser("USER_GT_GT").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        final Builder expectedForTenantOnlyUser = table().row("Horta").row("Stockholm");
        final Builder expectedForGroupOnlyUser = table().row("Helsinki").row("Horta").row("Lhasa").row("Moskow")
                .row("Stockholm");
        final Builder expectedForGroupAndTenantUser = table().row("Lhasa").row("Rio");
        assertAll(() -> assertThat(queryForUser(sql, userTenantOnly), expectedForTenantOnlyUser.matches()),
                () -> assertThat(queryForUser(sql, userGroupOnly), expectedForGroupOnlyUser.matches()),
                () -> assertThat(queryForUser(sql, userGroupAndTenant), expectedForGroupAndTenantUser.matches()));
    }

    @Test
    void testGroupRestrictedTableWithUnauthorizedUser() {
        final ExasolSchema sourceSchema = objectFactory.createSchema("GROUP_PROTECTED_SCHEMA_UNAUTHORIZED_USER");
        sourceSchema.createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)") //
                .insert("Rio", "HOT");
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_G", "HOT");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP_UNAUTHORIZED_USER", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_NA").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        final SQLDataException exception = assertThrows(SQLDataException.class, () -> queryForUser(sql, user));
        assertThat(exception.getMessage(), containsString("E-VSRLS-JAVA-7"));
    }

    @Test
    void testGroupAndRoleRestrictedTable() {
        final ExasolSchema sourceSchema = objectFactory.createSchema("GROUP_AND_ROLE_PROTECTED_SCHEMA");
        sourceSchema
                .createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)", "EXA_ROW_ROLES",
                        "VARCHAR(128)") //
                .insert("Stockholm", "COLD", 1) //
                .insert("Moskow", "COLD", 3) //
                .insert("Horta", "MODERATE", 2);
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_GR", "COLD");
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("USER_GR", 1);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP_AND_ROLE", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_GR").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        final SQLDataException exception = assertThrows(SQLDataException.class, () -> queryForUser(sql, user));
        assertThat(exception.getMessage(), containsString("E-VSRLS-JAVA-8"));
    }

    @Test
    void testGroupAndRoleRestrictedTableWithWhereClause() {
        final ExasolSchema sourceSchema = objectFactory.createSchema("GROUP_AND_ROLE_PROTECTED_SCHEMA_2");
        sourceSchema
                .createTable("SOURCE_TABLE", "CITY", "VARCHAR(40)", "EXA_ROW_GROUP", "VARCHAR(128)", "EXA_ROW_ROLES",
                        "VARCHAR(128)") //
                .insert("Stockholm", "COLD", 1) //
                .insert("Moskow", "COLD", 3) //
                .insert("Horta", "MODERATE", 2);
        sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_GROUP", "VARCHAR(128)") //
                .insert("USER_GR_2", "COLD");
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("USER_GR_2", 1);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_GROUP_AND_ROLE_2", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_GR_2").grant(virtualSchema, SELECT);
        final String sqlWithWhereClause = "SELECT * FROM " + virtualSchema.getFullyQualifiedName()
                + ".SOURCE_TABLE WHERE CITY = 'Stockholm' ORDER BY CITY";
        final SQLDataException exception = assertThrows(SQLDataException.class,
                () -> queryForUser(sqlWithWhereClause, user));
        assertThat(exception.getMessage(), containsString("E-VSRLS-JAVA-8"));
    }

    // [itest->dsn~all-users-have-the-public-access-role~1]
    @Test
    void testRoleRestrictedTable() {
        final ExasolSchema sourceSchema = objectFactory.createSchema("ROLE_PROTECTED_SCHEMA");
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
        final User userA = objectFactory.createLoginUser("USER_R_A").grant(virtualSchema, SELECT);
        final User userB = objectFactory.createLoginUser("USER_R_B").grant(virtualSchema, SELECT);
        final User userPublic = objectFactory.createLoginUser("USER_R_PUBLIC").grant(virtualSchema, SELECT);
        final String sql = "SELECT ZIP FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY ZIP";
        assertAll(
                () -> assertThat(queryForUser(sql, userA),
                        table().row(83301).row(83334).row(93161).matches(NO_JAVA_TYPE_CHECK)),
                () -> assertThat(queryForUser(sql, userB),
                        table().row(83334).row(90411).row(93161).matches(NO_JAVA_TYPE_CHECK)),
                () -> assertThat(queryForUser(sql, userPublic), table().row(93161).matches(NO_JAVA_TYPE_CHECK)));
    }

    // [itest->dsn~query-rewriter-treats-protected-tables-with-roles-and-tenant-restrictions~1]
    @Test
    void testRoleAndTenantRestrictedTable() {
        final ExasolSchema sourceSchema = objectFactory.createSchema("ROLE_AND_TENANT_PROTECTED_SCHEMA");
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
        final User userA = objectFactory.createLoginUser("USER_RT_A").grant(virtualSchema, SELECT);
        final User userB = objectFactory.createLoginUser("USER_RT_B").grant(virtualSchema, SELECT);
        final String sql = "SELECT CITY FROM " + virtualSchema.getFullyQualifiedName() + ".SOURCE_TABLE ORDER BY CITY";
        assertAll(() -> assertThat(queryForUser(sql, userA), table().row("Eilsbrunn").row("Traunreut").matches()),
                () -> assertThat(queryForUser(sql, userB), table().row("Tennenlohe").matches()));
    }

    // [itest->dsn~null-values-in-role-ids-and-masks~1]
    @Test
    void testNullValueInRoleIsTreatedAsZero() throws SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("NULL_IN_ROLE_SCHEMA");
        sourceSchema.createTable("FOODS", "FOOD", "VARCHAR(40)", "EXA_ROW_ROLES", "VARCHAR(128)") //
                .insert("meat", 2) //
                .insert("vegetable", 0) //
                .insert("fruit", null);
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("BEN", 10);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE_NULL", sourceSchema);
        final User user = objectFactory.createLoginUser("BEN").grant(virtualSchema, SELECT);
        final String sql = "SELECT FOOD FROM " + virtualSchema.getFullyQualifiedName() + ".FOODS";
        assertThat(queryForUser(sql, user), table().row("meat").matches());
    }

    @Test
    void testUnprotectedTable() throws SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("UNPROTECTED_SCHEMA");
        sourceSchema.createTable("FRUITS", "NAME", "VARCHAR(20)").insert("Apple").insert("Pear").insert("Orange");
        final VirtualSchema virtualSchema = installVirtualSchema("VS_UNPROTECTED", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_FOR_UNPROTECTED_TABLE").grant(virtualSchema, SELECT);
        final String sql = "SELECT * FROM " + virtualSchema.getFullyQualifiedName() + ".FRUITS";
        assertThat(queryForUser(sql, user), table().row("Apple").row("Pear").row("Orange").matches());
    }

    @Test
    void testTablesHiddenThroughVirtualSchema() throws SQLException {
        final ExasolSchema sourceSchema = objectFactory.createSchema("HIDDEN_TABLES_SCHEMA");
        final Table groupTable = sourceSchema.createTable("EXA_GROUP_MEMBERS", "EXA_USER_NAME", "VARCHAR(128)",
                "EXA_GROUP", "VARCHAR(128)");
        final Table userTable = sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)",
                "EXA_ROLE_MASK", "DECIMAL(20)");
        final VirtualSchema virtualSchema = installVirtualSchema("HIDDEN_TABLES_VS", sourceSchema);
        final User user = objectFactory.createLoginUser("USER_FOR_HIDDEN_TABLE_CHECK").grant(virtualSchema, SELECT);
        assertUserCanNotAccessTables(user, userTable, groupTable);
    }

    private void assertUserCanNotAccessTables(final User user, final Table... tables) throws SQLException {
        final Connection rlsConnection = EXASOL.createConnectionForUser(user.getName(), user.getPassword());
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
        final ExasolSchema sourceSchema = objectFactory.createSchema("ROLES_FILTER_SCHEMA");
        final Table sourceTable = sourceSchema.createTable("CITIES", "CITY", "VARCHAR(40)", "EXA_ROW_ROLES",
                "VARCHAR(128)");
        sourceSchema.createTable("EXA_RLS_USERS", "EXA_USER_NAME", "VARCHAR(128)", "EXA_ROLE_MASK", "DECIMAL(20)") //
                .insert("ULF", 0);
        final VirtualSchema virtualSchema = installVirtualSchema("VS_ROLE_FILTER", sourceSchema);
        final User user = objectFactory.createLoginUser("ULF") //
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