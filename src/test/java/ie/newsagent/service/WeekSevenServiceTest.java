package ie.newsagent.service;

import ie.newsagent.dao.sqlite.*;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

class WeekSevenServiceTest {
    @TempDir Path dir;
    private SqliteDatabase database(String file) {
        var db=SqliteDatabase.fromPath(dir.resolve(file)); db.initialiseSchema();
        var customers=new SqliteCustomerDao(db);
        customers.save(new Customer("C1234","Alex Murphy","0871234567","10 Main Street","D02X285",1));
        customers.save(new Customer("C5678","Jamie Kelly","0877654321","2 Main Road","D01X111",2));
        var publications=new SqlitePublicationDao(db); publications.save(new Publication("P1","Daily News",20));
        new SqliteSubscriptionDao(db).save(new Subscription("S1","C1234","P1",DeliveryFrequency.MONTHLY));
        return db;
    }
    private Delivery delivery(String ref,String customer,DeliveryStatus status,LocalDate date) {
        var d=new Delivery(ref,customer,"P1");d.setStatus(status);d.setDeliveryDate(date);return d;
    }
    @Test void summaryIncludesCustomerPublicationCompletedFailedAndOutOfStockRows() {
        var db=database("summary.db"); var dao=new SqliteDeliveryDao(db);
        dao.save(delivery("D1","C1234",DeliveryStatus.DELIVERED,LocalDate.of(2026,10,2)));
        dao.save(delivery("D2","C1234",DeliveryStatus.FAILED,LocalDate.of(2026,10,3)));
        dao.save(delivery("D3","C1234",DeliveryStatus.OUT_OF_STOCK,LocalDate.of(2026,10,4)));
        var summary=new DeliverySummaryService(new SqliteCustomerDao(db),dao).getSummary();
        assertEquals(2,summary.size());
        var alex=summary.stream().filter(s->s.customerId().equals("C1234")).findFirst().orElseThrow();
        assertEquals("Alex Murphy",alex.customerName()); assertEquals(3,alex.deliveries().size());
        assertEquals("Daily News",alex.deliveries().get(0).getPublicationName());
        assertEquals(DeliveryStatus.DELIVERED,alex.deliveries().get(0).getStatus());
        assertTrue(summary.stream().filter(s->s.customerId().equals("C5678")).findFirst().orElseThrow().deliveries().isEmpty());
    }
    @Test void billsPriorMonthDeliveredHistoryForEachCustomerAndLinksNextDocket() {
        var db=database("billing-flow.db");var deliveryDao=new SqliteDeliveryDao(db);var billDao=new SqliteBillDao(db);
        deliveryDao.save(delivery("OCT-1","C1234",DeliveryStatus.DELIVERED,LocalDate.of(2026,10,8)));
        deliveryDao.save(delivery("OCT-2","C1234",DeliveryStatus.FAILED,LocalDate.of(2026,10,9)));
        deliveryDao.save(delivery("OCT-3","C1234",DeliveryStatus.DELIVERED,LocalDate.of(2026,10,10)));
        var service=new BillingService(billDao,new SqliteCustomerDao(db),deliveryDao,new BigDecimal("1.25"));
        var bills=service.generateOnFirstOfMonth(LocalDate.of(2026,11,1));
        assertEquals(2,bills.size());
        assertTrue(service.generateOnFirstOfMonth(LocalDate.of(2026,11,1)).isEmpty());
        var alex=bills.stream().filter(b->b.getCustomerId().equals("C1234")).findFirst().orElseThrow();
        assertEquals(YearMonth.of(2026,11),alex.getBillingMonth());assertEquals(2,alex.getItems().size());
        assertEquals("Daily News",alex.getItems().get(0).itemName());assertEquals(LocalDate.of(2026,10,8),alex.getItems().get(0).deliveryDate());
        assertEquals("OCT-1",alex.getItems().get(0).deliveryReference());assertEquals("OCT-3",alex.getItems().get(1).deliveryReference());
        assertEquals(new BigDecimal("2.50"),alex.getMonthlyTotal());
        assertEquals(BigDecimal.ZERO,bills.stream().filter(b->b.getCustomerId().equals("C5678")).findFirst().orElseThrow().getMonthlyTotal());
        assertTrue(alex.getItems().get(0).itemName().length()>=3);
        assertTrue(service.generateOnFirstOfMonth(LocalDate.of(2026,11,2)).isEmpty());
        deliveryDao.save(delivery("NOV-1","C1234",DeliveryStatus.DELIVERED,LocalDate.of(2026,11,3)));
        var reopened=new SqliteBillDao(db).findByReference(alex.getBillReference()).orElseThrow();
        assertEquals("NOV-1-D",reopened.getDocketReference());
    }
    @Test void unpaidBillsAreRemindedNextMonthAndPaidBillsAreExcluded() {
        var db=database("reminders.db");var dao=new SqliteBillDao(db);var billing=new BillingService(dao);
        var month=YearMonth.of(2026,10);
        var unpaid=billing.generate("C1234",month,java.util.List.of());
        var paid=billing.generate("C5678",month,java.util.List.of());
        var payments=new PaymentService(dao);
        assertEquals(2,payments.remindersFor(month.plusMonths(1)).size());
        assertThrows(IllegalArgumentException.class,()->payments.markPaid("missing"));
        assertThrows(IllegalArgumentException.class,()->payments.markPaid(" "));
        assertEquals(BillPaymentStatus.PAID,payments.markPaid(paid.getBillReference()).getPaymentStatus());
        assertEquals(BillPaymentStatus.PAID,payments.markPaid(paid.getBillReference()).getPaymentStatus());
        assertEquals(java.util.List.of(unpaid.getBillReference()),payments.remindersFor(month.plusMonths(1)).stream().map(Bill::getBillReference).toList());
        assertEquals(BillPaymentStatus.PAID,new SqliteBillDao(db).findByReference(paid.getBillReference()).orElseThrow().getPaymentStatus());
    }
    @Test void monthlyBillingRejectsDuplicateMonthAndInvalidDescription() {
        var db=database("validation.db");var service=new BillingService(new SqliteBillDao(db));
        var items=java.util.List.of(new BillItem("Paper",LocalDate.of(2026,10,1),BigDecimal.ONE));
        service.generate("C1234",YearMonth.of(2026,11),items);
        assertThrows(IllegalStateException.class,()->service.generate("C1234",YearMonth.of(2026,11),items));
        assertThrows(IllegalArgumentException.class,()->service.generate("C5678",YearMonth.of(2026,11),java.util.List.of(new BillItem("ab",LocalDate.now(),BigDecimal.ONE))));
    }

