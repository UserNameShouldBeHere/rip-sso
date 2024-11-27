package nas.sso.repository;

import nas.sso.exception.InvalidSessionException;
import nas.sso.model.UserSession;

public interface SessionRepository {
    String createSession(final UserSession session);
    boolean removeSession(final String token) throws InvalidSessionException;
    boolean removeAllSessions(final String token) throws InvalidSessionException;
    boolean check(final String token);
}
