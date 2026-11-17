package ie.newsagent.dao.sqlite;

import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.DuplicateCustomerException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteCustomerDaoTest {
    @TempDir
    Path temporaryDirectory;

    private SqliteCustomerDao customerDao;

    @BeforeEach
    void setUp() {
        SqliteDatabase database = SqliteDatabase.fromPath(temporaryDirectory.resolve("customers.db"));
        database.initialiseSchema();
        customerDao = new SqliteCustomerDao(database);
    }

    @Test
    void savesAndRetrievesAllCustomerFields() {
        Customer customer = customer("A1234Z", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 24);

        customerDao.save(customer);

        Customer retrieved = customerDao.findById("A1234Z").orElseThrow();
        assertEquals("A1234Z", retrieved.getCustomerId());
        assertEquals("Alex Murphy", retrieved.getName());
        assertEquals("0871234567", retrieved.getPhoneNumber());
        assertEquals("10 Main Street", retrieved.getAddress());
        assertEquals("D02X285", retrieved.getEircode());
        assertEquals(24, retrieved.getDeliveryArea());
    }

    @Test
    void rejectsDuplicateCustomerId() {
        Customer customer = customer("C1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1);
        customerDao.save(customer);

        assertThrows(DuplicateCustomerException.class, () -> customerDao.save(customer));
    }

    @Test
    void acceptsMinimumAndMaximumCustomerIdLengths() {
        customerDao.save(customer("12345", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));
        customerDao.save(customer("123456789012345", "Taylor Smith", "0861234567",
                "12 Main Street", "D02X286", 2));

        assertEquals(2, customerDao.findAll().size());
    }

    @Test
    void returnsEmptyForUnknownIdButThrowsForDatabaseFailure() {
        assertTrue(customerDao.findById("missing").isEmpty());

        SqliteDatabase uninitialisedDatabase = SqliteDatabase.fromPath(
                temporaryDirectory.resolve("uninitialised.db"));
        SqliteCustomerDao daoWithMissingSchema = new SqliteCustomerDao(uninitialisedDatabase);
        assertThrows(DatabaseException.class, () -> daoWithMissingSchema.findById("missing"));
    }

    @Test
    void returnsAllCustomersInIdOrder() {
        customerDao.save(customer("C0002", "Taylor Smith", "0861234567",
                "12 Main Street", "D02X286", 2));
        customerDao.save(customer("C0001", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));

        List<Customer> customers = customerDao.findAll();

        assertEquals(List.of("C0001", "C0002"),
                customers.stream().map(Customer::getCustomerId).toList());
    }

    @Test
    void deletesExistingCustomerAndReturnsFalseForUnknownId() {
        customerDao.save(customer("C1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));

        assertTrue(customerDao.deleteById("C1234"));
        assertTrue(customerDao.findById("C1234").isEmpty());
        assertFalse(customerDao.deleteById("missing"));
    }

    @Test
    void databaseConstraintsRejectInvalidCustomerValues() {
        Customer invalidCustomer = customer("C1234", "Al", "0871234567",
                "10 Main Street", "D02X285", 1);

        assertThrows(DatabaseException.class, () -> customerDao.save(invalidCustomer));
    }

    @Test
    void databaseConstraintsRejectInvalidCustomerIdLengths() {
        Customer shortId = customer("1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1);
        Customer longId = customer("1234567890123456", "Taylor Smith", "0861234567",
                "12 Main Street", "D02X286", 2);

        assertThrows(DatabaseException.class, () -> customerDao.save(shortId));
        assertThrows(DatabaseException.class, () -> customerDao.save(longId));
    }

    @Test
    void databaseConstraintsRejectInvalidEircodeLengthAndDeliveryArea() {
        Customer shortEircode = customer("C1001", "Alex Murphy", "0871234567",
                "10 Main Street", "123456", 1);
        Customer invalidArea = customer("C1002", "Taylor Smith", "0861234567",
                "12 Main Street", "D02X286", 25);

        assertThrows(DatabaseException.class, () -> customerDao.save(shortEircode));
        assertThrows(DatabaseException.class, () -> customerDao.save(invalidArea));
    }

    @Test
    void updatesExistingCustomerWithoutChangingItsId() {
        Customer original = customer("C1234", "Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1);
        customerDao.save(original);
        Customer changed = customer("C1234", "Alex Murphy Updated", "0861234567", "12 Main Street", "D02X286", 2);

        assertTrue(customerDao.updateById("C1234", changed));
        assertEquals("Alex Murphy Updated", customerDao.findById("C1234").orElseThrow().getName());
        assertFalse(customerDao.updateById("C9999", customer("C9999", "Missing Person", "0871234567",
                "10 Main Street", "D02X285", 1)));
        assertThrows(IllegalArgumentException.class, () -> customerDao.updateById("other", changed));
    }

    @Test
    void rejectsInvalidUpdatesAndPreservesCustomerWhenUpdateFails() {
        customerDao.save(customer("C1234", "Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1));
        Customer invalid = customer("C1234", "Al", "0871234567", "10 Main Street", "D02X285", 1);

        assertThrows(DatabaseException.class, () -> customerDao.updateById("C1234", invalid));
        assertEquals("Alex Murphy", customerDao.findById("C1234").orElseThrow().getName());
    }

    @Test
    void deleteIsRestrictedWhileSubscriptionHistoryReferencesCustomer() {
        SqliteDatabase database = SqliteDatabase.fromPath(temporaryDirectory.resolve("customers.db"));
        var publicationDao = new SqlitePublicationDao(database);
        var subscriptionDao = new SqliteSubscriptionDao(database);
        customerDao.save(customer("C1234", "Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1));
        publicationDao.save(new ie.newsagent.model.Publication("P1", "Daily News", 20));
        subscriptionDao.save(new ie.newsagent.model.Subscription("S1", "C1234", "P1",
                ie.newsagent.model.DeliveryFrequency.MONTHLY));

        assertThrows(DatabaseException.class, () -> customerDao.deleteById("C1234"));
        assertTrue(customerDao.findById("C1234").isPresent());
        assertTrue(subscriptionDao.findById("S1").isPresent());
    }

    @Test
    void customerRemainsSavedAfterDatabaseIsReopened() {
        customerDao.save(customer("C1234", "Alex Murphy", "0871234567", "10 Main Street", "D02X285", 1));
        SqliteDatabase reopened = SqliteDatabase.fromPath(temporaryDirectory.resolve("customers.db"));
        reopened.initialiseSchema();

        assertEquals("Alex Murphy", new SqliteCustomerDao(reopened).findById("C1234").orElseThrow().getName());
    }

    private Customer customer(String id, String name, String phone, String address,
                               String eircode, int deliveryArea) {
        return new Customer(id, name, phone, address, eircode, deliveryArea);
    }
}
