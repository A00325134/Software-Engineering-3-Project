package ie.newsagent.database;

/** Unchecked persistence error; distinguishes database failures from absent rows. */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
