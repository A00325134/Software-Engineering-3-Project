package ie.newsagent.service;

import ie.newsagent.dao.sqlite.*;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class WeekFourToSixServiceTest {
    @TempDir Path dir;
    SqliteDatabase db;
    @BeforeEach void setup() { db=SqliteDatabase.fromPath(dir.resolve("app.db")); db.initialiseSchema(); }

    @Test void driverCrudUsesValidatedServiceAndPersistsEdits() {
        var service=new DriverService(new SqliteDriverDao(db));
        service.create("D1","Sam Driver","hash");
        assertEquals("Sam Updated",service.update("D1","Sam Updated","hash2").getName());
        assertEquals("Sam Updated",service.get("D1").getName());
        assertThrows(IllegalArgumentException.class,()->service.create("D2"," ","hash"));
        service.delete("D1"); assertTrue(service.getAll().isEmpty());
    }
    @Test void publicationStockChangesAndLowStockFollowFiveItemThreshold() {
        var service=new PublicationService(new SqlitePublicationDao(db));
        service.create("P1","Daily News",10);
        service.create("P2","Evening News",10);
        service.updateStock("P1",0);
        service.updateStock("P2",4);
        assertEquals(0,service.get("P1").getStockQuantity());
        assertEquals("P1",service.lowStock().get(0).getPublicationId());
        assertEquals(2,service.lowStock().size());
        service.updateStock("P2",5);
        assertEquals(1,service.lowStock().size());
        assertThrows(IllegalArgumentException.class,()->service.updateStock("P1",-1));
        assertThrows(IllegalArgumentException.class,()->service.updateStock("P1",1001));
    }
    @Test void subscriptionPauseAndResumePersist() {
        new SqliteCustomerDao(db).save(new Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        new SqlitePublicationDao(db).save(new Publication("P1","Daily News",20));
        var service=new SubscriptionService(new SqliteSubscriptionDao(db));
        service.create("S1","C1234","P1",DeliveryFrequency.MONTHLY);
        assertTrue(service.pause("S1").isPaused());
        assertTrue(service.get("S1").isPaused());
        assertFalse(service.resume("S1").isPaused());
        assertThrows(IllegalArgumentException.class,()->service.pause("missing"));
    }
}
