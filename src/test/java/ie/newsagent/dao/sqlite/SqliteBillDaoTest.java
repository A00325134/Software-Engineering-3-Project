package ie.newsagent.dao.sqlite;

import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SqliteBillDaoTest {
    @TempDir Path dir;
    @Test void savesBillAndItemsAcrossReconnectAndPersistsPayment() {
        var db=SqliteDatabase.fromPath(dir.resolve("billing.db")); db.initialiseSchema();
        new SqliteCustomerDao(db).save(new Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        var dao=new SqliteBillDao(db);
        var bill=new Bill("B1","C1234", YearMonth.of(2026,10), List.of(new BillItem("Daily News",LocalDate.of(2026,10,3),new BigDecimal("2.50"))),new BigDecimal("2.50"));
        dao.save(bill);
        assertEquals("Daily News",new SqliteBillDao(db).findByReference("B1").orElseThrow().getItems().get(0).itemName());
        assertTrue(dao.markPaid("B1"));
        assertEquals(BillPaymentStatus.PAID,new SqliteBillDao(db).findByReference("B1").orElseThrow().getPaymentStatus());
    }
    @Test void sqliteRejectsBadItemDescriptionAndDuplicateCustomerMonth() {
        var db=SqliteDatabase.fromPath(dir.resolve("constraints.db")); db.initialiseSchema();
        new SqliteCustomerDao(db).save(new Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        var dao=new SqliteBillDao(db);
        var b=new Bill("B1","C1234",YearMonth.of(2026,10),List.of(new BillItem("ok",LocalDate.now(),BigDecimal.ONE)),BigDecimal.ONE);
        assertThrows(RuntimeException.class,()->dao.save(b));
        dao.save(new Bill("B2","C1234",YearMonth.of(2026,10),List.of(),BigDecimal.ZERO));
        assertThrows(RuntimeException.class,()->dao.save(new Bill("B3","C1234",YearMonth.of(2026,10),List.of(),BigDecimal.ZERO)));
    }

    @Test void failedItemInsertDoesNotLeavePartialBill() {
        var db=SqliteDatabase.fromPath(dir.resolve("rollback.db")); db.initialiseSchema();
        new SqliteCustomerDao(db).save(new Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        var dao=new SqliteBillDao(db);
        var bill=new Bill("B1","C1234",YearMonth.of(2026,10),
                List.of(new BillItem("Valid item",LocalDate.now(),BigDecimal.ONE),
                        new BillItem("x",LocalDate.now(),BigDecimal.ONE)),BigDecimal.valueOf(2));

        assertThrows(DatabaseException.class,()->dao.save(bill));
        assertTrue(dao.findByReference("B1").isEmpty());
    }

    @Test void rejectsBillsForUnknownCustomersAndReturnsEmptyForUnknownReferences() {
        var db=SqliteDatabase.fromPath(dir.resolve("foreign-key.db")); db.initialiseSchema();
        var dao=new SqliteBillDao(db);

        assertThrows(DatabaseException.class,()->dao.save(
                new Bill("B1","missing",YearMonth.of(2026,10),List.of(),BigDecimal.ZERO)));
        assertTrue(dao.findByReference("missing").isEmpty());
    }
}
