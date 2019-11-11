package com.exasol.adapter.sql;

import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

import java.math.BigInteger;
import java.sql.*;
import java.util.logging.Logger;

import static java.lang.Long.toUnsignedString;

/**
 * This class collect information about a user's roles.
 */
public class UserInformation {
    private static final Logger LOGGER = Logger.getLogger(UserInformation.class.getName());
    private static final long MAX_ROLE_VALUE =
          BigInteger.valueOf(2).pow(63).subtract(BigInteger.valueOf(1)).longValue();
    private static final long DEFAULT_ROLE_MASK = BigInteger.valueOf(2).pow(63).longValue();
    private final String rlsUsersTableName;
    private final String schemaName;
    private final String currentUser;

    public UserInformation(final String currentUser, final String schemaName, final String rlsUsersTableName) {
        this.schemaName = schemaName;
        this.rlsUsersTableName = rlsUsersTableName;
        this.currentUser = currentUser;
    }

    /**
     * Get user's role mask.
     *
     * @param connection a connection to Exasol
     * @return role mask as a String
     */
    public String getRoleMask(final Connection connection) {
        LOGGER.info(() -> "Current user: " + this.currentUser);
        final String query =
              "SELECT EXA_ROLE_MASK FROM " + this.schemaName + "." + this.rlsUsersTableName + " WHERE EXA_USER_NAME = '"
                    + this.currentUser + "'";
        try (final ResultSet resultSet = connection.prepareStatement(query).executeQuery()) {
            final long mask = setUserMask(resultSet);
            return toUnsignedString(mask);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                  "Unable to read role mask from " + this.rlsUsersTableName + ". Caused by: " + exception.getMessage(),
                  exception);
        }
    }

    private long setUserMask(final ResultSet resultSet) throws SQLException {
        if (resultSet != null && resultSet.next()) {
            final long exaRoleMask = resultSet.getLong("exa_role_mask");
            if (validateExaRoleMask(resultSet, exaRoleMask)) {
                return setPublicAccess(exaRoleMask);
            } else {
                return DEFAULT_ROLE_MASK;
            }
        } else {
            LOGGER.warning(() -> "Role mask for current user was not found in table " + this.rlsUsersTableName
                  + ". The role will be set to the default.");
            return DEFAULT_ROLE_MASK;
        }
    }

    private static long setPublicAccess(final long exaRoleMask) {
        return exaRoleMask | BigInteger.valueOf(2).pow(63).longValue();
    }

    private boolean validateExaRoleMask(final ResultSet resultSet, final long exaRoleMask) throws SQLException {
        return returnsOnlyOneResult(resultSet) && maskIsInAllowedRange(exaRoleMask);
    }

    private boolean returnsOnlyOneResult(final ResultSet resultSet) throws SQLException {
        final boolean isLast = resultSet.last();
        if (!isLast) {
            LOGGER.warning(() -> "Role mask for current user was not found in table " + this.rlsUsersTableName
                  + ". The role will be set to the default.");
        }
        return isLast;
    }

    private boolean maskIsInAllowedRange(final long exaRoleMask) {
        final boolean isInRange = exaRoleMask <= MAX_ROLE_VALUE && exaRoleMask >= 0;
        if (!isInRange) {
            LOGGER.warning(() -> "Role mask for current user from table " + this.rlsUsersTableName
                  + " exceeded allowed limit. Allowed limit: " + MAX_ROLE_VALUE + ", user mask:" + exaRoleMask
                  + ". The role will be set to the default.");
        }
        return isInRange;
    }
}