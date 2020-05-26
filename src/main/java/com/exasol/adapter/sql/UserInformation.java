package com.exasol.adapter.sql;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.*;
import static java.lang.Long.toUnsignedString;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

/**
 * This class collect information about a user's roles.
 */
public final class UserInformation {
    private static final Logger LOGGER = Logger.getLogger(UserInformation.class.getName());
    private final String schemaName;
    private final String currentUser;
    private final ConnectionFactory connectionFactory;
    private String cachedRoleMask = null;
    private List<String> cachedGroups = null;
    private Connection sharedConnection = null;

    /**
     * Create a new instance of {@link UserInformation}
     *
     * @param currentUser       name of the current user
     * @param schemaName        mane of the schema
     * @param connectionFactory factory for JDBC database connections
     */
    public UserInformation(final String currentUser, final String schemaName,
            final ConnectionFactory connectionFactory) {
        this.schemaName = schemaName;
        this.currentUser = currentUser;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Get current user.
     *
     * @return current session's user
     */
    public String getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Get user's role mask.
     *
     * @return role mask as a String
     */
    public synchronized String getRoleMask() {
        if (this.cachedRoleMask == null) {
            final String query = "SELECT EXA_ROLE_MASK FROM " + this.schemaName + "." + EXA_RLS_USERS_TABLE_NAME
                    + " WHERE EXA_USER_NAME = '" + this.currentUser + "'";
            try (final ResultSet resultSet = getConnection().prepareStatement(query).executeQuery()) {
                final long mask = setUserMask(resultSet);
                this.cachedRoleMask = toUnsignedString(mask);
            } catch (final SQLException exception) {
                throw new RemoteMetadataReaderException("Unable to read role mask from " + EXA_RLS_USERS_TABLE_NAME
                        + ". Caused by: " + exception.getMessage(), exception);
            }
        }
        return this.cachedRoleMask;
    }

    private synchronized Connection getConnection() throws SQLException {
        if (this.sharedConnection == null) {
            this.sharedConnection = this.connectionFactory.getConnection();
        }
        return this.sharedConnection;
    }

    private long setUserMask(final ResultSet resultSet) throws SQLException {
        if ((resultSet != null) && resultSet.next()) {
            final long exaRoleMask = resultSet.getLong("exa_role_mask");
            if (validateExaRoleMask(resultSet, exaRoleMask)) {
                return setPublicAccess(exaRoleMask);
            } else {
                return DEFAULT_ROLE_MASK;
            }
        } else {
            LOGGER.fine(() -> "No role mask for current user found in table \"" + EXA_RLS_USERS_TABLE_NAME
                    + ". The role will be set to the default.");
            return DEFAULT_ROLE_MASK;
        }
    }

    private long setPublicAccess(final long exaRoleMask) {
        return exaRoleMask | DEFAULT_ROLE_MASK;
    }

    private boolean validateExaRoleMask(final ResultSet resultSet, final long exaRoleMask) throws SQLException {
        return returnsOnlyOneResult(resultSet) && maskIsInAllowedRange(exaRoleMask);
    }

    private boolean returnsOnlyOneResult(final ResultSet resultSet) throws SQLException {
        final boolean isLast = resultSet.last();
        if (!isLast) {
            LOGGER.fine(() -> "No role mask for current user found in table \"" + EXA_RLS_USERS_TABLE_NAME
                    + ". The role will be set to the default.");
        }
        return isLast;
    }

    private boolean maskIsInAllowedRange(final long exaRoleMask) {
        final boolean isInRange = (exaRoleMask <= MAX_ROLE_VALUE) && (exaRoleMask >= 0);
        if (!isInRange) {
            LOGGER.warning(() -> "Role mask for current user from table \"" + EXA_RLS_USERS_TABLE_NAME
                    + "\" exceeds allowed limit " + MAX_ROLE_VALUE + ". Treating user as if no roles were assigned.");
        }
        return isInRange;
    }

    /**
     * Get the list of groups the user is a member of.
     *
     * @return list of groups
     * @throws SQLException if group membership can't be read from database
     */
    public synchronized List<String> getGroups() throws SQLException {
        if (this.cachedGroups == null) {
            final String sql = "SELECT \"EXA_GROUP\" FROM \"" + this.schemaName + "\".\"" + EXA_GROUP_MEMBERS_TABLE_NAME
                    + "\" WHERE \"EXA_USER_NAME\"='" + this.currentUser + "'";
            try (final Statement statement = this.getConnection().createStatement()) {
                try (final ResultSet result = statement.executeQuery(sql)) {
                    this.cachedGroups = new ArrayList<>();
                    boolean badGroups = false;
                    while (result.next()) {
                        final String group = result.getString(1);
                        if ((group == null) || group.isBlank()) {
                            badGroups = true;
                        } else {
                            this.cachedGroups.add(group);
                        }
                    }
                    if (badGroups) {
                        LOGGER.warning(() -> "Ignored malformed groups of user \"" + this.currentUser
                                + "\". Please make sure that entries in " + EXA_GROUP_MEMBERS_TABLE_NAME
                                + " are neither NULL nor blank.");
                    }
                }
            }
        }
        return this.cachedGroups;
    }
}