package ie.newsagent.service;

/** Assigns a customer's delivery area from an address using project-supplied mapping data. */
@FunctionalInterface
public interface DeliveryAreaAssigner {
    int assignArea(String address);
}
