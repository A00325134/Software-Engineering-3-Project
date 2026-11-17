# Newsagent Management System

Java 17, Maven, SQLite, and JUnit 5 project. The existing console entry point is `ie.newsagent.ui.Main`; its feature menus remain placeholders while the planned increments are developed.

## Database

The SQLite schema is in `src/main/resources/db/schema.sql`. It defines customers, delivery drivers, publications and stock, subscriptions, bills and bill items, delivery dockets, deliveries, and out-of-stock records. Subscriptions reference customers and publications; bills reference customers and bill items reference bills; deliveries reference dockets and subscriptions; out-of-stock records reference deliveries and publications. Foreign keys use `RESTRICT` so deleting a parent cannot silently erase billing or delivery history. Customer fields and their constraints follow the customer acceptance criteria, including string IDs, 1–24 delivery areas, and exactly seven Eircode characters.

At startup `DatabaseInitializer` creates the schema safely using `CREATE TABLE IF NOT EXISTS`. `SqliteDatabase.openConnection()` enables and verifies SQLite foreign-key enforcement on every connection. The default database file is `data/newsagent.db` under the working directory. Override it with the JVM property `-Dnewsagent.db=/path/to/newsagent.db`. No credentials or secrets are stored in the source.

Run tests with `mvn test`. Run the application from IntelliJ by running `ie.newsagent.ui.Main` with Maven dependencies on the classpath; startup creates `data/newsagent.db`. To inspect the database in IntelliJ, open the Database tool window, add a SQLite data source, and select `data/newsagent.db`; any SQLite viewer can open the same file.

## Delivery-area mapping

The project documents 24 delivery areas but provides no address, Eircode, or geographic mapping. `DeliveryAreaAssigner` defines the mapping seam, and `MapDeliveryAreaAssigner` performs exact-address lookups against supplied mapping data while rejecting blank or unmapped addresses. No address-to-area mapping has been guessed. Customer create/update calls in the existing service still accept an explicit area; once project mapping data and unknown-address behavior are agreed, that strategy can be injected so address changes recalculate the area.

## Week 1 and Week 2 status

- **Week 1 foundation:** SQLite connection management, repeatable schema initialization, database constraints and relationships, future entity tables, and existing DAO contracts are established.
- **Week 2 US2–US3 persistence:** the SQLite customer DAO creates, retrieves by string ID, lists, and provides an isolated delete contract consistent with the existing DAO. Duplicate IDs and database failures have distinct exceptions; unknown IDs return `Optional.empty()`.
- **Prepared for later work:** the schema and DAO contracts can support subscriptions, billing, dockets, deliveries, and stock outcomes. Their business operations are not implemented here.
- **Scheduled later:** full customer update/delete service behavior, UI integration, address mapping, and full customer CRUD acceptance tests are Week 3 work. The existing service contains update/delete methods from the skeleton but they are not claimed as a completed increment.

The Java `Customer` fields map directly to `customer.customer_id`, `name`, `phone_number`, `address`, `eircode`, and `delivery_area`. Existing model classes suggest the future fields used for the additional tables; those tables are schema groundwork, not a claim that their full requirements are settled.
