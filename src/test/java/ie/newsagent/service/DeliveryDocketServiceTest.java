package ie.newsagent.service;

import ie.newsagent.dao.sqlite.SqliteBillDao;
import ie.newsagent.dao.sqlite.SqliteCustomerDao;
import ie.newsagent.dao.sqlite.SqliteDeliveryDao;
import ie.newsagent.dao.sqlite.SqlitePublicationDao;
import ie.newsagent.dao.sqlite.SqliteSubscriptionDao;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Bill;
import ie.newsagent.model.Customer;
import ie.newsagent.model.DeliveryDocket;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.model.DeliveryStatus;
import ie.newsagent.model.Publication;
import ie.newsagent.model.Subscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryDocketServiceTest {
    @TempDir Path directory;

    private SqliteDatabase database() {
        SqliteDatabase database = SqliteDatabase.fromPath(directory.resolve("dockets.db"));
        database.initialiseSchema();
        var customers = new SqliteCustomerDao(database);
        customers.save(new Customer("C1234", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));
        customers.save(new Customer("C5678", "Jamie Kelly", "0877654321",
                "2 Main Road", "D01X111", 2));
        var publications = new SqlitePublicationDao(database);
        publications.save(new Publication("P1", "Daily News", 4));
        publications.save(new Publication("P2", "Morning Paper", 20));
        var subscriptions = new SqliteSubscriptionDao(database);
        subscriptions.save(new Subscription("S1", "C1234", "P1", DeliveryFrequency.MONTHLY));
        subscriptions.save(new Subscription("S2", "C1234", "P2", DeliveryFrequency.MONTHLY));
        Subscription paused = new Subscription("S3", "C5678", "P1", DeliveryFrequency.MONTHLY);
        paused.setPaused(true);
        subscriptions.save(paused);
        return database;
    }

    @Test
    void createsDocketsForAllAreasAndOnlyIncludesScheduledActiveCustomers() {
        var database = database();
        var service = service(database);
        LocalDate date = LocalDate.of(2026, 11, 3);

        List<DeliveryDocket> dockets = service.generateAll(date);

        assertEquals(24, dockets.size());
        assertEquals(24, dockets.stream().map(DeliveryDocket::deliveryArea).distinct().count());
        DeliveryDocket areaOne = dockets.get(0);
        assertEquals("C1234", areaOne.items().get(0).customerId());
        assertEquals("Alex Murphy", areaOne.items().get(0).customerName());
        assertEquals("10 Main Street", areaOne.items().get(0).customerAddress());
        assertEquals("Daily News", areaOne.items().get(0).publicationName());
        assertEquals(4, areaOne.items().get(0).stockQuantity());
        assertTrue(dockets.get(1).items().isEmpty());
        assertTrue(dockets.get(23).items().isEmpty());

        assertEquals(areaOne.docketReference(), service.generate(date, 1).docketReference());
        assertEquals(2, service.generate(date, 1).items().size());
        assertTrue(service.generate(date.plusDays(2), 1).items().isEmpty(),
                "A monthly subscription cannot be scheduled twice in the same month");
        assertThrows(IllegalArgumentException.class, () -> service.generate(date, 0));
        assertThrows(IllegalArgumentException.class, () -> service.generate(date, 25));
    }

    @Test
    void includesAnOutstandingMonthlyBillWithTheCustomersNextDocket() {
        var database = database();
        var billDao = new SqliteBillDao(database);
        Bill bill = new BillingService(billDao).generate("C1234", YearMonth.of(2026, 10), List.of(
                new ie.newsagent.model.BillItem("Daily News", LocalDate.of(2026, 10, 4), BigDecimal.ONE)));

        DeliveryDocket docket = service(database).generate(LocalDate.of(2026, 11, 3), 1);

        assertEquals(List.of(bill.getBillReference()),
                docket.bills().stream().map(Bill::getBillReference).toList());
        assertEquals(docket.docketReference(), billDao.findByReference(bill.getBillReference())
                .orElseThrow().getDocketReference());
        assertTrue(service(database).printableOutput(docket).contains(bill.getBillReference()));
    }

    @Test
    void includesBillsCreatedAfterTheDocketWasFirstGenerated() {
        var database = database();
        var service = service(database);
        DeliveryDocket docket = service.generate(LocalDate.of(2026, 11, 3), 1);
        var bill = new BillingService(new SqliteBillDao(database)).generate(
                "C1234", YearMonth.of(2026, 10), List.of());

        service.saveOutcome(docket.docketReference(), docket.items().get(0).deliveryReference(),
                DeliveryStatus.DELIVERED);

        assertTrue(service.get(docket.docketReference()).bills().stream()
                .anyMatch(assigned -> assigned.getBillReference().equals(bill.getBillReference())));
    }

    @Test
    void driverResultsPersistAndAppearInNewsagentSummary() throws Exception {
        var database = database();
        var service = service(database);
        DeliveryDocket docket = service.generate(LocalDate.of(2026, 11, 3), 1);
        String deliveredReference = docket.items().get(0).deliveryReference();
        String failedReference = docket.items().get(1).deliveryReference();

        assertEquals(DeliveryStatus.DELIVERED,
                service.saveOutcome(docket.docketReference(), deliveredReference, DeliveryStatus.DELIVERED)
                        .getStatus());
        assertEquals(DeliveryStatus.FAILED,
                service.saveOutcome(docket.docketReference(), failedReference, DeliveryStatus.FAILED)
                        .getStatus());
        assertEquals(DeliveryStatus.OUT_OF_STOCK,
                service.saveOutcome(docket.docketReference(), deliveredReference, DeliveryStatus.OUT_OF_STOCK)
                        .getStatus());

        var reopened = new SqliteDeliveryDao(database);
        var summary = new DeliverySummaryService(new SqliteCustomerDao(database), reopened).getSummary();
        var alex = summary.stream().filter(row -> row.customerId().equals("C1234")).findFirst().orElseThrow();
        assertEquals(java.util.Set.of(DeliveryStatus.OUT_OF_STOCK, DeliveryStatus.FAILED),
                alex.deliveries().stream().map(delivery -> delivery.getStatus())
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(reopened.findDeliveredForCustomer("C1234", YearMonth.of(2026, 11)).isEmpty());
        try (var connection = database.openConnection();
             var query = connection.prepareStatement(
                     "SELECT COUNT(*) FROM out_of_stock_record WHERE delivery_reference=?")) {
            query.setString(1, deliveredReference);
            try (var result = query.executeQuery()) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            }
        }
        assertEquals(DeliveryStatus.OUT_OF_STOCK,
                service.get(docket.docketReference()).items().stream()
                        .filter(item -> item.deliveryReference().equals(deliveredReference))
                        .findFirst().orElseThrow().status());
    }

    @Test
    void rejectsMissingStatusUnknownReferenceAndReferencesFromAnotherDocket() {
        var database = database();
        var service = service(database);
        DeliveryDocket first = service.generate(LocalDate.of(2026, 11, 3), 1);
        DeliveryDocket otherArea = service.generate(LocalDate.of(2026, 11, 3), 2);

        assertThrows(IllegalArgumentException.class,
                () -> service.saveOutcome(first.docketReference(), first.items().get(0).deliveryReference(), null));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveOutcome(first.docketReference(), "not-on-docket", DeliveryStatus.DELIVERED));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveOutcome(otherArea.docketReference(), first.items().get(0).deliveryReference(),
                        DeliveryStatus.DELIVERED));
        assertThrows(IllegalArgumentException.class, () -> service.get("missing-docket"));
    }

    private DeliveryDocketService service(SqliteDatabase database) {
        return new DeliveryDocketService(new SqliteDeliveryDao(database), new SqliteBillDao(database));
    }
}
