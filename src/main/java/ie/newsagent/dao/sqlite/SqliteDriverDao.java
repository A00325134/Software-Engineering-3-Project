package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.DriverDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.DeliveryDriver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Persists delivery-driver profiles in the delivery_driver table. */
public class SqliteDriverDao implements DriverDao {
    private final SqliteDatabase database;

    public SqliteDriverDao(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public DeliveryDriver save(DeliveryDriver driver) {
        Objects.requireNonNull(driver, "driver");
        requireText(driver.getDriverId(), "Driver ID");
        requireText(driver.getName(), "Driver name");
        requireText(driver.getPasswordHash(), "Password hash");
        String sql = "INSERT INTO delivery_driver (driver_id, name, password_hash) VALUES (?, ?, ?) " +
                "ON CONFLICT(driver_id) DO UPDATE SET name = excluded.name, password_hash = excluded.password_hash";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, driver.getDriverId());
            statement.setString(2, driver.getName());
            statement.setString(3, driver.getPasswordHash());
            statement.executeUpdate();
            return driver;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save delivery driver " + driver.getDriverId(), exception);
        }
    }

    @Override
    public boolean updateById(String driverId, DeliveryDriver driver) {
        Objects.requireNonNull(driver, "driver");
        if (!Objects.equals(driverId, driver.getDriverId())) throw new IllegalArgumentException("Driver ID cannot change");
        requireText(driver.getName(), "Driver name");
        requireText(driver.getPasswordHash(), "Password hash");
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(
                "UPDATE delivery_driver SET name = ?, password_hash = ? WHERE driver_id = ?")) {
            statement.setString(1, driver.getName()); statement.setString(2, driver.getPasswordHash());
            statement.setString(3, driverId); return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw new DatabaseException("Could not update delivery driver " + driverId, exception); }
    }

    @Override
    public Optional<DeliveryDriver> findById(String driverId) {
        String sql = "SELECT driver_id, name, password_hash FROM delivery_driver WHERE driver_id = ?";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, driverId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapDriver(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve delivery driver " + driverId, exception);
        }
    }

    @Override
    public List<DeliveryDriver> findAll() {
        String sql = "SELECT driver_id, name, password_hash FROM delivery_driver ORDER BY driver_id";
        try (var connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<DeliveryDriver> drivers = new ArrayList<>();
            while (result.next()) drivers.add(mapDriver(result));
            return List.copyOf(drivers);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve delivery drivers", exception);
        }
    }

    @Override
    public boolean deleteById(String driverId) {
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("DELETE FROM delivery_driver WHERE driver_id = ?")) {
            statement.setString(1, driverId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not delete delivery driver " + driverId, exception);
        }
    }

    private DeliveryDriver mapDriver(ResultSet result) throws SQLException {
        return new DeliveryDriver(result.getString("driver_id"), result.getString("name"),
                result.getString("password_hash"));
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }
}