    @Test void validatesDescriptionAndMonthlyTotalBoundaries() {
        var db=database("bill-boundaries.db");
        var service=new BillingService(new SqliteBillDao(db));
        var date=LocalDate.of(2026,10,1);
        var maximumTotal=service.generate("C1234",YearMonth.of(2026,11),
                java.util.List.of(new BillItem("ABC",date,new BigDecimal("99999.99"))));
        assertEquals(new BigDecimal("99999.99"),maximumTotal.getMonthlyTotal());

        var maximumDescription=service.generate("C5678",YearMonth.of(2026,11),
                java.util.List.of(new BillItem("D".repeat(50),date,BigDecimal.ZERO)));
        assertEquals(50,maximumDescription.getItems().get(0).itemName().length());
        assertEquals(BigDecimal.ZERO,maximumDescription.getMonthlyTotal());

        assertThrows(IllegalArgumentException.class,()->service.generate("C1234",YearMonth.of(2026,12),
                java.util.List.of(new BillItem("AB",date,BigDecimal.ONE))));
        assertThrows(IllegalArgumentException.class,()->service.generate("C5678",YearMonth.of(2026,12),
                java.util.List.of(new BillItem("D".repeat(51),date,BigDecimal.ONE))));
        assertThrows(IllegalArgumentException.class,()->service.generate("C1234",YearMonth.of(2027,1),
                java.util.List.of(new BillItem("Daily News",date,new BigDecimal("100000.00")))));
        assertThrows(IllegalArgumentException.class,()->service.generate("C5678",YearMonth.of(2027,1),
                java.util.List.of(new BillItem("Daily News",date,new BigDecimal("-0.01")))));
    }

    @Test void summaryIsEmptyWhenThereAreNoCustomers() {
        var db=SqliteDatabase.fromPath(dir.resolve("empty-summary.db"));
        db.initialiseSchema();
        assertTrue(new DeliverySummaryService(new SqliteCustomerDao(db),new SqliteDeliveryDao(db))
                .getSummary().isEmpty());
    }
}
