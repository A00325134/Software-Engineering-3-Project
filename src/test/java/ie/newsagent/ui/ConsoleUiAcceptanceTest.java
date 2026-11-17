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
        assertTrue(output.contains("area 1"));
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
                        "1\n1\n1\n3\n"+customerId+"\n1\n5\n"+customerId+"\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(output.contains("Updated "+customerId));
        assertTrue(output.contains("Alex Updated"));
        assertTrue(output.contains("Customer deleted."));
        assertTrue(customers.getAll().isEmpty());
    }

    @Test void driverAndStockFlowsWorkThroughSQLiteMenus() {
        var db=SqliteDatabase.fromPath(dir.resolve("driver-stock-flow.db"));
        db.initialiseSchema();
        var customers=new CustomerService(new SqliteCustomerDao(db));
        var drivers=new DriverService(new SqliteDriverDao(db));
        var publications=new PublicationService(new SqlitePublicationDao(db));
        var subscriptions=new SubscriptionService(new SqliteSubscriptionDao(db));
        String driverOutput=run("2\n2\nD1\nSam Driver\nhash\n2\n4\nD1\nSam Updated\nhash2\n" +
                        "2\n3\nD1\n2\n5\nD1\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(driverOutput.contains("Updated D1"));
        assertTrue(driverOutput.contains("Sam Updated"));
        assertTrue(driverOutput.contains("Driver deleted."));
        assertTrue(drivers.getAll().isEmpty());

        String stockOutput=run("3\n2\nP1\nDaily News\n10\n3\n4\nP1\n0\n3\n5\n0\n",
                customers,drivers,publications,subscriptions);
        assertTrue(stockOutput.contains("Stock updated to 0"));
        assertTrue(stockOutput.contains("P1 | Daily News | stock 0"));
        assertEquals(0,publications.get("P1").getStockQuantity());
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
}
