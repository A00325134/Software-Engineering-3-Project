package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.SubscriptionDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.model.Subscription;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Persists subscriptions and their pause state in the subscription table. */
public class SqliteSubscriptionDao implements SubscriptionDao {
    private final SqliteDatabase database;

    public SqliteSubscriptionDao(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Subscription save(Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        validateFrequency(subscription.getDeliveryFrequency());
        requireText(subscription.getSubscriptionId(), "Subscription ID");
        requireText(subscription.getCustomerId(), "Customer ID");
        requireText(subscription.getPublicationId(), "Publication ID");
        String sql = "INSERT INTO subscription (subscription_id, customer_id, publication_id, " +
                "delivery_frequency, paused) VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(subscription_id) DO UPDATE SET customer_id = excluded.customer_id, " +
                "publication_id = excluded.publication_id, delivery_frequency = excluded.delivery_frequency, " +
                "paused = excluded.paused";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, subscription.getSubscriptionId());
            statement.setString(2, subscription.getCustomerId());
            statement.setString(3, subscription.getPublicationId());
            statement.setString(4, subscription.getDeliveryFrequency().name());
            statement.setInt(5, subscription.isPaused() ? 1 : 0);
            statement.executeUpdate();
            return subscription;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save subscription " + subscription.getSubscriptionId(), exception);
        }
    }

    @Override
    public boolean update(Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        validateFrequency(subscription.getDeliveryFrequency());
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(
                "UPDATE subscription SET customer_id = ?, publication_id = ?, delivery_frequency = ?, paused = ? WHERE subscription_id = ?")) {
            statement.setString(1, subscription.getCustomerId()); statement.setString(2, subscription.getPublicationId());
            statement.setString(3, subscription.getDeliveryFrequency().name()); statement.setInt(4, subscription.isPaused() ? 1 : 0);
            statement.setString(5, subscription.getSubscriptionId()); return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw new DatabaseException("Could not update subscription " + subscription.getSubscriptionId(), exception); }
    }

    @Override
    public Optional<Subscription> findById(String subscriptionId) {
        String sql = "SELECT subscription_id, customer_id, publication_id, delivery_frequency, paused " +
                "FROM subscription WHERE subscription_id = ?";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, subscriptionId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapSubscription(result)) : Optional.empty();
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new DatabaseException("Could not retrieve subscription " + subscriptionId, exception);
        }
    }

    @Override
    public List<Subscription> findByCustomerId(String customerId) {
        String sql = "SELECT subscription_id, customer_id, publication_id, delivery_frequency, paused " +
                "FROM subscription WHERE customer_id = ? ORDER BY subscription_id";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                List<Subscription> subscriptions = new ArrayList<>();
                while (result.next()) subscriptions.add(mapSubscription(result));
                return List.copyOf(subscriptions);
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new DatabaseException("Could not retrieve subscriptions for customer " + customerId, exception);
        }
    }

    @Override
    public List<Subscription> findAll() {
        String sql = "SELECT subscription_id, customer_id, publication_id, delivery_frequency, paused " +
                "FROM subscription ORDER BY subscription_id";
        try (var connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<Subscription> subscriptions = new ArrayList<>();
            while (result.next()) subscriptions.add(mapSubscription(result));
            return List.copyOf(subscriptions);
        } catch (SQLException | IllegalArgumentException exception) {
            throw new DatabaseException("Could not retrieve subscriptions", exception);
        }
    }

    @Override
    public boolean deleteById(String subscriptionId) {
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("DELETE FROM subscription WHERE subscription_id = ?")) {
            statement.setString(1, subscriptionId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not delete subscription " + subscriptionId, exception);
        }
    }

    private Subscription mapSubscription(ResultSet result) throws SQLException {
        DeliveryFrequency frequency = DeliveryFrequency.valueOf(result.getString("delivery_frequency"));
        Subscription subscription = new Subscription(result.getString("subscription_id"),
                result.getString("customer_id"), result.getString("publication_id"), frequency);
        subscription.setPaused(result.getInt("paused") == 1);
        return subscription;
    }

    private void validateFrequency(DeliveryFrequency frequency) {
        if (frequency == null) throw new IllegalArgumentException("Delivery frequency is required");
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
    }
}
