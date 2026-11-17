package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.CustomerDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.DuplicateCustomerException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Customer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** SQLite implementation of customer persistence. */
public class SqliteCustomerDao implements CustomerDao {
    private static final String COLUMNS = "customer_id, name, phone_number, address, eircode, delivery_area";
    private final SqliteDatabase database;

    public SqliteCustomerDao(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Customer save(Customer customer) {
        Objects.requireNonNull(customer, "customer");
        String sql = "INSERT INTO customer (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (var connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getCustomerId());
            statement.setString(2, customer.getName());
            statement.setString(3, customer.getPhoneNumber());
            statement.setString(4, customer.getAddress());
            statement.setString(5, customer.getEircode());
            statement.setInt(6, customer.getDeliveryArea());
            statement.executeUpdate();
            return customer;
        } catch (SQLException exception) {
            if (isDuplicateCustomerId(exception)) {
                throw new DuplicateCustomerException(customer.getCustomerId(), exception);
            }
            throw new DatabaseException("Could not save customer " + customer.getCustomerId(), exception);
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) {
        String sql = "SELECT " + COLUMNS + " FROM customer WHERE customer_id = ?";
        try (var connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapCustomer(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve customer " + customerId, exception);
        }
    }

    @Override
    public boolean updateById(String customerId, Customer customer) {
        Objects.requireNonNull(customer, "customer");
        if (!Objects.equals(customerId, customer.getCustomerId())) {
            throw new IllegalArgumentException("Customer ID cannot be changed during an update");
        }
        String sql = "UPDATE customer SET name = ?, phone_number = ?, address = ?, eircode = ?, " +
                "delivery_area = ? WHERE customer_id = ?";
        try (var connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customer.getName());
            statement.setString(2, customer.getPhoneNumber());
            statement.setString(3, customer.getAddress());
            statement.setString(4, customer.getEircode());
            statement.setInt(5, customer.getDeliveryArea());
            statement.setString(6, customerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update customer " + customerId, exception);
        }
    }

    @Override
    public List<Customer> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM customer ORDER BY customer_id";
        try (var connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<Customer> customers = new ArrayList<>();
            while (result.next()) customers.add(mapCustomer(result));
            return List.copyOf(customers);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve customers", exception);
        }
    }

    @Override
    public boolean deleteById(String customerId) {
        String sql = "DELETE FROM customer WHERE customer_id = ?";
        try (var connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not delete customer " + customerId, exception);
        }
    }

    private Customer mapCustomer(ResultSet result) throws SQLException {
        return new Customer(result.getString("customer_id"), result.getString("name"),
                result.getString("phone_number"), result.getString("address"),
                result.getString("eircode"), result.getInt("delivery_area"));
    }

    private boolean isDuplicateCustomerId(SQLException exception) {
        return exception.getErrorCode() == 19
                && exception.getMessage() != null
                && exception.getMessage().contains("customer.customer_id");
    }
}
