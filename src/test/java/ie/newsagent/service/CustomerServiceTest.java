package ie.newsagent.service;

import ie.newsagent.dao.memory.InMemoryCustomerDao;
import ie.newsagent.model.Customer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerServiceTest {
    @Test
    void createsAndRetrievesCustomer() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        Customer created = service.create("Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1);

        Customer retrieved = service.get(created.getCustomerId());
        assertEquals(created.getCustomerId(), retrieved.getCustomerId());
        assertEquals("Alex Murphy", retrieved.getName());
    }

    @Test
    void returnsAllCustomers() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());
        service.create("Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1);
        service.create("Taylor Smith", "0861234567", "12 Main Street", "D02X286", 2);

        assertEquals(2, service.getAll().size());
    }

    @Test
    void rejectsUnknownCustomer() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertThrows(IllegalArgumentException.class, () -> service.get("missing"));
    }

    @Test
    void acceptsMinimumAndMaximumLengthsAndAreaValues() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        Customer minimum = service.create("Abc", "12345", "Elm", "1234567", 1);
        Customer maximum = service.create("N".repeat(50), "P".repeat(20), "A".repeat(100),
                "7654321", 24);

        assertEquals("Abc", minimum.getName());
        assertEquals(1, minimum.getDeliveryArea());
        assertEquals(50, maximum.getName().length());
        assertEquals(24, maximum.getDeliveryArea());
    }

    @Test
    void rejectsNameOutsideAllowedLength() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertInvalidCustomer(service, "Ab", "12345", "Elm", "1234567", 1);
        assertInvalidCustomer(service, "N".repeat(51), "12345", "Elm", "1234567", 1);
    }

    @Test
    void rejectsPhoneOutsideAllowedLength() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertInvalidCustomer(service, "Valid Name", "1234", "Elm", "1234567", 1);
        assertInvalidCustomer(service, "Valid Name", "P".repeat(21), "Elm", "1234567", 1);
    }

    @Test
    void rejectsAddressOutsideAllowedLength() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertInvalidCustomer(service, "Valid Name", "12345", "No", "1234567", 1);
        assertInvalidCustomer(service, "Valid Name", "12345", "A".repeat(101), "1234567", 1);
    }

    @Test
    void rejectsMissingRequiredFields() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertInvalidCustomer(service, null, "12345", "Elm", "1234567", 1);
        assertInvalidCustomer(service, "Valid Name", null, "Elm", "1234567", 1);
        assertInvalidCustomer(service, "Valid Name", "12345", null, "1234567", 1);
        assertInvalidCustomer(service, "Valid Name", "12345", "Elm", null, 1);
    }

    @Test
    void rejectsEircodeWithWrongLength() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertInvalidCustomer(service, "Valid Name", "12345", "Elm", "123456", 1);
        assertInvalidCustomer(service, "Valid Name", "12345", "Elm", "12345678", 1);
    }

    @Test
    void rejectsDeliveryAreaOutsideAllowedRange() {
        CustomerService service = new CustomerService(new InMemoryCustomerDao());

        assertInvalidCustomer(service, "Valid Name", "12345", "Elm", "1234567", 0);
        assertInvalidCustomer(service, "Valid Name", "12345", "Elm", "1234567", 25);
    }

    private void assertInvalidCustomer(CustomerService service, String name, String phone,
                                       String address, String eircode, int deliveryArea) {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(name, phone, address, eircode, deliveryArea));
    }
}
