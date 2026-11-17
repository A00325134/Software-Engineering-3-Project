package ie.newsagent.dao;

import ie.newsagent.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerDao {
    /** Inserts a new customer; implementations should reject an existing ID. */
    Customer save(Customer customer);
    Optional<Customer> findById(String customerId);
    List<Customer> findAll();
    /** Reserved for the Week 3 customer increment; false means no row matched. */
    boolean deleteById(String customerId);
}
