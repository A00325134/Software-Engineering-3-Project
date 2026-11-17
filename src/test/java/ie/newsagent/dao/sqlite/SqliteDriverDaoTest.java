package ie.newsagent.dao.sqlite;

import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.DeliveryDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDriverDaoTest {
    @TempDir Path directory;
    private SqliteDriverDao dao;

    @BeforeEach void setUp() {
        SqliteDatabase database = SqliteDatabase.fromPath(directory.resolve("drivers.db"));
        database.initialiseSchema();
        dao = new SqliteDriverDao(database);
    }

    @Test void createsRetrievesAndUpdatesDriverProfile() {
        dao.save(new DeliveryDriver("D1", "Sam Driver", "hash-one"));
        dao.save(new DeliveryDriver("D1", "Sam Updated", "hash-two"));

        DeliveryDriver driver = dao.findById("D1").orElseThrow();
        assertEquals("Sam Updated", driver.getName());
        assertEquals("hash-two", driver.getPasswordHash());
        assertEquals(1, dao.findAll().size());
    }

    @Test void rejectsMissingDriverFieldsAndDeletesProfile() {
        assertThrows(IllegalArgumentException.class, () -> dao.save(new DeliveryDriver("D2", " ", "hash")));
        assertThrows(IllegalArgumentException.class, () -> dao.save(new DeliveryDriver("D2", "Sam Driver", null)));
        dao.save(new DeliveryDriver("D2", "Sam Driver", "hash"));

        assertTrue(dao.deleteById("D2"));
        assertTrue(dao.findById("D2").isEmpty());
        assertFalse(dao.deleteById("missing"));
    }

    @Test void duplicateIdentifierUpdatesOneProfileInsteadOfCreatingDuplicates() {
        dao.save(new DeliveryDriver("D1", "Sam Driver", "hash-one"));
        dao.save(new DeliveryDriver("D1", "Sam Driver Two", "hash-two"));

        assertEquals(1, dao.findAll().size());
    }

    @Test void cannotDeleteDriverReferencedByDeliveryDocket() throws Exception {
        dao.save(new DeliveryDriver("D1", "Sam Driver", "hash"));
        try (var connection = SqliteDatabase.fromPath(directory.resolve("drivers.db")).openConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO delivery_docket (docket_reference, delivery_date, driver_id, delivery_area) " +
                    "VALUES ('DK1', '2026-11-03', 'D1', 1)");
        }

        assertThrows(DatabaseException.class, () -> dao.deleteById("D1"));
        assertTrue(dao.findById("D1").isPresent());
    }
}
