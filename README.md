# Newsagent Management System

This is our Java project for managing the day-to-day operations of a newsagent. I worked on the project as part of a three-person Software Engineering team, developing the database, application functionality and user interface across the nine-week project.

The system uses a console-based interface, Java services for business logic and SQLite for storing application data. It is built using **Java 17, Maven, JDBC and JUnit 5**.

## What the system does

The project has been developed through Week 9 and covers the following functionality:

- **Customer Management:** Create, view, edit and delete customer records, with input validation and database persistence.
- **Delivery Driver Management:** Create and manage delivery driver profiles.
- **Publications and Stock:** Manage publications, update stock quantities and identify low-stock publications.
- **Subscriptions:** Create and manage customer subscriptions, including pausing and resuming deliveries.
- **Delivery Summaries:** View completed deliveries, failed deliveries and out-of-stock publications.
- **Monthly Billing:** Generate customer bills based on delivered publications, display item descriptions, delivery dates and monthly totals.
- **Bill Payments:** Record payments, display payment status and identify unpaid bills requiring reminders.
- **Daily Delivery Dockets:** Generate daily delivery dockets for the 24 delivery areas, showing customer details, addresses and required publications.
- **Delivery Status:** Allow drivers to record deliveries as Delivered, Failed or Out of Stock.
- **Final Integration:** Connect the Newsagent and Delivery Driver workflows so that delivery results are saved and can be viewed by the Newsagent.

## Technologies and architecture

The project uses:

- **Java 17** – Main application language.
- **Maven** – Project and dependency management.
- **SQLite** – Persistent database storage.
- **JDBC** – Communication between Java and SQLite.
- **JUnit 5** – Unit, database and integration testing.

The application follows a simple layered structure:

```text
User Interface
      |
      v
Service Layer
      |
      v
DAO Layer
      |
      v
SQLite Database
```

The UI handles user interaction, the service layer contains the main application rules, and the DAO layer handles database operations. This keeps database queries separate from the user interface.

## Running the project

The main application class is:

```text
ie.newsagent.ui.Main
```

The project can be opened and run through IntelliJ with its Maven dependencies available.

To run the test suite:

```bash
mvn test
```

The default SQLite database is:

```text
data/newsagent.db
```

This path is relative to the application's working directory. A different database location can be supplied using the JVM property:

```text
-Dnewsagent.db=/path/to/newsagent.db
```

The database schema is located at:

```text
src/main/resources/db/schema.sql
```

The application initialises the database schema when needed. Tests use separate temporary databases so they do not interfere with the normal application data.

## Testing

JUnit 5 is used throughout the project to test database operations, service functionality and application workflows.

Testing covers areas such as:

- Valid and invalid user input.
- Creating, retrieving, updating and deleting records.
- Database relationships and constraints.
- Subscription and stock business rules.
- Delivery docket generation and delivery outcomes.
- Monthly billing and payment status.
- Persistence after reconnecting to SQLite.
- Integration between the Newsagent and Delivery Driver functionality.

The complete test suite can be run using `mvn test`.

## Project completion

The project has now been developed through **Week 9**, covering the planned functionality from the initial project setup through customer management, subscriptions, stock management, reporting, billing, delivery dockets and final integration.

The Git commit history has also been reorganised by me as part of the final project cleanup. Because of this, some commits may display recent dates reflecting when the history was reorganised rather than when the original work was carried out. Feel free to inspect the earlier commit history, merge records and commit details for more context about the original development timeline.
