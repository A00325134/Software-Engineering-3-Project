package ie.newsagent.demo;

import ie.newsagent.dao.sqlite.SqliteBillDao;
import ie.newsagent.dao.sqlite.SqliteCustomerDao;
import ie.newsagent.dao.sqlite.SqliteDeliveryDao;
import ie.newsagent.dao.sqlite.SqliteDriverDao;
import ie.newsagent.dao.sqlite.SqlitePublicationDao;
import ie.newsagent.dao.sqlite.SqliteSubscriptionDao;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.Bill;
import ie.newsagent.model.BillPaymentStatus;
import ie.newsagent.model.Customer;
import ie.newsagent.model.DeliveryDocket;
import ie.newsagent.model.DeliveryDriver;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.model.DeliveryStatus;
import ie.newsagent.model.Publication;
import ie.newsagent.model.Subscription;
import ie.newsagent.service.BillingService;
import ie.newsagent.service.DeliveryDocketService;
import ie.newsagent.service.PaymentService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Explicitly seeds a separate demo database. This is never called at application startup.
 */
public final class DemoDataSeeder {
    public static final LocalDate DELIVERY_DATE = LocalDate.of(2026, 11, 17);
    public static final YearMonth BILLING_MONTH = YearMonth.of(2026, 12);

    private DemoDataSeeder() {}

    public static void main(String[] args) {
        Path path = Path.of(System.getProperty("newsagent.demo.db", "data/newsagent-demo.db"));
        SqliteDatabase database = SqliteDatabase.fromPath(path);
        seed(database);
        System.out.println("Demo database ready: " + path.toAbsolutePath());
    }

    public static void seed(SqliteDatabase database) {
        database.initialiseSchema();
        var customers = new SqliteCustomerDao(database);
        saveCustomerIfMissing(customers, new Customer("DEMO-C1", "Alex Murphy", "0871234567",
                "10 Main Street", "D02X285", 1));
        saveCustomerIfMissing(customers, new Customer("DEMO-C2", "Jamie Kelly", "0877654321",
                "2 Main Road", "D01X111", 2));
        saveCustomerIfMissing(customers, new Customer("DEMO-C3", "Casey Byrne", "0861234567",
                "4 North Road", "D03X222", 3));

        var drivers = new SqliteDriverDao(database);
        if (drivers.findById("DEMO-D1").isEmpty()) {
            drivers.save(new DeliveryDriver("DEMO-D1", "Sam Driver", "demo-password-hash"));
        }

        var publications = new SqlitePublicationDao(database);
        savePublicationIfMissing(publications, new Publication("DEMO-P1", "Demo Daily News", 12));
        savePublicationIfMissing(publications, new Publication("DEMO-P2", "Demo Morning Paper", 3));
        savePublicationIfMissing(publications, new Publication("DEMO-P3", "Demo Evening News", 40));

        var subscriptions = new SqliteSubscriptionDao(database);
        saveSubscriptionIfMissing(subscriptions,
                new Subscription("DEMO-S1", "DEMO-C1", "DEMO-P1", DeliveryFrequency.MONTHLY));
        saveSubscriptionIfMissing(subscriptions,
                new Subscription("DEMO-S2", "DEMO-C1", "DEMO-P2", DeliveryFrequency.MONTHLY));
        saveSubscriptionIfMissing(subscriptions,
                new Subscription("DEMO-S3", "DEMO-C2", "DEMO-P3", DeliveryFrequency.MONTHLY));
        Subscription paused = new Subscription("DEMO-S4", "DEMO-C3", "DEMO-P1", DeliveryFrequency.MONTHLY);
        paused.setPaused(true);
        saveSubscriptionIfMissing(subscriptions, paused);

        var billDao = new SqliteBillDao(database);
        var deliveryDao = new SqliteDeliveryDao(database);
        var dockets = new DeliveryDocketService(deliveryDao, billDao);
        List<DeliveryDocket> novemberDockets = dockets.generateAll(DELIVERY_DATE);
        recordOutcome(novemberDockets, "DEMO-C1", "DEMO-P1", DeliveryStatus.DELIVERED, dockets);
        recordOutcome(novemberDockets, "DEMO-C1", "DEMO-P2", DeliveryStatus.OUT_OF_STOCK, dockets);
        recordOutcome(novemberDockets, "DEMO-C2", "DEMO-P3", DeliveryStatus.FAILED, dockets);

        BillingService billing = new BillingService(billDao, customers, deliveryDao, BigDecimal.ONE);
        billing.generateMonthlyBills(BILLING_MONTH, customers, deliveryDao, BigDecimal.ONE);
        Bill customerOneBill = billing.forCustomer("DEMO-C1").stream()
                .filter(bill -> bill.getBillingMonth().equals(BILLING_MONTH))
                .findFirst().orElseThrow(() -> new IllegalStateException("Demo monthly bill was not generated"));
        if (customerOneBill.getPaymentStatus() != BillPaymentStatus.PAID) {
            new PaymentService(billDao).markPaid(customerOneBill.getBillReference());
        }
        dockets.generateAll(BILLING_MONTH.atDay(3));
    }

    private static void recordOutcome(List<DeliveryDocket> dockets, String customerId,
                                      String publicationId, DeliveryStatus status,
                                      DeliveryDocketService service) {
        for (DeliveryDocket docket : dockets) {
            for (var item : docket.items()) {
                if (item.customerId().equals(customerId) && item.publicationId().equals(publicationId)) {
                    service.saveOutcome(docket.docketReference(), item.deliveryReference(), status);
                    return;
                }
            }
        }
        throw new IllegalStateException("Demo delivery is missing from its generated docket: "
                + customerId + "/" + publicationId);
    }

    private static void saveCustomerIfMissing(SqliteCustomerDao dao, Customer customer) {
        if (dao.findById(customer.getCustomerId()).isEmpty()) dao.save(customer);
    }

    private static void savePublicationIfMissing(SqlitePublicationDao dao, Publication publication) {
        if (dao.findById(publication.getPublicationId()).isEmpty()) dao.save(publication);
    }

    private static void saveSubscriptionIfMissing(SqliteSubscriptionDao dao, Subscription subscription) {
        if (dao.findById(subscription.getSubscriptionId()).isEmpty()) dao.save(subscription);
    }
}
