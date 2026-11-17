package ie.newsagent.database;

import ie.newsagent.dao.sqlite.SqliteCustomerDao;
import ie.newsagent.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDatabaseTest {
    @TempDir
    Path temporaryDirectory;

    private SqliteDatabase database;

    @BeforeEach
    void setUp() {
        database = SqliteDatabase.fromPath(temporaryDirectory.resolve("newsagent.db"));
    }

    @Test
    void opensConnectionWithForeignKeysEnabled() throws Exception {
        try (var connection = database.openConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA foreign_keys")) {

            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void createsRequiredTablesAndEnforcesForeignKeys() throws Exception {
        database.initialiseSchema();

        try (var connection = database.openConnection();
             var statement = connection.createStatement()) {
            Set<String> tableNames = new HashSet<>();
            try (var result = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table'")) {
                while (result.next()) {
                    tableNames.add(result.getString("name"));
                }
            }

            assertTrue(tableNames.containsAll(Set.of("customer", "delivery_driver", "publication",
                    "subscription", "bill", "bill_item", "delivery_docket", "delivery",
                    "out_of_stock_record", "delivery_area", "docket_item")));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO subscription (subscription_id, customer_id, publication_id, " +
                            "delivery_frequency) VALUES ('s1', 'missing', 'missing', 'MONTHLY')"));
        }
    }

    @Test
    void seedsEveryDeliveryAreaAndRejectsAnInvalidArea() throws Exception {
        database.initialiseSchema();
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*), MIN(area_id), MAX(area_id) FROM delivery_area")) {
            assertTrue(result.next());
            assertEquals(24, result.getInt(1));
            assertEquals(1, result.getInt(2));
            assertEquals(24, result.getInt(3));
        }
        try (var connection = database.openConnection(); var statement = connection.createStatement()) {
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO delivery_docket(docket_reference,delivery_date,delivery_area) " +
                            "VALUES('invalid-area','2026-11-03',25)"));
        }
    }

    @Test
    void canInitialiseSchemaAgainWithoutLosingCustomerData() {
        database.initialiseSchema();
        SqliteCustomerDao customerDao = new SqliteCustomerDao(database);
        customerDao.save(new Customer("C1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));

        database.initialiseSchema();

        assertEquals("Alex Murphy", customerDao.findById("C1234").orElseThrow().getName());
    }

    @Test
    void upgradesWeekSixBillingTablesBeforeCreatingWeekSevenIndexes() throws Exception {
        try (var connection = database.openConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE customer (customer_id TEXT PRIMARY KEY, name TEXT NOT NULL, " +
                    "phone_number TEXT NOT NULL, address TEXT NOT NULL, eircode TEXT NOT NULL, delivery_area INTEGER NOT NULL)");
            statement.execute("CREATE TABLE bill (bill_reference TEXT PRIMARY KEY, customer_id TEXT NOT NULL, " +
                    "billing_month TEXT NOT NULL, monthly_total NUMERIC NOT NULL, payment_status TEXT NOT NULL, " +
                    "FOREIGN KEY (customer_id) REFERENCES customer(customer_id))");
            statement.execute("CREATE TABLE bill_item (bill_reference TEXT NOT NULL, item_number INTEGER NOT NULL, " +
                    "item_name TEXT NOT NULL, delivery_date TEXT NOT NULL, amount NUMERIC NOT NULL, " +
                    "PRIMARY KEY (bill_reference, item_number), FOREIGN KEY (bill_reference) REFERENCES bill(bill_reference))");
            statement.executeUpdate("INSERT INTO customer VALUES " +
                    "('C1234', 'Alex Murphy', '0871234567', '10 Main Street', 'D02X285', 1)");
            statement.executeUpdate("INSERT INTO bill VALUES ('B1', 'C1234', '2026-10', 2.50, 'UNPAID')");
            statement.executeUpdate("INSERT INTO bill_item VALUES ('B1', 1, 'Daily News', '2026-10-03', 2.50)");
        }

        database.initialiseSchema();

        try (var connection = database.openConnection(); var statement = connection.createStatement()) {
            assertColumnExists(statement, "bill", "docket_reference");
            assertColumnExists(statement, "bill_item", "delivery_reference");
            try (var result = statement.executeQuery(
                    "SELECT b.bill_reference, i.item_name FROM bill b JOIN bill_item i USING (bill_reference)")) {
                assertTrue(result.next());
                assertEquals("B1", result.getString("bill_reference"));
                assertEquals("Daily News", result.getString("item_name"));
            }
            try (var result = statement.executeQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name IN " +
                            "('idx_bill_customer_month', 'idx_bill_item_delivery')")) {
                assertTrue(result.next());
                assertEquals(2, result.getInt(1));
            }
        }
    }

    private void assertColumnExists(Statement statement, String table, String column) throws SQLException {
        try (var result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equals(result.getString("name"))) return;
            }
        }
        throw new AssertionError("Expected column " + table + "." + column);
    }
}
