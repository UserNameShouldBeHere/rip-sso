package nas.sso.repository.redis;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import nas.sso.model.UserSession;
import nas.sso.repository.SessionRepository;
import redis.clients.jedis.UnifiedJedis;

public class SessionRepositoryImpl implements SessionRepository {
    private UnifiedJedis redis;
    private SecretKey key;
    private int sessionExpiration;

    public SessionRepositoryImpl(final String connStr, final int sessionExpiration) {
        redis = new UnifiedJedis(connStr);

        this.key = Jwts.SIG.HS256.key().build();
        this.sessionExpiration = sessionExpiration;
    }

    @Override
    public String createSession(final UserSession session) {
        String token = createToken(session.getUuid(), session.getUserName(), session.getVersion());
        
        redis.sadd(session.getUserName(), token);

        return token;
    }

    @Override
    public void removeSession(String userName, String token) {
        redis.srem(userName, token);
    }

    @Override
    public void removeSessions(String userName) {
        redis.del(userName);
    }

    @Override
    public boolean check(final String token) {
        try {
            Jws<Claims> claims = validateToken(token);
            
            return this.redis.sismember(claims.getPayload().get("username").toString(), token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> validateToken(final String token) throws JwtException, IllegalArgumentException {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token);
    }

    private String createToken(final String uuid, final String userName, final int version) {
        String jwt = Jwts.builder()
            .issuer("sso")
            .claim("uuid", uuid)
            .claim("username", userName)
            .claim("version", version)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(sessionExpiration)))
            .signWith(key)
            .compact();

        return jwt;
    }
}
