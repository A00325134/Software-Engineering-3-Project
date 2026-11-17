package ie.newsagent.model;

public class DeliveryDriver {
    private final String driverId;
    private String name;
    /** In production, store a salted password hash, not the raw password. */
    private String passwordHash;

    public DeliveryDriver(String driverId, String name, String passwordHash) {
        this.driverId = driverId; this.name = name; this.passwordHash = passwordHash;
    }
    public String getDriverId() { return driverId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
