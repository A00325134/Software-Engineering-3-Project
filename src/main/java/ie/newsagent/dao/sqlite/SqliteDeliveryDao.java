package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.DeliveryDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Delivery;
import ie.newsagent.model.DeliveryDocket;
import ie.newsagent.model.DeliveryDocketItem;
import ie.newsagent.model.DeliveryStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Persists deliveries and daily dockets, and reads delivery details for reporting. */
public final class SqliteDeliveryDao implements DeliveryDao {
    private final SqliteDatabase database;

    public SqliteDeliveryDao(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Delivery save(Delivery delivery) {
        validateDelivery(delivery);
        try (var connection = database.openConnection()) {
            connection.setAutoCommit(false);
            int area = findCustomerArea(connection, delivery.getCustomerId());
            String subscription = findActiveSubscription(connection, delivery.getCustomerId(),
                    delivery.getPublicationId());
            LocalDate date = delivery.getDeliveryDate() == null ? LocalDate.now() : delivery.getDeliveryDate();
            String docket = delivery.getDeliveryReference() + "-D";
            try (var statement = connection.prepareStatement(
                    "INSERT INTO delivery_docket(docket_reference,delivery_date,delivery_area) VALUES(?,?,?)")) {
                statement.setString(1, docket);
                statement.setString(2, date.toString());
                statement.setInt(3, area);
                statement.executeUpdate();
            }
            insertDocketItem(connection, delivery.getDeliveryReference(), docket, subscription,
                    delivery.getStatus());
            insertOutcome(connection, delivery.getDeliveryReference(), docket, subscription,
                    delivery.getStatus());
            if (delivery.getStatus() == DeliveryStatus.OUT_OF_STOCK) {
                insertOutOfStockRecord(connection, delivery.getDeliveryReference(),
                        delivery.getPublicationId(), date);
            }
            assignOutstandingBills(connection, docket, delivery.getCustomerId(), date);
            connection.commit();
            delivery.setSummaryDetails(null, null, date);
            return delivery;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save delivery " + delivery.getDeliveryReference(), exception);
        }
    }

    @Override
    public DeliveryDocket generateDocket(LocalDate deliveryDate, int deliveryArea) {
        Objects.requireNonNull(deliveryDate, "Delivery date is required");
        validateArea(deliveryArea);
        try (var connection = database.openConnection()) {
            connection.setAutoCommit(false);
            String docketReference = findOrCreateDocket(connection, deliveryDate, deliveryArea);
            String sql = """
                    SELECT s.subscription_id, c.customer_id, p.publication_id
                    FROM subscription s
                    JOIN customer c ON c.customer_id = s.customer_id
                    JOIN publication p ON p.publication_id = s.publication_id
                    WHERE c.delivery_area = ? AND s.paused = 0 AND s.delivery_frequency = 'MONTHLY'
                    AND NOT EXISTS (
                        SELECT 1 FROM docket_item prior
                        JOIN delivery_docket prior_docket
                          ON prior_docket.docket_reference = prior.docket_reference
                        WHERE prior.subscription_id = s.subscription_id
                          AND substr(prior_docket.delivery_date, 1, 7) = ?
                    )
                    ORDER BY c.customer_id, p.publication_id, s.subscription_id
                    """;
            List<String> subscriptionIds = new ArrayList<>();
            try (var statement = connection.prepareStatement(sql)) {
                statement.setInt(1, deliveryArea);
                statement.setString(2, YearMonth.from(deliveryDate).toString());
                try (var result = statement.executeQuery()) {
                    while (result.next()) subscriptionIds.add(result.getString("subscription_id"));
                }
            }
            for (String subscriptionId : subscriptionIds) {
                insertDocketItem(connection, UUID.randomUUID().toString(), docketReference,
                        subscriptionId, null);
            }
            assignBillsToDocketCustomers(connection, docketReference, deliveryDate);
            connection.commit();
            return readDocket(connection, docketReference);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not generate docket for area " + deliveryArea, exception);
        }
    }

    @Override
    public DeliveryDocket findDocket(String docketReference) {
        if (docketReference == null || docketReference.isBlank()) {
            throw new IllegalArgumentException("Docket reference is required");
        }
        try (var connection = database.openConnection()) {
            return readDocket(connection, docketReference);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve docket " + docketReference, exception);
        }
    }

    @Override
    public Delivery saveOutcome(String docketReference, String deliveryReference, DeliveryStatus status) {
        if (docketReference == null || docketReference.isBlank()
                || deliveryReference == null || deliveryReference.isBlank()) {
            throw new IllegalArgumentException("Docket and delivery references are required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Delivery status is required");
        }
        try (var connection = database.openConnection()) {
            connection.setAutoCommit(false);
            String subscriptionId;
            String customerId;
            String date;
            try (var statement = connection.prepareStatement("""
                    SELECT i.subscription_id, s.customer_id, k.delivery_date
                    FROM docket_item i
                    JOIN subscription s ON s.subscription_id = i.subscription_id
                    JOIN delivery_docket k ON k.docket_reference = i.docket_reference
                    WHERE i.docket_reference = ? AND i.delivery_reference = ?
                    """)) {
                statement.setString(1, docketReference);
                statement.setString(2, deliveryReference);
                try (var result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalArgumentException("Delivery reference is not on docket: " + deliveryReference);
                    }
                    subscriptionId = result.getString("subscription_id");
                    customerId = result.getString("customer_id");
                    date = result.getString("delivery_date");
                }
            }
            try (var statement = connection.prepareStatement(
                    "UPDATE docket_item SET outcome = ? WHERE docket_reference = ? AND delivery_reference = ?")) {
                statement.setString(1, status.name());
                statement.setString(2, docketReference);
                statement.setString(3, deliveryReference);
                statement.executeUpdate();
            }
            insertOutcome(connection, deliveryReference, docketReference, subscriptionId, status);
            String publicationId = findPublicationId(connection, subscriptionId);
            if (status == DeliveryStatus.OUT_OF_STOCK) {
                insertOutOfStockRecord(connection, deliveryReference, publicationId, LocalDate.parse(date));
            } else {
                try (var statement = connection.prepareStatement(
                        "DELETE FROM out_of_stock_record WHERE delivery_reference = ?")) {
                    statement.setString(1, deliveryReference);
                    statement.executeUpdate();
                }
            }
            assignOutstandingBills(connection, docketReference, customerId, LocalDate.parse(date));
            connection.commit();
            return findByReference(deliveryReference).orElseThrow();
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save delivery result " + deliveryReference, exception);
        }
    }

