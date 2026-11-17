package ie.newsagent.dao.sqlite;

import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Customer;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.model.Publication;
import ie.newsagent.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SqliteSubscriptionDaoTest {
    @TempDir Path directory;
    private SqliteDatabase database;
    private SqliteSubscriptionDao dao;

    @BeforeEach void setUp() {
        database = SqliteDatabase.fromPath(directory.resolve("subscriptions.db"));
        database.initialiseSchema();
        dao = new SqliteSubscriptionDao(database);
        new SqliteCustomerDao(database).save(new Customer("C1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));
        new SqlitePublicationDao(database).save(new Publication("P1", "Daily News", 20));
    }

    @Test void createsAndRetrievesSubscriptionLinkedToCustomerAndPublication() {
        dao.save(subscription("S1"));

        Subscription retrieved = dao.findById("S1").orElseThrow();
        assertEquals("C1234", retrieved.getCustomerId());
        assertEquals("P1", retrieved.getPublicationId());
        assertEquals(DeliveryFrequency.MONTHLY, retrieved.getDeliveryFrequency());
        assertFalse(retrieved.isPaused());
        assertEquals(1, dao.findByCustomerId("C1234").size());
        assertTrue(dao.findByCustomerId("unknown").isEmpty());
    }

    @Test void pauseAndResumeStatePersistsAfterReopeningDatabase() {
        dao.save(subscription("S1"));
        Subscription paused = subscription("S1");
        paused.setPaused(true);
        dao.save(paused);

        SqliteDatabase reopenedDatabase = SqliteDatabase.fromPath(directory.resolve("subscriptions.db"));
        SqliteSubscriptionDao reopenedDao = new SqliteSubscriptionDao(reopenedDatabase);
        assertTrue(reopenedDao.findById("S1").orElseThrow().isPaused());

        Subscription resumed = reopenedDao.findById("S1").orElseThrow();
        resumed.setPaused(false);
        reopenedDao.save(resumed);
        assertFalse(reopenedDao.findById("S1").orElseThrow().isPaused());
    }

    @Test void rejectsMissingCustomerAndPublicationReferences() {
        try (var connection = database.openConnection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO subscription " +
                    "(subscription_id, customer_id, publication_id, delivery_frequency, paused) " +
                    "VALUES ('S2', 'missing', 'P1', 'MONTHLY', 0)"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO subscription " +
                    "(subscription_id, customer_id, publication_id, delivery_frequency, paused) " +
                    "VALUES ('S3', 'C1234', 'missing', 'MONTHLY', 0)"));
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test void rejectsInvalidFrequencyAndHandlesUnknownIds() {
        assertThrows(IllegalArgumentException.class, () -> dao.save(new Subscription("S1", "C1234", "P1", null)));
        assertTrue(dao.findById("missing").isEmpty());
        assertFalse(dao.deleteById("missing"));

        try (var connection = database.openConnection(); Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO subscription " +
                    "(subscription_id, customer_id, publication_id, delivery_frequency, paused) " +
                    "VALUES ('S4', 'C1234', 'P1', 'DAILY', 0)"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("INSERT INTO subscription " +
                    "(subscription_id, customer_id, publication_id, delivery_frequency, paused) " +
                    "VALUES ('S5', 'C1234', 'P1', 'MONTHLY', 2)"));
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    @Test void duplicateSubscriptionIdUpdatesExistingRecord() {
        dao.save(subscription("S1"));
        Subscription paused = subscription("S1");
        paused.setPaused(true);
        dao.save(paused);

        assertEquals(1, dao.findAll().size());
        assertTrue(dao.findById("S1").orElseThrow().isPaused());
    }

    @Test void doesNotDeleteSubscriptionUsedByDeliveryHistory() throws Exception {
        dao.save(subscription("S1"));
        try (var connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO delivery_docket (docket_reference, delivery_date, delivery_area) " +
                    "VALUES ('DK1', '2026-11-03', 1)");
            statement.executeUpdate("INSERT INTO delivery (delivery_reference, docket_reference, subscription_id, status) " +
                    "VALUES ('DL1', 'DK1', 'S1', 'DELIVERED')");
        }

        assertThrows(DatabaseException.class, () -> dao.deleteById("S1"));
        assertTrue(dao.findById("S1").isPresent());
    }

    private Subscription subscription(String id) {
        return new Subscription(id, "C1234", "P1", DeliveryFrequency.MONTHLY);
    }
}
