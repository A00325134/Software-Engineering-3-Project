package ie.newsagent.ui;

import ie.newsagent.dao.memory.InMemoryCustomerDao;
import ie.newsagent.dao.memory.InMemoryPublicationDao;
import ie.newsagent.dao.sqlite.*;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;
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
    @Test void subscriptionPauseAndResumeAreAccessibleAndPersisted() {
        var db=SqliteDatabase.fromPath(dir.resolve("ui.db")); db.initialiseSchema();
        var c=new SqliteCustomerDao(db); c.save(new ie.newsagent.model.Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        new SqlitePublicationDao(db).save(new ie.newsagent.model.Publication("P1","Daily News",20));
        var subscriptions=new SubscriptionService(new SqliteSubscriptionDao(db));
        subscriptions.create("S1","C1234","P1",ie.newsagent.model.DeliveryFrequency.MONTHLY);
        String output=run("4\n4\nS1\n4\n1\n0\n",new CustomerService(c),new DriverService(new SqliteDriverDao(db)),new PublicationService(new SqlitePublicationDao(db)),subscriptions);
        assertTrue(output.contains("Paused S1"));
        assertTrue(output.contains("paused"));
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
}
