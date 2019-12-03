package com.exasol.matcher;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public final class ResultSetMatcher {
    /**
     * This method compares two result sets. Currently supported data types for comparison: INTEGER and STRING.
     */
    public static void assertEqualResultSets(final ResultSet expectedResultSet, final ResultSet actualResultSet)
            throws SQLException {
        final int expectedColumnCount = expectedResultSet.getMetaData().getColumnCount();
        final int actualColumnCount = actualResultSet.getMetaData().getColumnCount();
        assertColumnCount(expectedColumnCount, actualColumnCount);
        int row = 1;
        boolean expectedNext;
        do {
            expectedNext = expectedResultSet.next();
            assertNumberOfRows(expectedNext, actualResultSet.next());
            assertNumberOfRows(expectedResultSet.isLast(), actualResultSet.isLast());
            if (expectedNext) {
                assertRow(expectedResultSet, actualResultSet, expectedColumnCount, row);
                ++row;
            }
        } while (expectedNext);
    }

    private static void assertRow(ResultSet expectedResultSet, ResultSet actualResultSet, int expectedColumnCount,
            int row) throws SQLException {
        for (int column = 1; column <= expectedColumnCount; ++column) {
            assertValue(expectedResultSet, actualResultSet, row, column);
        }
    }

    private static void assertValue(final ResultSet expectedResultSet, final ResultSet actualResultSet, final int row,
            final int column) throws SQLException {
        final int resultSetTypeExpected = expectedResultSet.getMetaData().getColumnType(column);
        final int resultSetTypeActual = actualResultSet.getMetaData().getColumnType(column);
        if (resultSetTypeExpected == resultSetTypeActual) {
            assertData(expectedResultSet, actualResultSet, row, column, resultSetTypeExpected);
        } else {
            throw new AssertionError("Expected and actual data types for column " + column + " do not match.");
        }
    }

    private static void assertData(final ResultSet expectedResultSet, final ResultSet actualResultSet, final int row,
            final int column, final int resultSetTypeExpected) throws SQLException {
        switch (resultSetTypeExpected) {
        case Types.BIGINT:
            assertInt(expectedResultSet, actualResultSet, row, column);
            break;
        case Types.VARCHAR:
            assertVarchar(expectedResultSet, actualResultSet, row, column);
            break;
        default:
            throw new AssertionError("Unknown data type.");
        }
    }

    private static void assertNumberOfRows(final boolean nextOrLastExpected, final boolean nextOrLastActual) {
        if (nextOrLastExpected != nextOrLastActual) {
            throw new AssertionError("Number of rows in expected and actual result sets does not match.");
        }
    }

    private static void assertColumnCount(final int expectedColumnCount, final int actualColumnCount) {
        if (expectedColumnCount != actualColumnCount) {
            throw new AssertionError("Number of columns in expected and actual result sets does not match.");
        }
    }

    private static void assertInt(final ResultSet expectedResultSet, final ResultSet actualResultSet, final int row,
            final int column) throws SQLException {
        final int expectedInt = expectedResultSet.getInt(column);
        final int actualInt = actualResultSet.getInt(column);
        if (expectedInt != actualInt) {
            throw new AssertionError("Integer data mismatch in row " + row + " column " + column + ". Expected: "
                    + expectedInt + ". Actual: " + actualInt);
        }
    }

    private static void assertVarchar(final ResultSet expectedResultSet, final ResultSet actualResultSet, final int row,
            final int column) throws SQLException {
        final String expectedString = expectedResultSet.getString(column);
        final String actualString = actualResultSet.getString(column);
        if (!expectedString.equals(actualString)) {
            throw new AssertionError("String data mismatch in row " + row + " column " + column + ". Expected: "
                    + expectedString + ". Actual: " + actualString);
        }
    }
}
