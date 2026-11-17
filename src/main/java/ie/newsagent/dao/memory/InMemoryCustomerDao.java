package ie.newsagent.dao.memory;

import ie.newsagent.dao.CustomerDao;
import ie.newsagent.model.Customer;
import java.util.*;

public class InMemoryCustomerDao implements CustomerDao {
    private final Map<String, Customer> records = new LinkedHashMap<>();
    public Customer save(Customer customer) { records.put(customer.getCustomerId(), customer); return customer; }
    public Optional<Customer> findById(String id) { return Optional.ofNullable(records.get(id)); }
    public List<Customer> findAll() { return List.copyOf(records.values()); }
    public boolean deleteById(String id) { return records.remove(id) != null; }
}
