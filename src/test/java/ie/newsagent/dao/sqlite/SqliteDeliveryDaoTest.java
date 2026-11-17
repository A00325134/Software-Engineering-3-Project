package ie.newsagent.dao.sqlite;

import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Customer;
import ie.newsagent.model.Delivery;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.model.DeliveryStatus;
import ie.newsagent.model.Publication;
import ie.newsagent.model.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDeliveryDaoTest {
    @TempDir Path directory;

    private SqliteDatabase database() {
        SqliteDatabase database = SqliteDatabase.fromPath(directory.resolve("deliveries.db"));
        database.initialiseSchema();
        var customers = new SqliteCustomerDao(database);
        customers.save(new Customer("C1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));
        new SqlitePublicationDao(database).save(new Publication("P1", "Daily News", 20));
        new SqliteSubscriptionDao(database).save(
                new Subscription("S1", "C1234", "P1", DeliveryFrequency.MONTHLY));
        return database;
    }

    private Delivery delivery(String reference, DeliveryStatus status) {
        Delivery delivery = new Delivery(reference, "C1234", "P1");
        delivery.setStatus(status);
        delivery.setDeliveryDate(LocalDate.of(2026, 10, 3));
        return delivery;
    }

    @Test
    void savesDeliveryHistoryAndOutOfStockRecordAcrossConnections() throws Exception {
        SqliteDatabase database = database();
        SqliteDeliveryDao dao = new SqliteDeliveryDao(database);
        dao.save(delivery("D1", DeliveryStatus.DELIVERED));
        dao.save(delivery("D2", DeliveryStatus.FAILED));
        dao.save(delivery("D3", DeliveryStatus.OUT_OF_STOCK));

        List<Delivery> deliveries = new SqliteDeliveryDao(database).findAll();
        assertEquals(List.of(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED, DeliveryStatus.OUT_OF_STOCK),
                deliveries.stream().map(Delivery::getStatus).toList());
        assertEquals("Alex Murphy", deliveries.get(0).getCustomerName());
        assertEquals("Daily News", deliveries.get(0).getPublicationName());
        assertEquals(LocalDate.of(2026, 10, 3), deliveries.get(0).getDeliveryDate());
        assertEquals(List.of("D1"), dao.findDeliveredForCustomer("C1234", YearMonth.of(2026, 10))
                .stream().map(Delivery::getDeliveryReference).toList());
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT publication_id, note FROM out_of_stock_record " +
                     "WHERE delivery_reference='D3'")) {
            assertTrue(result.next());
            assertEquals("P1", result.getString("publication_id"));
            assertEquals("Out of stock", result.getString("note"));
        }
    }

    @Test
    void rejectsMissingCustomerSubscriptionAndInvalidDeliveryWithoutPartialRows() throws Exception {
        SqliteDatabase database = database();
        SqliteDeliveryDao dao = new SqliteDeliveryDao(database);
        Delivery missingCustomer = new Delivery("D1", "missing", "P1");
        missingCustomer.setStatus(DeliveryStatus.DELIVERED);
        Delivery missingSubscription = new Delivery("D2", "C1234", "missing");
        missingSubscription.setStatus(DeliveryStatus.DELIVERED);
        Delivery invalid = delivery(" ", DeliveryStatus.DELIVERED);

        assertThrows(IllegalArgumentException.class, () -> dao.save(missingCustomer));
        assertThrows(IllegalArgumentException.class, () -> dao.save(missingSubscription));
        assertThrows(IllegalArgumentException.class, () -> dao.save(invalid));
        assertTrue(dao.findAll().isEmpty());
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM delivery_docket")) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    @Test
    void duplicateDeliveryReferenceRollsBackItsNewDocket() throws Exception {
        SqliteDatabase database = database();
        SqliteDeliveryDao dao = new SqliteDeliveryDao(database);
        dao.save(delivery("D1", DeliveryStatus.DELIVERED));

        assertThrows(DatabaseException.class, () -> dao.save(delivery("D1", DeliveryStatus.FAILED)));
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM delivery_docket")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }
}
