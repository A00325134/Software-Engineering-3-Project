package ie.newsagent.database;

/** Raised when a customer ID already exists. */
public class DuplicateCustomerException extends RuntimeException {
    public DuplicateCustomerException(String customerId, Throwable cause) {
        super("Customer ID already exists: " + customerId, cause);
    }
}
