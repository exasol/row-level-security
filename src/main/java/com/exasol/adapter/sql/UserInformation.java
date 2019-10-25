package com.exasol.adapter.sql;

import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

import java.math.BigInteger;
import java.sql.*;
import java.util.logging.Logger;

/**
 * This class collect information about a user's roles.
 */
public class UserInformation {
    private static final Logger LOGGER = Logger.getLogger(UserInformation.class.getName());
    private static final long MAX_ROLE_VALUE = BigInteger.valueOf(2).pow(63).subtract(BigInteger.valueOf(1)).longValue();
    private static final long DEFAULT_ROLE_MASK = 0;
    private final String rlsUsersTableName;

    public UserInformation(final String rlsUsersTableName) {
        this.rlsUsersTableName = rlsUsersTableName;
    }

    /**
     * Get user's role mask.
     *
     * @param connection a connection to Exasol
     * @return role mask as an long
     */
    public long getRoleMask(final Connection connection) {
        final String query =
              "SELECT exa_role_mask FROM " + this.rlsUsersTableName + " WHERE exa_user_name = CURRENT_USER";
        try (final ResultSet resultSet = connection.prepareStatement(query).executeQuery()) {
            return setUserMask(resultSet);
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                  "Unable to read role mask from " + this.rlsUsersTableName + ". Caused by: " + exception.getMessage(),
                  exception);
        }
    }

    private long setUserMask(final ResultSet resultSet) throws SQLException {
        if (resultSet != null && resultSet.next()) {
            final long exa_role_mask = resultSet.getLong("exa_role_mask");
            if (validateExaRoleMask(resultSet, exa_role_mask)) {
                return exa_role_mask;
            } else {
                return DEFAULT_ROLE_MASK;
            }
        } else {
            LOGGER.warning(() -> "Role mask for current user was not found in table " + this.rlsUsersTableName
                  + ". The role will be set to the default.");
            return DEFAULT_ROLE_MASK;
        }
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