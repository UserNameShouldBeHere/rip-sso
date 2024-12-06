package nas.sso.model;

public class TokenPayload {
    private boolean isTokenValid;
    private String uuid;
    private String username;

    public TokenPayload(final boolean isTokenValid) {
        this.isTokenValid = isTokenValid;
    }

    public TokenPayload(final boolean isTokenValid, final String uuid, final String username) {
        this.isTokenValid = isTokenValid;
        this.uuid = uuid;
        this.username = username;
    }

    public boolean isTokenValid() {
        return this.isTokenValid;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }
}
