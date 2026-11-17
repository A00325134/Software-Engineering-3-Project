package ie.newsagent.service;

/** Indicates that an address is not present in the configured delivery-area mapping. */
public class UnmappedAddressException extends IllegalArgumentException {
    public UnmappedAddressException(String address) {
        super("No delivery area is configured for address: " + address);
    }
}
