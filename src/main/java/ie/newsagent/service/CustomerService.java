package ie.newsagent.service;

import ie.newsagent.dao.CustomerDao;
import ie.newsagent.model.Customer;

import java.util.List;
import java.util.UUID;

public class CustomerService {
    private final CustomerDao customerDao;

    public CustomerService(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    public Customer create(String name, String phone, String address, String eircode, int deliveryArea) {
        validate(name, phone, address, eircode, deliveryArea);
        String customerId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Customer customer = new Customer(customerId, name.trim(), phone.trim(), address.trim(),
                eircode.trim(), deliveryArea);
        return customerDao.save(customer);
    }

    public Customer get(String customerId) {
        return customerDao.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
    }

    public List<Customer> getAll() {
        return customerDao.findAll();
    }

    public void delete(String customerId) {
        if (!customerDao.deleteById(customerId)) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
    }

    public Customer update(String customerId, String name, String phone, String address,
                           String eircode, int deliveryArea) {
        validate(name, phone, address, eircode, deliveryArea);
        Customer customer = get(customerId);
        customer.setName(name.trim());
        customer.setPhoneNumber(phone.trim());
        customer.setAddress(address.trim());
        customer.setEircode(eircode.trim());
        customer.setDeliveryArea(deliveryArea);
        if (!customerDao.updateById(customerId, customer)) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        return customer;
    }

    private void validate(String name, String phone, String address, String eircode, int deliveryArea) {
        if (name == null || name.trim().length() < 3 || name.trim().length() > 50) {
            throw new IllegalArgumentException("Name must be 3-50 characters.");
        }
        if (phone == null || phone.trim().length() < 5 || phone.trim().length() > 20) {
            throw new IllegalArgumentException("Phone number must be 5-20 characters.");
        }
        if (address == null || address.trim().length() < 3 || address.trim().length() > 100) {
            throw new IllegalArgumentException("Address must be 3-100 characters.");
        }
        if (eircode == null || eircode.trim().length() != 7) {
            throw new IllegalArgumentException("Eircode must be exactly 7 characters.");
        }
        if (deliveryArea < 1 || deliveryArea > 24) {
            throw new IllegalArgumentException("Delivery area must be between 1 and 24.");
        }
    }
}
