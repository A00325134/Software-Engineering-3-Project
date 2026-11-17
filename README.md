# Newsagent Management System

This is our Java project for managing some of the day-to-day work in a newsagent. The system uses a console menu, a service layer for the main rules, and SQLite for saving data. It is built with Java 17 and Maven, with JUnit 5 tests.

## What the project can do so far

The work completed for Weeks 1–6 covers the first three planned increments:

- **Week 1 – Project setup:** We set up the Java/Maven project, shared models and test structure, and the SQLite database connection and schema. The database turns on foreign-key checks for each connection.
- **Weeks 2–3 – Customers:** A user can create, view, list, edit and delete customer records. Customer input is checked for required fields, field lengths, a seven-character Eircode, and a delivery area from 1 to 24. The database prevents deleting customers that still have related history.
- **Week 4 – Delivery drivers:** A user can create, view, list, edit and delete driver profiles. The database prevents deleting drivers referenced by a delivery docket.
- **Week 5 – Publications and stock:** A user can create and view publications, see all publications, update stock and view low-stock items. The low-stock threshold is 5, as currently defined in the project schema. Stock can be reduced to zero but cannot be negative or exceed 1000.
- **Week 6 – Subscriptions:** A user can create, view, list, pause, resume and delete subscriptions. Subscriptions currently use monthly delivery frequency. Their paused state is saved in SQLite and remains after reopening the database.

The console calls the service layer, and services use DAOs for database work. This keeps SQL out of the user interface. The current menu and the supported workflows are for newsagent administration.

## Running the project

The main class is `ie.newsagent.ui.Main`. Run it from IntelliJ with the Maven dependencies available, or build and run it with Maven. Starting the application creates the database schema if needed.

```text
mvn test
```

The default database file is `data/newsagent.db`, relative to the working directory. To use another path, set the JVM property `-Dnewsagent.db=/path/to/newsagent.db`. Tests use their own temporary database files so they do not modify application data.

## Database and validation notes

The schema is in `src/main/resources/db/schema.sql`. It includes the customer, driver, publication, subscription, billing and delivery tables. Some billing and delivery models/tables are groundwork for later project increments; a complete billing or delivery workflow is not part of the Weeks 1–6 menu.

Customer delivery areas are entered explicitly. The project has 24 areas, but no address-to-area lookup data has been provided, so the application does not guess an area from an address. Driver profiles store a password hash field; password hashing and authentication are not implemented in this increment.

## Tests

Run the full test suite with `mvn test`. Tests cover SQLite schema and DAO behavior, customer and Week 4–6 service behavior, and console acceptance flows. Database tests use isolated temporary databases. The last full run completed with 49 tests passing and no failures, errors or skipped tests.

## Next increment

The nine-week plan places delivery summaries, billing and payment recording in Week 7. Delivery-driver docket and delivery-status features are planned for Week 8, followed by final system testing and submission preparation in Week 9.
