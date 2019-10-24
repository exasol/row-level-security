package com.exasol.adapter.sql;

import com.exasol.adapter.jdbc.RemoteMetadataReaderException;

import java.sql.*;

public class UserInformation {
    public int getRoleMask(final Connection connection) {
        final String rlsUsersTableName = "rls_users";
        final String query = "SELECT exa_role_mask FROM " + rlsUsersTableName + " WHERE exa_user_name = CURRENT_USER";
        try (final ResultSet resultSet = connection.prepareStatement(query).executeQuery()) {
            return resultSet.getInt("exa_role_mask");
        } catch (final SQLException exception) {
            throw new RemoteMetadataReaderException(
                  "Unable to read role mask from " + rlsUsersTableName + ". Caused by: " + exception.getMessage(),
                  exception);
        }
    }
}
