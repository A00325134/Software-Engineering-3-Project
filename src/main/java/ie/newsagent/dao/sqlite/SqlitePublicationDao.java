package ie.newsagent.dao.sqlite;

import ie.newsagent.dao.PublicationDao;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Publication;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Persists publication records and stock quantities in the publication table. */
public class SqlitePublicationDao implements PublicationDao {
    private final SqliteDatabase database;

    public SqlitePublicationDao(SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Publication save(Publication publication) {
        Objects.requireNonNull(publication, "publication");
        if (publication.getPublicationId() == null || publication.getPublicationId().isBlank()) {
            throw new IllegalArgumentException("Publication ID is required");
        }
        if (publication.getName() == null || publication.getName().trim().length() < 3 ||
                publication.getName().trim().length() > 50) {
            throw new IllegalArgumentException("Publication name must be 3-50 characters");
        }
        if (publication.getStockQuantity() < 0 || publication.getStockQuantity() > 1000) {
            throw new IllegalArgumentException("Stock quantity must be between 0 and 1000");
        }
        if (publication.getLowStockThreshold() != 5) {
            throw new IllegalArgumentException("Low-stock threshold is 5");
        }
        String sql = "INSERT INTO publication (publication_id, name, stock_quantity, low_stock_threshold) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT(publication_id) DO UPDATE SET name = excluded.name, " +
                "stock_quantity = excluded.stock_quantity, low_stock_threshold = excluded.low_stock_threshold";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, publication.getPublicationId());
            statement.setString(2, publication.getName());
            statement.setInt(3, publication.getStockQuantity());
            statement.setInt(4, publication.getLowStockThreshold());
            statement.executeUpdate();
            return publication;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not save publication " + publication.getPublicationId(), exception);
        }
    }

    @Override
    public boolean updateName(String publicationId, String name) {
        if (name == null || name.trim().length() < 3 || name.trim().length() > 50) {
            throw new IllegalArgumentException("Publication name must be 3-50 characters");
        }
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(
                "UPDATE publication SET name = ? WHERE publication_id = ?")) {
            statement.setString(1, name.trim());
            statement.setString(2, publicationId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not update publication " + publicationId, exception);
        }
    }

    @Override
    public boolean updateStock(String publicationId, int stockQuantity) {
        if (stockQuantity < 0 || stockQuantity > 1000) throw new IllegalArgumentException("Stock quantity must be between 0 and 1000");
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(
                "UPDATE publication SET stock_quantity = ? WHERE publication_id = ?")) {
            statement.setInt(1, stockQuantity); statement.setString(2, publicationId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) { throw new DatabaseException("Could not update stock for " + publicationId, exception); }
    }

    @Override
    public Optional<Publication> findById(String publicationId) {
        String sql = "SELECT publication_id, name, stock_quantity, low_stock_threshold FROM publication " +
                "WHERE publication_id = ?";
        try (var connection = database.openConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, publicationId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPublication(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve publication " + publicationId, exception);
        }
    }

    @Override
    public List<Publication> findAll() {
        String sql = "SELECT publication_id, name, stock_quantity, low_stock_threshold " +
                "FROM publication ORDER BY publication_id";
        try (var connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<Publication> publications = new ArrayList<>();
            while (result.next()) publications.add(mapPublication(result));
            return List.copyOf(publications);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve publications", exception);
        }
    }

    @Override
    public boolean deleteById(String publicationId) {
        try (var connection = database.openConnection();
             var statement = connection.prepareStatement("DELETE FROM publication WHERE publication_id = ?")) {
            statement.setString(1, publicationId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DatabaseException("Could not delete publication " + publicationId, exception);
        }
    }

    @Override
    public List<Publication> findLowStock() {
        String sql = "SELECT publication_id, name, stock_quantity, low_stock_threshold FROM publication " +
                "WHERE stock_quantity < low_stock_threshold ORDER BY publication_id";
        try (var connection = database.openConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            List<Publication> publications = new ArrayList<>();
            while (result.next()) publications.add(mapPublication(result));
            return List.copyOf(publications);
        } catch (SQLException exception) {
            throw new DatabaseException("Could not retrieve low-stock publications", exception);
        }
    }

    private Publication mapPublication(ResultSet result) throws SQLException {
        Publication publication = new Publication(result.getString("publication_id"),
                result.getString("name"), result.getInt("stock_quantity"));
        publication.setLowStockThreshold(result.getInt("low_stock_threshold"));
        return publication;
    }
}
