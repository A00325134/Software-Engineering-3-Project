package ie.newsagent.dao;

import ie.newsagent.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerDao {
    /** Inserts a new customer; implementations should reject an existing ID. */
    Customer save(Customer customer);
    boolean updateById(String customerId, Customer customer);
    Optional<Customer> findById(String customerId);
    List<Customer> findAll();
    /** False means no row matched; related history may restrict deletion. */
    boolean deleteById(String customerId);
}
