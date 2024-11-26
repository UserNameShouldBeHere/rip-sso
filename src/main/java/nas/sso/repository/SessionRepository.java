package nas.sso.repository;

import nas.sso.model.UserSession;

public interface SessionRepository {
    String createSession(final UserSession session);
    void removeSession(final String userName, final String token);
    void removeSessions(final String userName);
    boolean check(final String token);
}