    @Override
    public Optional<Delivery> findByReference(String reference) {
        return query("WHERE d.delivery_reference=?", reference, null).stream().findFirst();
    }

    @Override
    public List<Delivery> findAll() {
        return query("", null, null);
    }

    @Override
    public List<Delivery> findDeliveredForCustomer(String customerId, YearMonth month) {
        return query("WHERE s.customer_id=? AND d.status='DELIVERED' AND substr(k.delivery_date,1,7)=?",
                customerId, month.toString());
    }

    private List<Delivery> query(String where, String first, String second) {
        String sql = """
                SELECT d.delivery_reference, s.customer_id, p.publication_id, d.status,
                       c.name customer_name, p.name publication_name, k.delivery_date
                FROM delivery d
                JOIN subscription s ON s.subscription_id=d.subscription_id
                JOIN customer c ON c.customer_id=s.customer_id
                JOIN publication p ON p.publication_id=s.publication_id
                JOIN delivery_docket k ON k.docket_reference=d.docket_reference
                """ + where + " ORDER BY k.delivery_date,d.delivery_reference";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            if (first != null) statement.setString(1, first);
            if (second != null) statement.setString(2, second);
            try (var result = statement.executeQuery()) {
                List<Delivery> deliveries = new ArrayList<>();
                while (result.next()) deliveries.add(mapDelivery(result));
                return List.copyOf(deliveries);
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve delivery history", exception);
        }
    }

