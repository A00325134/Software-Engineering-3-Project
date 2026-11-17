package ie.newsagent.demo;

import ie.newsagent.dao.sqlite.SqliteBillDao;
import ie.newsagent.dao.sqlite.SqliteDeliveryDao;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.DeliveryStatus;
import ie.newsagent.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class DemoDataSeederTest {
    @TempDir Path directory;

    @Test
    void seedsIsolatedDemoDatabaseWithRepeatableCrossRoleExamples() throws Exception {
        var database = SqliteDatabase.fromPath(directory.resolve("newsagent-demo.db"));

        DemoDataSeeder.seed(database);
        DemoDataSeeder.seed(database);

        var deliveries = new SqliteDeliveryDao(database).findAll();
        assertEquals(3, deliveries.size());
        assertEquals(1, deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.DELIVERED).count());
        assertEquals(1, deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.FAILED).count());
        assertEquals(1, deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.OUT_OF_STOCK).count());
        assertEquals(3, new SqliteBillDao(database).findAll().size());
        assertEquals(2, new PaymentService(new SqliteBillDao(database))
                .remindersFor(YearMonth.of(2027, 1)).size());
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("""
                     SELECT
                       (SELECT COUNT(*) FROM customer WHERE customer_id LIKE 'DEMO-%'),
                       (SELECT COUNT(*) FROM delivery_driver WHERE driver_id LIKE 'DEMO-%'),
                       (SELECT COUNT(*) FROM publication WHERE publication_id LIKE 'DEMO-%'),
                       (SELECT COUNT(*) FROM subscription WHERE subscription_id LIKE 'DEMO-%'),
                       (SELECT COUNT(*) FROM delivery_docket WHERE delivery_date IN ('2026-11-17','2026-12-03'))
                     """)) {
            assertTrue(result.next());
            assertEquals(3, result.getInt(1));
            assertEquals(1, result.getInt(2));
            assertEquals(3, result.getInt(3));
            assertEquals(4, result.getInt(4));
            assertEquals(48, result.getInt(5));
        }
    }
}
