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
                    "out_of_stock_record")));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO subscription (subscription_id, customer_id, publication_id, " +
                            "delivery_frequency) VALUES ('s1', 'missing', 'missing', 'MONTHLY')"));
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
}