    private DeliveryDocket readDocket(Connection connection, String docketReference) throws SQLException {
        LocalDate date;
        int area;
        try (var statement = connection.prepareStatement(
                "SELECT delivery_date, delivery_area FROM delivery_docket WHERE docket_reference = ?")) {
            statement.setString(1, docketReference);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Docket not found: " + docketReference);
                date = LocalDate.parse(result.getString("delivery_date"));
                area = result.getInt("delivery_area");
            }
        }
        String sql = """
                SELECT i.delivery_reference, c.customer_id, c.name customer_name, c.address,
                       p.publication_id, p.name publication_name, p.stock_quantity,
                       COALESCE(d.status, i.outcome) outcome
                FROM docket_item i
                JOIN subscription s ON s.subscription_id = i.subscription_id
                JOIN customer c ON c.customer_id = s.customer_id
                JOIN publication p ON p.publication_id = s.publication_id
                LEFT JOIN delivery d ON d.delivery_reference = i.delivery_reference
                WHERE i.docket_reference = ?
                ORDER BY c.customer_id, p.publication_id
                """;
        List<DeliveryDocketItem> items = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, docketReference);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    String outcome = result.getString("outcome");
                    items.add(new DeliveryDocketItem(result.getString("delivery_reference"),
                            result.getString("customer_id"), result.getString("customer_name"),
                            result.getString("address"), result.getString("publication_id"),
                            result.getString("publication_name"), result.getInt("stock_quantity"),
                            outcome == null ? null : DeliveryStatus.valueOf(outcome)));
                }
            }
        }
        return new DeliveryDocket(docketReference, date, area, items, List.of());
    }

    private String findOrCreateDocket(Connection connection, LocalDate date, int area) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT docket_reference FROM delivery_docket WHERE delivery_date = ? AND delivery_area = ? " +
                        "ORDER BY docket_reference LIMIT 1")) {
            statement.setString(1, date.toString());
            statement.setInt(2, area);
            try (var result = statement.executeQuery()) {
                if (result.next()) return result.getString(1);
            }
        }
        String reference = "DOCKET-" + date + "-A" + area;
        try (var statement = connection.prepareStatement(
                "INSERT INTO delivery_docket(docket_reference, delivery_date, delivery_area) VALUES(?,?,?)")) {
            statement.setString(1, reference);
            statement.setString(2, date.toString());
            statement.setInt(3, area);
            statement.executeUpdate();
        }
        return reference;
    }

    private void assignBillsToDocketCustomers(Connection connection, String docketReference, LocalDate date)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE bill SET docket_reference = ?
                WHERE docket_reference IS NULL AND billing_month <= ?
                  AND customer_id IN (
                    SELECT DISTINCT c.customer_id FROM docket_item i
                    JOIN subscription s ON s.subscription_id = i.subscription_id
                    JOIN customer c ON c.customer_id = s.customer_id
                    WHERE i.docket_reference = ?
                  )
                """)) {
            statement.setString(1, docketReference);
            statement.setString(2, YearMonth.from(date).toString());
            statement.setString(3, docketReference);
            statement.executeUpdate();
        }
    }

    private void assignOutstandingBills(Connection connection, String docketReference, String customerId,
                                        LocalDate date) throws SQLException {
        try (var statement = connection.prepareStatement(
                "UPDATE bill SET docket_reference=? WHERE customer_id=? AND docket_reference IS NULL AND billing_month<=?")) {
            statement.setString(1, docketReference);
            statement.setString(2, customerId);
            statement.setString(3, YearMonth.from(date).toString());
            statement.executeUpdate();
        }
    }

    private int findCustomerArea(Connection connection, String customerId) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT delivery_area FROM customer WHERE customer_id=?")) {
            statement.setString(1, customerId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Customer not found: " + customerId);
                return result.getInt(1);
            }
        }
    }

    private String findActiveSubscription(Connection connection, String customerId, String publicationId)
            throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT subscription_id FROM subscription
                WHERE customer_id=? AND publication_id=? AND paused=0
                ORDER BY subscription_id LIMIT 1
                """)) {
            statement.setString(1, customerId);
            statement.setString(2, publicationId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("No active customer subscription for publication");
                return result.getString(1);
            }
        }
    }

    private String findPublicationId(Connection connection, String subscriptionId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT publication_id FROM subscription WHERE subscription_id = ?")) {
            statement.setString(1, subscriptionId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
                return result.getString(1);
            }
        }
    }

    private void insertDocketItem(Connection connection, String reference, String docketReference,
                                  String subscriptionId, DeliveryStatus outcome) throws SQLException {
        try (var statement = connection.prepareStatement(
                "INSERT INTO docket_item(delivery_reference,docket_reference,subscription_id,outcome) VALUES(?,?,?,?)")) {
            statement.setString(1, reference);
            statement.setString(2, docketReference);
            statement.setString(3, subscriptionId);
            statement.setString(4, outcome == null ? null : outcome.name());
            statement.executeUpdate();
        }
    }

    private void insertOutcome(Connection connection, String reference, String docketReference,
                               String subscriptionId, DeliveryStatus status) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO delivery(delivery_reference,docket_reference,subscription_id,status)
                VALUES(?,?,?,?)
                ON CONFLICT(delivery_reference) DO UPDATE SET
                    docket_reference=excluded.docket_reference,
                    subscription_id=excluded.subscription_id,
                    status=excluded.status
                """)) {
            statement.setString(1, reference);
            statement.setString(2, docketReference);
            statement.setString(3, subscriptionId);
            statement.setString(4, status.name());
            statement.executeUpdate();
        }
    }

    private void insertOutOfStockRecord(Connection connection, String reference, String publicationId,
                                        LocalDate date) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO out_of_stock_record(delivery_reference,publication_id,recorded_at,note)
                VALUES(?,?,?,'Out of stock')
                ON CONFLICT(delivery_reference) DO UPDATE SET
                    publication_id=excluded.publication_id, recorded_at=excluded.recorded_at
                """)) {
            statement.setString(1, reference);
            statement.setString(2, publicationId);
            statement.setString(3, date.toString());
            statement.executeUpdate();
        }
    }

    private Delivery mapDelivery(ResultSet result) throws SQLException {
        Delivery delivery = new Delivery(result.getString("delivery_reference"),
                result.getString("customer_id"), result.getString("publication_id"));
        delivery.setStatus(DeliveryStatus.valueOf(result.getString("status")));
        delivery.setSummaryDetails(result.getString("customer_name"), result.getString("publication_name"),
                LocalDate.parse(result.getString("delivery_date")));
        return delivery;
    }

    private void validateDelivery(Delivery delivery) {
        Objects.requireNonNull(delivery, "delivery");
        if (delivery.getDeliveryReference() == null || delivery.getDeliveryReference().isBlank()
                || delivery.getCustomerId() == null || delivery.getPublicationId() == null
                || delivery.getStatus() == null) {
            throw new IllegalArgumentException("Delivery reference, customer, publication and status are required");
        }
    }

    private void validateArea(int deliveryArea) {
        if (deliveryArea < 1 || deliveryArea > 24) {
            throw new IllegalArgumentException("Delivery area must be between 1 and 24");
        }
    }
}
