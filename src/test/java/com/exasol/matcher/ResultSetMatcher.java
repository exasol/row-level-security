package com.exasol.matcher;

import java.sql.*;

public final class ResultSetMatcher {
    public static void assertEqualResultSets(final ResultSet expectedResultSet, final ResultSet actualResultSet)
            throws SQLException {
        final ResultSetMetaData expectedMetadata = expectedResultSet.getMetaData();
        final ResultSetMetaData actualMetadata = actualResultSet.getMetaData();
        final int expectedColumnCount = expectedMetadata.getColumnCount();
        final int actualColumnCount = actualMetadata.getColumnCount();
        if (expectedColumnCount != actualColumnCount) {
            throw new AssertionError("Number of columns in expected and actual result sets does not match.");
        }
        int row = 1;
        boolean next;
        do {
            next = expectedResultSet.next();
            if (next != actualResultSet.next()) {
                throw new AssertionError("Number of rows in expected and actual result sets does not match.");
            }
            if (next) {
                for (int column = 1; column <= expectedColumnCount; ++column) {
                    final int resultSetType1 = expectedMetadata.getColumnType(column);
                    final int resultSetType2 = actualMetadata.getColumnType(column);
                    if (resultSetType1 == resultSetType2) {
                        switch (resultSetType1) {
                        case Types.BIGINT:
                            final int expectedInt = expectedResultSet.getInt(column);
                            final int actualInt = actualResultSet.getInt(column);
                            if (expectedInt != actualInt) {
                                throw new AssertionError("Data mismatch in row " + row + " column " + column
                                        + ". Expected: " + expectedInt + ". Actual: " + actualInt);
                            }
                            break;
                        case Types.VARCHAR:
                            final String expectedString = expectedResultSet.getString(column);
                            final String actualString = actualResultSet.getString(column);
                            if (!expectedString.equals(actualString)) {
                                throw new AssertionError("Data mismatch in row " + row + " column " + column
                                        + ". Expected: " + expectedString + ". Actual: " + actualString);
                            }
                            break;
                        default:
                            throw new AssertionError("Unknown data type.");
                        }
                    } else {
                        throw new AssertionError("Types does not match.");
                    }
                }
                if ((expectedResultSet.isLast() != actualResultSet.isLast())) {
                    throw new AssertionError("Number of rows in expected and actual result sets does not match.");
                }
                ++row;
            }
        } while (next);
    }
}
