package ie.newsagent.dao.sqlite;

import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Publication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlitePublicationDaoTest {
    @TempDir Path directory;
    private SqlitePublicationDao dao;

    @BeforeEach void setUp() {
        SqliteDatabase database = SqliteDatabase.fromPath(directory.resolve("publications.db"));
        database.initialiseSchema();
        dao = new SqlitePublicationDao(database);
    }

    @Test void createsRetrievesAndUpdatesPublicationAndStock() {
        dao.save(new Publication("P1", "Daily News", 20));
        Publication changed = new Publication("P1", "Morning News", 8);
        dao.save(changed);

        Publication retrieved = dao.findById("P1").orElseThrow();
        assertEquals("Morning News", retrieved.getName());
        assertEquals(8, retrieved.getStockQuantity());
        assertEquals(5, retrieved.getLowStockThreshold());
        assertEquals(1, dao.findAll().size());
    }

    @Test void lowStockIncludesOnlyValuesBelowFive() {
        dao.save(new Publication("P1", "Daily News", 4));
        dao.save(new Publication("P2", "Evening News", 5));
        dao.save(new Publication("P3", "Weekly Review", 9));

        List<Publication> lowStock = dao.findLowStock();

        assertEquals(List.of("P1"), lowStock.stream().map(Publication::getPublicationId).toList());
        assertEquals(4, lowStock.get(0).getStockQuantity());
    }

    @Test void returnsEmptyLowStockResultsAndRejectsInvalidPublicationValues() {
        assertTrue(dao.findLowStock().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.save(new Publication("P1", "No", 20)));
        assertThrows(IllegalArgumentException.class, () -> dao.save(new Publication("P1", "Daily News", -1)));
        assertThrows(IllegalArgumentException.class, () -> dao.save(new Publication("P1", "Daily News", 1001)));
        Publication invalidThreshold = new Publication("P1", "Daily News", 20);
        invalidThreshold.setLowStockThreshold(6);
        assertThrows(IllegalArgumentException.class, () -> dao.save(invalidThreshold));
    }

    @Test void deletionIsRestrictedWhenSubscriptionReferencesPublication() {
        SqliteDatabase database = SqliteDatabase.fromPath(directory.resolve("publications.db"));
        new SqliteCustomerDao(database).save(new ie.newsagent.model.Customer("C1234", "Alex Murphy",
                "0871234567", "10 Main Street", "D02X285", 1));
        dao.save(new Publication("P1", "Daily News", 20));
        new SqliteSubscriptionDao(database).save(new ie.newsagent.model.Subscription("S1", "C1234", "P1",
                ie.newsagent.model.DeliveryFrequency.MONTHLY));

        assertThrows(DatabaseException.class, () -> dao.deleteById("P1"));
        assertTrue(dao.findById("P1").isPresent());
    }
}
