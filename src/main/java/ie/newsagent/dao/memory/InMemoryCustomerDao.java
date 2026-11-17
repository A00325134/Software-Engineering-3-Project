package ie.newsagent.dao.memory;

import ie.newsagent.dao.CustomerDao;
import ie.newsagent.model.Customer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCustomerDao implements CustomerDao {
    private final Map<String, Customer> records = new LinkedHashMap<>();

    @Override
    public Customer save(Customer customer) {
        records.put(customer.getCustomerId(), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        return Optional.ofNullable(records.get(customerId));
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(records.values());
    }

    @Override
    public boolean deleteById(String customerId) {
        return records.remove(customerId) != null;
    }
}
