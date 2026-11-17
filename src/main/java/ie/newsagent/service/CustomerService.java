package ie.newsagent.service;

import ie.newsagent.dao.CustomerDao;
import ie.newsagent.model.Customer;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class CustomerService {
    private static final Pattern EIRCODE = Pattern.compile("(?i)^[A-Z0-9]{7}$");
    private final CustomerDao customerDao;
    public CustomerService(CustomerDao customerDao) { this.customerDao = customerDao; }

    public Customer create(String name, String phone, String address, String eircode, int deliveryArea) {
        validate(name, phone, address, eircode, deliveryArea);
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return customerDao.save(new Customer(id, name.trim(), phone.trim(), address.trim(), eircode.trim(), deliveryArea));
    }
    public Customer get(String id) {
        return customerDao.findById(id).orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }
    public List<Customer> getAll() { return customerDao.findAll(); }
    public void delete(String id) {
        if (!customerDao.deleteById(id)) throw new IllegalArgumentException("Customer not found: " + id);
    }
    public Customer update(String id, String name, String phone, String address, String eircode, int deliveryArea) {
        validate(name, phone, address, eircode, deliveryArea);
        Customer c = get(id);
        c.setName(name.trim()); c.setPhoneNumber(phone.trim()); c.setAddress(address.trim());
        c.setEircode(eircode.trim()); c.setDeliveryArea(deliveryArea);
        return customerDao.save(c);
    }
    private void validate(String name, String phone, String address, String eircode, int area) {
        if (name == null || name.trim().length() < 3 || name.trim().length() > 50) throw new IllegalArgumentException("Name must be 3–50 characters.");
        if (phone == null || phone.trim().length() < 5 || phone.trim().length() > 20) throw new IllegalArgumentException("Phone number must be 5–20 characters.");
        if (address == null || address.trim().length() < 3 || address.trim().length() > 100) throw new IllegalArgumentException("Address must be 3–100 characters.");
        if (eircode == null || !EIRCODE.matcher(eircode.trim()).matches()) throw new IllegalArgumentException("Eircode must be 7 alphanumeric characters.");
        if (area < 1 || area > 24) throw new IllegalArgumentException("Delivery area must be between 1 and 24.");
    }
}
