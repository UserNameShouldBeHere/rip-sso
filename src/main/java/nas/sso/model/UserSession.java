package nas.sso.model;

public class UserSession {
    private String uuid;
    private String userName;
    private int version;

    public UserSession(final String uuid, final String userName, final int version) {
        this.uuid = uuid;
        this.userName = userName;
        this.version = version;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String getUserName() {
        return this.userName;
    }

    public int getVersion() {
        return this.version;
    }
}
