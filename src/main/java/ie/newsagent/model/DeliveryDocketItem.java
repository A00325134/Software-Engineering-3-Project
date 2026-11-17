package ie.newsagent.model;

/** A publication scheduled for one customer on a daily delivery docket. */
public record DeliveryDocketItem(
        String deliveryReference,
        String customerId,
        String customerName,
        String customerAddress,
        String publicationId,
        String publicationName,
        int stockQuantity,
        DeliveryStatus status) {
}
