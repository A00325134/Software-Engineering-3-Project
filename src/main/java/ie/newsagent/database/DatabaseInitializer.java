package ie.newsagent.database;

/** Application startup hook for creating the configured SQLite database and schema. */
public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void initialiseApplicationDatabase() {
        SqliteDatabase.applicationDatabase().initialiseSchema();
    }
}
