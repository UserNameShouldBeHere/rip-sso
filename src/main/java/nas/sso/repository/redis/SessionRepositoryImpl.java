package nas.sso.repository.redis;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import nas.sso.exception.InvalidSessionException;
import nas.sso.model.TokenPayload;
import nas.sso.model.UserSession;
import nas.sso.repository.SessionRepository;
import redis.clients.jedis.UnifiedJedis;

@Component
public class SessionRepositoryImpl implements SessionRepository {
    private UnifiedJedis redis;
    private SecretKey key;
    private int sessionExpiration;

    public SessionRepositoryImpl(final int sessionExpiration) {
        redis = new UnifiedJedis("redis://localhost:6379");

        this.key = Jwts.SIG.HS256.key().build();
        this.sessionExpiration = sessionExpiration;
    }

    public SessionRepositoryImpl(final String host, final String port, final int sessionExpiration) {
        redis = new UnifiedJedis(String.format("redis://%s:%s", host, port));

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
    public boolean removeSession(final String token) throws InvalidSessionException {
        Jws<Claims> claims;
        try {
            claims = validateToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidSessionException(e.getMessage());
        }

        long res = redis.srem(claims.getPayload().get("username").toString(), token);

        return res > 0;
    }

    @Override
    public boolean removeAllSessions(final String token) throws InvalidSessionException {
        Jws<Claims> claims;
        try {
            claims = validateToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidSessionException(e.getMessage());
        }

        if (!this.redis.sismember(claims.getPayload().get("username").toString(), token)) {
            return false;
        } else {
            long res = redis.del(claims.getPayload().get("username").toString());

            return res > 0;
        }
    }

    @Override
    public TokenPayload check(final String token) throws InvalidSessionException {
        try {
            Jws<Claims> claims = validateToken(token);
            
            TokenPayload tokenPayload = new TokenPayload(
                this.redis.sismember(claims.getPayload().get("username").toString(), token),
                claims.getPayload().get("uuid").toString(),
                claims.getPayload().get("username").toString()
            );

            return tokenPayload;
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidSessionException(e.getMessage());
        }
    }

    private Jws<Claims> validateToken(final String token) throws JwtException, IllegalArgumentException {
        return Jwts.parser()
            .verifyWith(this.key)
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
            .expiration(Date.from(Instant.now().plusSeconds(this.sessionExpiration)))
            .signWith(this.key)
            .compact();

        return jwt;
    }
}
