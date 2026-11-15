package ie.newsagent.ui;

import ie.newsagent.dao.memory.InMemoryCustomerDao;
import ie.newsagent.dao.memory.InMemoryPublicationDao;
import ie.newsagent.dao.sqlite.*;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleUiAcceptanceTest {
    @TempDir Path dir;
    @Test void customerCreationAndValidationFeedbackAreAccessibleFromMenu() {
        var customers=new CustomerService(new InMemoryCustomerDao());
        var output=run("1\n2\nAlex Murphy\n0871234567\n10 Main Street\nD02X285\n1\n1\n1\n0\n",customers);
        assertTrue(output.contains("Created customer"));
        assertTrue(output.contains("Alex Murphy"));
        assertTrue(output.contains("area 3"));
    }
    @Test void customerCreateEditRetrieveAndDeleteWorkThroughSQLiteMenus() {
        var db=SqliteDatabase.fromPath(dir.resolve("customer-flow.db"));
        db.initialiseSchema();
        var customers=new CustomerService(new SqliteCustomerDao(db));
        var drivers=new DriverService(new SqliteDriverDao(db));
        var publications=new PublicationService(new SqlitePublicationDao(db));
        var subscriptions=new SubscriptionService(new SqliteSubscriptionDao(db));
        String created=run("1\n2\nAlex Murphy\n0871234567\n10 Main Street\nD02X285\n1\n0\n",
                customers,drivers,publications,subscriptions);
        Matcher idMatcher=Pattern.compile("Created customer (\\w+)").matcher(created);
        assertTrue(idMatcher.find(),created);
        String customerId=idMatcher.group(1);

        String output=run("1\n4\n"+customerId+"\nAlex Updated\n0861234567\n12 Main Street\nD02X286\n" +
                        "1\n1\n1\n3\n"+customerId+"\n1\n5\n"+customerId+"\ny\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(output.contains("Updated "+customerId));
        assertTrue(output.contains("Alex Updated"));
        assertTrue(output.contains("Customer deleted."));
        assertTrue(customers.getAll().isEmpty());
    }

    @Test void customerDeletionCanBeCancelledFromTheMenu() {
        var db=SqliteDatabase.fromPath(dir.resolve("cancel-delete.db"));db.initialiseSchema();
        var customers=new CustomerService(new SqliteCustomerDao(db));
        var customer=customers.create("Alex Murphy","0871234567","10 Main Street","D02X285",1);
        String output=run("1\n5\n"+customer.getCustomerId()+"\nn\n0\n",customers);
        assertTrue(output.contains("Deletion cancelled."));
        assertEquals(customer.getCustomerId(),customers.get(customer.getCustomerId()).getCustomerId());
    }

    @Test void driverAndStockFlowsWorkThroughSQLiteMenus() {
        var db=SqliteDatabase.fromPath(dir.resolve("driver-stock-flow.db"));
        db.initialiseSchema();
        var customers=new CustomerService(new SqliteCustomerDao(db));
        var drivers=new DriverService(new SqliteDriverDao(db));
        var publications=new PublicationService(new SqlitePublicationDao(db));
        var subscriptions=new SubscriptionService(new SqliteSubscriptionDao(db));
        String driverOutput=run("2\n2\nD1\nSam Driver\nhash\n2\n4\nD1\nSam Updated\nhash2\n" +
                        "2\n3\nD1\n2\n5\nD1\ny\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(driverOutput.contains("Updated D1"));
        assertTrue(driverOutput.contains("Sam Updated"));
        assertTrue(driverOutput.contains("Driver deleted."));
        assertTrue(drivers.getAll().isEmpty());

        String stockOutput=run("3\n2\nP1\nDaily News\n10\n3\n4\nP1\nEvening Paper\n3\n5\nP1\n0\n3\n6\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(stockOutput.contains("Stock updated to 0"));
        assertTrue(stockOutput.contains("Updated P1"));
        assertTrue(stockOutput.contains("P1 | Evening Paper | stock 0"));
        assertEquals(0,publications.get("P1").getStockQuantity());
        assertEquals("Evening Paper",publications.get("P1").getName());
    }
    @Test void subscriptionPauseAndResumeAreAccessibleAndPersisted() {
        var db=SqliteDatabase.fromPath(dir.resolve("ui.db")); db.initialiseSchema();
        var c=new SqliteCustomerDao(db); c.save(new ie.newsagent.model.Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        new SqlitePublicationDao(db).save(new ie.newsagent.model.Publication("P1","Daily News",20));
        var subscriptions=new SubscriptionService(new SqliteSubscriptionDao(db));
        subscriptions.create("S1","C1234","P1",ie.newsagent.model.DeliveryFrequency.MONTHLY);
        String output=run("4\n4\nS1\n4\n5\nS1\n4\n1\n0\n",new CustomerService(c),new DriverService(new SqliteDriverDao(db)),new PublicationService(new SqlitePublicationDao(db)),subscriptions);
        assertTrue(output.contains("Paused S1"));
        assertTrue(output.contains("Resumed S1"));
        assertTrue(output.contains("active"));
        assertFalse(subscriptions.get("S1").isPaused());
    }

    @Test void databaseErrorsAreShownInTheConsoleInsteadOfTerminatingTheApplication() {
        var db=SqliteDatabase.fromPath(dir.resolve("missing-schema.db"));
        var output=run("1\n3\nC1234\n0\n",new CustomerService(new SqliteCustomerDao(db)),
                new DriverService(new SqliteDriverDao(db)),new PublicationService(new InMemoryPublicationDao()),
                new SubscriptionService(new SqliteSubscriptionDao(db)));

        assertTrue(output.contains("Error: Could not retrieve customer C1234"));
        assertTrue(output.contains("Newsagent Management System"));
    }
    @Test void deliverySummaryAndBillPaymentFlowsAreVisibleInConsole() {
        var db=SqliteDatabase.fromPath(dir.resolve("week7-ui.db"));db.initialiseSchema();
        var customers=new SqliteCustomerDao(db);customers.save(new ie.newsagent.model.Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        customers.save(new ie.newsagent.model.Customer("C5678","Jamie Kelly","0877654321","2 Main Road","D01X111",2));
        new SqlitePublicationDao(db).save(new ie.newsagent.model.Publication("P1","Daily News",20));
        new SqliteSubscriptionDao(db).save(new ie.newsagent.model.Subscription("S1","C1234","P1",ie.newsagent.model.DeliveryFrequency.MONTHLY));
        var d=new ie.newsagent.model.Delivery("D1","C1234","P1");d.setStatus(ie.newsagent.model.DeliveryStatus.FAILED);d.setDeliveryDate(java.time.LocalDate.now());
        var deliveryDao=new SqliteDeliveryDao(db);deliveryDao.save(d);
        var billing=new BillingService(new SqliteBillDao(db));
        var bill=billing.generate("C1234",java.time.YearMonth.now(),java.util.List.of(new ie.newsagent.model.BillItem("Daily News",java.time.LocalDate.now(),java.math.BigDecimal.ONE)));
        String input="6\n5\n1\nC1234\n5\n2\n"+bill.getBillReference()+"\n0\n";
        var output=run(input,new CustomerService(customers),new DriverService(new SqliteDriverDao(db)),new PublicationService(new SqlitePublicationDao(db)),new SubscriptionService(new SqliteSubscriptionDao(db)),billing,new PaymentService(new SqliteBillDao(db)),new DeliverySummaryService(customers,deliveryDao));
        assertTrue(output.contains("Alex Murphy"));assertTrue(output.contains("Daily News | FAILED"));
        assertTrue(output.contains("total 1"));assertTrue(output.contains("Payment status: PAID"));
    }

    @Test void driverCanPrintADocketSaveOutOfStockAndNewsagentCanSeeTheResult() {
        var db=SqliteDatabase.fromPath(dir.resolve("driver-docket-flow.db"));db.initialiseSchema();
        var customers=new SqliteCustomerDao(db);
        customers.save(new ie.newsagent.model.Customer("C1234","Alex Murphy","0871234567",
                "10 Main Street","D02X285",1));
        new SqlitePublicationDao(db).save(new ie.newsagent.model.Publication("P1","Daily News",4));
        new SqliteSubscriptionDao(db).save(new ie.newsagent.model.Subscription(
                "S1","C1234","P1",ie.newsagent.model.DeliveryFrequency.MONTHLY));
        var deliveries=new SqliteDeliveryDao(db);
        var dockets=new DeliveryDocketService(deliveries,new SqliteBillDao(db));
        var docket=dockets.generate(java.time.LocalDate.of(2026,11,3),1);
        String reference=docket.items().get(0).deliveryReference();

        String output=run("7\n2\n2026-11-03\n1\n7\n3\n2026-11-03\n1\n"+reference+"\n3\n6\n0\n",
                new CustomerService(customers),new DriverService(new SqliteDriverDao(db)),
                new PublicationService(new SqlitePublicationDao(db)),
                new SubscriptionService(new SqliteSubscriptionDao(db)),null,null,
                new DeliverySummaryService(customers,deliveries),dockets);

        assertTrue(output.contains("Printable docket:"));
        assertTrue(output.contains("Alex Murphy | 10 Main Street"));
        assertTrue(output.contains("Daily News"));
        assertTrue(output.contains("Saved delivery status: OUT_OF_STOCK"));
        assertTrue(output.contains("OUT_OF_STOCK"));
        assertEquals(ie.newsagent.model.DeliveryStatus.OUT_OF_STOCK,
                new SqliteDeliveryDao(db).findByReference(reference).orElseThrow().getStatus());
    }

    @Test void driverReceivesAnErrorForAnInvalidDeliveryArea() {
        var db=SqliteDatabase.fromPath(dir.resolve("invalid-driver-area.db"));db.initialiseSchema();
        String output=run("7\n1\n2026-11-03\n25\n0\n",new CustomerService(new SqliteCustomerDao(db)),
                new DriverService(new SqliteDriverDao(db)),new PublicationService(new SqlitePublicationDao(db)),
                new SubscriptionService(new SqliteSubscriptionDao(db)),null,null,
                new DeliverySummaryService(new SqliteCustomerDao(db),new SqliteDeliveryDao(db)),
                new DeliveryDocketService(new SqliteDeliveryDao(db),new SqliteBillDao(db)));
        assertTrue(output.contains("Error: Delivery area must be between 1 and 24"));
    }

    @Test void publicationDeletionCanBeCancelledOrConfirmed() {
        var db=SqliteDatabase.fromPath(dir.resolve("publication-delete.db"));db.initialiseSchema();
        var customers=new CustomerService(new SqliteCustomerDao(db));
        var drivers=new DriverService(new SqliteDriverDao(db));
        var publications=new PublicationService(new SqlitePublicationDao(db));
        var subscriptions=new SubscriptionService(new SqliteSubscriptionDao(db));
        publications.create("P1","Daily News",10);
        String output=run("3\n7\nP1\nn\n3\n3\nP1\n3\n7\nP1\ny\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(output.contains("Deletion cancelled."));
        assertTrue(output.contains("Daily News"));
        assertTrue(output.contains("Publication deleted."));
        assertTrue(publications.getAll().isEmpty());
    }
    private String run(String input, CustomerService customers) {
        return run(input,customers,new DriverService(new SqliteDriverDao(database())),new PublicationService(new InMemoryPublicationDao()),new SubscriptionService(new SqliteSubscriptionDao(database())));
    }
    private SqliteDatabase database() { var db=SqliteDatabase.fromPath(dir.resolve("menu.db")); db.initialiseSchema(); return db; }
    private String run(String input, CustomerService customers, DriverService drivers, PublicationService publications, SubscriptionService subscriptions) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); PrintStream original=System.out;
        try { System.setOut(new PrintStream(bytes,true,StandardCharsets.UTF_8)); Main.run(new Scanner(input),customers,drivers,publications,subscriptions); }
        finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }
    private String run(String input,CustomerService customers,DriverService drivers,PublicationService publications,SubscriptionService subscriptions,BillingService billing,PaymentService payments,DeliverySummaryService summary) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); PrintStream original=System.out;
        try { System.setOut(new PrintStream(bytes,true,StandardCharsets.UTF_8)); Main.run(new Scanner(input),customers,drivers,publications,subscriptions,billing,payments,summary); }
        finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }
    private String run(String input,CustomerService customers,DriverService drivers,PublicationService publications,
                       SubscriptionService subscriptions,BillingService billing,PaymentService payments,
                       DeliverySummaryService summary,DeliveryDocketService dockets) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); PrintStream original=System.out;
        try { System.setOut(new PrintStream(bytes,true,StandardCharsets.UTF_8)); Main.run(new Scanner(input),customers,drivers,publications,subscriptions,billing,payments,summary,dockets); }
        finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
