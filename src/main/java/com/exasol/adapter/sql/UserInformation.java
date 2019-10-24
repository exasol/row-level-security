package com.exasol.adapter.sql;

import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

import java.sql.*;

public class UserInformation {
    public int getRoleMask(final Connection connection) {
        final String rlsUsersTableName = "rls_users";
        final String query = "SELECT exa_role_mask FROM " + rlsUsersTableName + " WHERE exa_user_name = CURRENT_USER";
        try (final PreparedStatement statement = connection.prepareStatement(query)) {
            final ResultSet resultSet = statement.executeQuery();
            final int exaRoleMask = resultSet.getInt("exa_role_mask");
            return exaRoleMask;
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                  "Unable to read role mask from " + rlsUsersTableName + ". Caused by: " + exception.getMessage(),
                  exception);
        }
    }
}
