package com.exasol.adapter.sql;

import static com.exasol.adapter.dialects.rls.RowLevelSecurityDialectConstants.*;
import static java.lang.Long.toUnsignedString;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.exasol.adapter.jdbc.ConnectionFactory;
import com.exasol.adapter.jdbc.RemoteMetadataReaderException;
import com.exasol.db.Identifier;

/**
 * This class collect information about a user's roles.
 */
public final class UserInformation {
    private static final Logger LOGGER = Logger.getLogger(UserInformation.class.getName());
    private final Identifier schema;
    private final Identifier currentUser;
    private final ConnectionFactory connectionFactory;
    private String cachedRoleMask = null;
    private List<String> cachedGroups = null;
    private Connection sharedConnection = null;

    /**
     * Create a new instance of {@link UserInformation}
     *
     * @param currentUser       current user identifier
     * @param schema            schema identifier
     * @param connectionFactory factory for JDBC database connections
     */
    public UserInformation(final Identifier currentUser, final Identifier schema,
            final ConnectionFactory connectionFactory) {
        this.schema = schema;
        this.currentUser = currentUser;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Get current user.
     *
     * @return current session's user
     */
    public Identifier getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Get user's role mask.
     *
     * @return role mask as a String
     */
    @SuppressWarnings("java:S2077") // SQL injection via schema prevented by wrapping schema in validated Identifier
    // object.
    public synchronized String getRoleMask() {
        if (this.cachedRoleMask == null) {
            final String query = "SELECT EXA_ROLE_MASK FROM " + this.schema + "." + EXA_RLS_USERS_TABLE_NAME
                    + " WHERE EXA_USER_NAME = ?";
            try (final PreparedStatement statement = getConnection().prepareStatement(query)) {
                statement.setString(1, this.currentUser.toString());
                try (final ResultSet resultSet = statement.executeQuery()) {
                    final long mask = setUserMask(resultSet);
                    this.cachedRoleMask = toUnsignedString(mask);

                }
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
    @SuppressWarnings("java:S2077") // SQL injection via schema prevented by wrapping schema in validated Identifier
                                    // object.
    public synchronized List<String> getGroups() throws SQLException {
        if (this.cachedGroups == null) {
            final String sql = "SELECT \"EXA_GROUP\" FROM \"" + this.schema + "\".\"" + EXA_GROUP_MEMBERS_TABLE_NAME
                    + "\" WHERE \"EXA_USER_NAME\" = ?";
            try (final PreparedStatement statement = this.getConnection().prepareStatement(sql)) {
                statement.setString(1, this.currentUser.toString());
                try (final ResultSet result = statement.executeQuery()) {
                    this.cachedGroups = new ArrayList<>();
                    boolean foundIllegalGroupName = false;
                    while (result.next()) {
                        final String group = result.getString(1);
                        if ((group == null) || group.isBlank()) {
                            foundIllegalGroupName = true;
                        } else {
                            this.cachedGroups.add(group);
                        }
                    }
                    if (foundIllegalGroupName) {
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