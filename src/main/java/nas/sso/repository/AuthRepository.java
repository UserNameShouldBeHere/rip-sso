package nas.sso.repository;

import java.sql.SQLException;

import nas.sso.exception.PasswordHashException;

public interface AuthRepository {
    String createUser(final String name, final String password) throws SQLException, PasswordHashException;
    boolean hasUser(final String name);
    boolean checkPassword(final String name, final String givenPassword) throws SQLException, PasswordHashException;
    String getUserUuid(final String name) throws SQLException;
    // void removeUser
}
