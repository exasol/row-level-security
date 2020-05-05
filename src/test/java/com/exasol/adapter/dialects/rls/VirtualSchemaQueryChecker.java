package com.exasol.adapter.dialects.rls;

import java.sql.*;
import java.util.*;

import com.exasol.containers.ExasolContainer;
import com.exasol.dbbuilder.Table;
import com.exasol.dbbuilder.User;

public final class VirtualSchemaQueryChecker {
    private final ExasolContainer<? extends ExasolContainer<?>> container;

    public VirtualSchemaQueryChecker(final ExasolContainer<? extends ExasolContainer<?>> container) {
        this.container = container;
    }

    public void assertUserQuery(final User user, final String sql, final Object[][] expectedResults) {
        final Connection rlsConnection = logIn(user);
        assertQueryResults(rlsConnection, sql, expectedResults);
    }

    private Connection logIn(final User user) {
        try {
            return this.container.createConnectionForUser(user.getName(), user.getPassword());
        } catch (final SQLException exception) {
            throw new AssertionError("Unable to prepare test. Logging in user \"" + user.getName() + "\" failed.",
                    exception);
        }
    }

    private void assertQueryResults(final Connection connection, final String sql, final Object[][] expectedResults) {
        try (final Statement statement = connection.createStatement()) {
            final ResultSet result = statement.executeQuery(sql);
            assertColumnCount(result, expectedResults);
            assertRowsMatch(expectedResults, result);
        } catch (final SQLException exception) {
            throw new AssertionError("Unable to execute statement to assert query result: " + sql, exception);
        }
    }

    private void assertRowsMatch(final Object[][] expectedResults, final ResultSet result)
            throws SQLException, AssertionError {
        int rowIndex = 1;
        for (final Object[] expectedRow : expectedResults) {
            if (result.next()) {
                assertValuesInRowMatch(result, rowIndex, expectedRow);
                ++rowIndex;
            } else {
                throw new AssertionError(
                        "Expected result set ot have " + expectedResults + " rows, but it only had " + rowIndex + ".");
            }
        }
        if (result.next()) {
            throw new AssertionError("Result table has more than the expected " + expectedResults.length + "rows.");
        }
    }

    private void assertValuesInRowMatch(final ResultSet result, final int rowIndex, final Object[] expectedRow)
            throws SQLException, AssertionError {
        int columnIndex = 1;
        for (final Object expectedValue : expectedRow) {
            final Object value = result.getObject(columnIndex);
            if (!value.equals(expectedValue)) {
                throw new AssertionError("Result deviates in row " + rowIndex + ", column " + columnIndex
                        + ". Expected: '" + expectedValue + "' But was: '" + value + "'.");
            }
            ++columnIndex;
        }
    }

    private void assertColumnCount(final ResultSet result, final Object[][] expectedResults)
            throws SQLException, AssertionError {
        final int resultColumnCount = result.getMetaData().getColumnCount();
        final int expectedColumnCount = expectedResults[0].length;
        if (resultColumnCount != expectedColumnCount) {
            throw new AssertionError(
                    "Result set has " + resultColumnCount + " columns, but " + expectedColumnCount + " were expected.");
        }
    }

    public void assertUserCanNotAccessTables(final User user, final Table... tables) {
        final Connection rlsConnection = logIn(user);
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
}