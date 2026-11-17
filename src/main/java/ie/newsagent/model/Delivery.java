package ie.newsagent.model;

public class Delivery {
    private final String deliveryReference;
    private final String customerId;
    private final String publicationId;
    private DeliveryStatus status;

    public Delivery(String deliveryReference, String customerId, String publicationId) {
        this.deliveryReference = deliveryReference; this.customerId = customerId; this.publicationId = publicationId;
    }
    public String getDeliveryReference() { return deliveryReference; }
    public String getCustomerId() { return customerId; }
    public String getPublicationId() { return publicationId; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
}
