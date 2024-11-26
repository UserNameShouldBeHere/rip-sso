package nas.sso.repository.postgres;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.stereotype.Component;

import nas.sso.exception.PasswordHashException;
import nas.sso.repository.AuthRepository;

@Component
public class AuthRepositoryImpl implements AuthRepository {
    private DataSource dataSource;
    private final int saltLength = 16;

    public AuthRepositoryImpl() {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl("jdbc:postgresql://localhost:5432/auth?user=postgres&password=postgres");
        this.dataSource = pgDataSource;
    }

    public AuthRepositoryImpl(final String host, final String port, final String db, final String user, final String password) {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl(String.format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s", 
            host, port, db, user, password));
        this.dataSource = pgDataSource;
    }

    @Override
    public String createUser(final String name, final String password) throws SQLException, PasswordHashException {
        Connection conn = null;
        ResultSet res = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            String passwordHash = genPasswordHash(password);

            PreparedStatement query = conn.prepareStatement("insert into users (name, password) values (?, ?) returning external_uuid");
            query.setString(1, name);
            query.setString(2, passwordHash);
            res = query.executeQuery();
            
            conn.commit();

            res.next();

            return res.getString("external_uuid");
        } catch (SQLException | PasswordHashException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
    }

    @Override
    public boolean hasUser(String name) {
        Connection conn = null;
        ResultSet res = null;
        try {
            conn = this.dataSource.getConnection();

            PreparedStatement query = conn.prepareStatement("select name from users where name = ?");
            query.setString(1, name);
            res = query.executeQuery();

            res.next();

            conn.close();
            
            return res.getString("name").equals(name);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean checkPassword(String name, String givenPassword) throws SQLException, PasswordHashException {
        Connection conn = null;
        ResultSet res = null;
        try {
            conn = dataSource.getConnection();

            PreparedStatement query = conn.prepareStatement("select password from users where name = ?");
            query.setString(1, name);
            res = query.executeQuery();

            res.next();

            conn.close();
            
            return validatePassword(givenPassword, res.getString("password"));
        } catch (SQLException e) {
            throw e;
        }
    }

    @Override
    public String getUserUuid(String name) throws SQLException {
        Connection conn = null;
        ResultSet res = null;
        try {
            conn = dataSource.getConnection();

            PreparedStatement query = conn.prepareStatement("select external_uuid from users where name = ?");
            query.setString(1, name);
            res = query.executeQuery();

            res.next();

            conn.close();
            
            return res.getString("external_uuid");
        } catch (SQLException e) {
            throw e;
        }
    }

    public String genPasswordHash(final String password) throws PasswordHashException {
        HexFormat commaFormat = HexFormat.of();

        byte[] salt = null;
        byte[] hash = null;

        try {
            salt = createSalt();
            hash = createPBEHash(password, salt, 64);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new PasswordHashException(e.getMessage());
        }

        return commaFormat.formatHex(salt) + commaFormat.formatHex(hash);
    }

    private byte[] createSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

        byte[] salt = new byte[saltLength];

        sr.nextBytes(salt);

        return salt;
    }

    private byte[] createPBEHash(final String password, final byte[] salt, final int keyLength)
        throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 500, keyLength * 8);

        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        return skf.generateSecret(spec).getEncoded();
    }

    private boolean validatePassword(final String givenPassword, final String expectedPassword) throws PasswordHashException {
        HexFormat commaFormat = HexFormat.of();

        byte[] salt = commaFormat.parseHex(expectedPassword.substring(0, saltLength*2));
        byte[] hash = null;

        try {
            hash = createPBEHash(givenPassword, salt, 64);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new PasswordHashException(e.getMessage());
        }

        String result = commaFormat.formatHex(salt) + commaFormat.formatHex(hash);

        return expectedPassword.equals(result);
    }
}
