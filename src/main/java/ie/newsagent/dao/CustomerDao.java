package ie.newsagent.dao;

import ie.newsagent.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerDao {
    Customer save(Customer customer);
    Optional<Customer> findById(String customerId);
    List<Customer> findAll();
    boolean deleteById(String customerId);
}
