package com.exasol.rls.administration.scripts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import org.testcontainers.containers.JdbcDatabaseContainer.NoDriverFoundException;

import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.dialects.DatabaseObjectException;
import com.exasol.dbbuilder.dialects.exasol.*;

public abstract class AbstractAdminScriptIT {
    private static final String[] INVALID_IDENTIFIERS = { //
            null, //
            "", //
            "   ", //
            "1_STARTING_WITH_NUMBER", //
            "_STARTING_WITH_UNDERSCORE", //
            " LEADING_SPACE", //
            "TRAILING_SPACE ", //
            "CONTAINS SPACE", //
            "CONTAINS-DASH", //
            "$STARTS_WITH_SPECIAL_CHAR", //
            "ENDS_WITH_SPECIAL_CHAR$", //
            "CONTAINS\"DOUBLE_QUOTE", //
            "CONTAINS'SINGLE_QUOTE", //
            "TOO_LONG_" + "0123456789_".repeat(11) };
    protected static final String ALLOWED_IDENTIFIER_EXPLAINATION = //
            "Allowed identifiers are ASCII only, starting with a letter. " //
                    + "Optionally followed by letters, numbers or underscores. Up to 128 characters.";
    protected static ExasolSchema schema;
    protected static Script script;
    protected static ExasolObjectFactory factory;

    protected static void initialize(final ExasolContainer<? extends ExasolContainer<?>> container,
            final String scriptName, final Path... scriptFilePaths) throws SQLException {
        final Connection connection = container.createConnection("");
        factory = new ExasolObjectFactory(connection);
        schema = factory.createSchema(scriptName + "_SCHEMA");
        factory.executeSqlFile(scriptFilePaths);
        script = schema.getScript(scriptName);
    }

    protected abstract Connection getConnection() throws NoDriverFoundException, SQLException;

    protected void execute(final String sql) throws SQLException {
        getConnection().createStatement().execute(sql);
    }

    protected ResultSet query(final String sql) throws SQLException {
        return getConnection().createStatement().executeQuery(sql);
    }

    protected static void assertScriptThrows(final String expectedMessageFragment, final Object... parameters) {
        final Throwable exception = assertThrows(DatabaseObjectException.class, () -> script.execute(parameters));
        assertThat(exception.getCause().getMessage(), containsString(expectedMessageFragment));
    }

    /**
     * Create a stream of identifiers that RLS considers invalid.
     * <p>
     * In RLS only letters numbers and underscores are allowed as identifiers. This method produce a stream of illegal
     * identifiers and a corresponding mangled version that is expected to appear in the error messages.
     *
     * @return stream of tuples: invalid identifiers and mangled identifiers
     */
    protected static Stream<Arguments> produceInvalidIdentifiers() {
        return Stream.of(INVALID_IDENTIFIERS).map(id -> Arguments.of(id, quote(id)));
    }

    // Note that Exasol treats internally represents empty VARCHARS by NULL values, so you can't distinguish them.
    private static String quote(final String id) {
        if ((id == null) || id.isEmpty()) {
            return "<null>";
        } else {
            return "\"" + id + "\"";
        }
    }

    protected static Stream<Arguments> produceInvalidIdentifiersInList() {
        final List<Arguments> cases = new ArrayList<>();
        for (final String invalidIdentifier : INVALID_IDENTIFIERS) {
            cases.add(produceListContainingAnInvalidIdentifier(invalidIdentifier));
        }
        cases.add(produceInvalidIdentifierSortingCase());
        return cases.stream();
    }

    private static Arguments produceListContainingAnInvalidIdentifier(final String invalidIdentifier) {
        // We use Arrays.asList instead of List.of since this allows injecting null values too.
        final List<String> listWIthInvalidIdentifier = Arrays.asList("ID_A", invalidIdentifier, "ID_C");
        return Arguments.of(listWIthInvalidIdentifier, quote(invalidIdentifier));
    }

    private static Arguments produceInvalidIdentifierSortingCase() {
        return Arguments.of(List.of("AAA-", "ccc-", "ZZZ-", "bbb-"), "\"AAA-\", \"ZZZ-\", \"bbb-\", \"ccc-\"");
    }
}