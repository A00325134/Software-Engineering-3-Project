package ie.newsagent.model;

import java.util.List;

/** Delivery outcomes for one customer, including customers with no deliveries. */
public record DeliverySummary(String customerId, String customerName, List<Delivery> deliveries) {
    public DeliverySummary { deliveries=List.copyOf(deliveries); }
}
