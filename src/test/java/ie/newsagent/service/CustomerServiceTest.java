package ie.newsagent.service;

import ie.newsagent.dao.memory.InMemoryCustomerDao;
import ie.newsagent.model.Customer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceTest {
    @Test void createsAndFindsCustomer() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());
        Customer created = service.create("Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1);
        assertEquals(created.getCustomerId(), service.get(created.getCustomerId()).getCustomerId());
        assertEquals("Alex Murphy", service.get(created.getCustomerId()).getName());
    }
    @Test void rejectsInvalidName() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());
        assertThrows(IllegalArgumentException.class,
            () -> service.create("Al", "0871234567", "10 Main Street", "D02X285", 1));
    }
    @Test void rejectsUnknownCustomer() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());
        assertThrows(IllegalArgumentException.class, () -> service.get("missing"));
    }
}
