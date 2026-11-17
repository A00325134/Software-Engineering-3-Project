package ie.newsagent.model;

public class Subscription {
    private final String subscriptionId;
    private final String customerId;
    private String publicationId;
    private DeliveryFrequency deliveryFrequency;
    private boolean paused;

    public Subscription(String subscriptionId, String customerId, String publicationId, DeliveryFrequency deliveryFrequency) {
        this.subscriptionId = subscriptionId; this.customerId = customerId;
        this.publicationId = publicationId; this.deliveryFrequency = deliveryFrequency;
    }
    public String getSubscriptionId() { return subscriptionId; }
    public String getCustomerId() { return customerId; }
    public String getPublicationId() { return publicationId; }
    public void setPublicationId(String publicationId) { this.publicationId = publicationId; }
    public DeliveryFrequency getDeliveryFrequency() { return deliveryFrequency; }
    public void setDeliveryFrequency(DeliveryFrequency deliveryFrequency) { this.deliveryFrequency = deliveryFrequency; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
}
