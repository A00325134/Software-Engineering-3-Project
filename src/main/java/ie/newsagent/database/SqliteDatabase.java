package ie.newsagent.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/** Opens SQLite connections with foreign keys enabled and initialises the schema. */
public final class SqliteDatabase {
    public static final String DEFAULT_DATABASE_PATH = "data/newsagent.db";
    private final String jdbcUrl;

    public SqliteDatabase(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        if (!jdbcUrl.startsWith("jdbc:sqlite:")) {
            throw new IllegalArgumentException("A SQLite JDBC URL is required");
        }
    }

    public static SqliteDatabase fromPath(Path path) {
        Path absolutePath = path.toAbsolutePath();
        return new SqliteDatabase("jdbc:sqlite:" + absolutePath);
    }

    public static SqliteDatabase applicationDatabase() {
        Path path = Path.of(System.getProperty("newsagent.db", DEFAULT_DATABASE_PATH));
        return fromPath(path);
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            try (var result = statement.executeQuery("PRAGMA foreign_keys")) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new SQLException("SQLite foreign-key enforcement could not be enabled");
                }
            }
            return connection;
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public void initialiseSchema() {
        if (jdbcUrl.startsWith("jdbc:sqlite:") && !jdbcUrl.equals("jdbc:sqlite::memory:")) {
            String filename = jdbcUrl.substring("jdbc:sqlite:".length());
            if (!filename.startsWith("file:") && !filename.isBlank()) {
                Path file = Path.of(filename).toAbsolutePath();
                try {
                    Path parent = file.getParent();
                    if (parent != null) Files.createDirectories(parent);
                } catch (IOException exception) {
                    throw new DatabaseException("Could not create database directory", exception);
                }
            }
        }

        try (Connection connection = openConnection();
             InputStream schema = SqliteDatabase.class.getResourceAsStream("/db/schema.sql")) {
            if (schema == null) throw new SQLException("Database schema resource /db/schema.sql is missing");
            String sql = new String(schema.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                for (String command : sql.split(";")) {
                    String trimmed = command.trim();
                    if (!trimmed.isEmpty()) statement.execute(trimmed);
                }
            }
        } catch (SQLException | IOException exception) {
            throw new DatabaseException("Could not initialise SQLite schema", exception);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
