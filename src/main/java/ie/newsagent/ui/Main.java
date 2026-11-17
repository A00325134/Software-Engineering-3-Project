package ie.newsagent.ui;

import ie.newsagent.dao.sqlite.*;
import ie.newsagent.database.DatabaseException;
import ie.newsagent.database.DatabaseInitializer;
import ie.newsagent.database.SqliteDatabase;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.service.*;
import java.util.Scanner;

/** Console UI; all business operations flow through services and DAOs. */
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        DatabaseInitializer.initialiseApplicationDatabase();
        var db = SqliteDatabase.applicationDatabase();
        var customerDao=new SqliteCustomerDao(db); var deliveryDao=new SqliteDeliveryDao(db);
        var billing=new BillingService(new SqliteBillDao(db),customerDao,deliveryDao,new java.math.BigDecimal("1.00"));
        billing.generateOnFirstOfMonth(java.time.LocalDate.now());
        run(new Scanner(System.in), new CustomerService(new SqliteCustomerDao(db)),
                new DriverService(new SqliteDriverDao(db)), new PublicationService(new SqlitePublicationDao(db)),
                new SubscriptionService(new SqliteSubscriptionDao(db)), billing,
                new PaymentService(new SqliteBillDao(db)), new DeliverySummaryService(customerDao,deliveryDao));
    }
    static void run(Scanner in, CustomerService customers, DriverService drivers,
                    PublicationService publications, SubscriptionService subscriptions) {
        run(in, customers, drivers, publications, subscriptions, null, null);
    }
    static void run(Scanner in, CustomerService customers, DriverService drivers,
                    PublicationService publications, SubscriptionService subscriptions,
                    BillingService billing, PaymentService payments) {
        run(in,customers,drivers,publications,subscriptions,billing,payments,null);
    }
    static void run(Scanner in, CustomerService customers, DriverService drivers,
                    PublicationService publications, SubscriptionService subscriptions,
                    BillingService billing, PaymentService payments, DeliverySummaryService summary) {
        while (true) {
            System.out.println("\nNewsagent Management System\n1 Customers\n2 Delivery drivers\n3 Publications and stock\n4 Subscriptions\n5 Bills and payments\n6 Delivery summary\n0 Exit");
            if (!in.hasNextLine()) return;
            String choice = ask(in, "Choose an area: ");
            if (choice.equals("0")) return;
            try {
                switch (choice) {
                    case "1" -> customerMenu(in, customers);
                    case "2" -> driverMenu(in, drivers);
                    case "3" -> publicationMenu(in, publications);
                    case "4" -> subscriptionMenu(in, subscriptions);
                    case "5" -> { if (billing == null || payments == null) System.out.println("Billing is unavailable."); else billingMenu(in, billing, payments); }
                    case "6" -> { if (summary == null) System.out.println("Delivery summary is unavailable."); else summaryMenu(summary); }
                    default -> System.out.println("Choose 0–6.");
                }
            } catch (IllegalArgumentException | IllegalStateException | DatabaseException e) { System.out.println("Error: " + e.getMessage()); }
        }
    }
    private static void summaryMenu(DeliverySummaryService summary) {
        var rows=summary.getSummary();
        if(rows.isEmpty()) { System.out.println("No customers or deliveries found."); return; }
        for(var customer:rows) {
            System.out.println(customer.customerName()+" ("+customer.customerId()+")");
            if(customer.deliveries().isEmpty()) { System.out.println("  No deliveries"); continue; }
            for(var d:customer.deliveries()) System.out.println("  "+d.getDeliveryDate()+" | "+d.getPublicationName()+" | "+d.getStatus());
        }
    }
    private static void billingMenu(Scanner in, BillingService billing, PaymentService payments) {
        switch (ask(in, "1 View customer bills  2 Mark bill paid  3 Unpaid reminders: ")) {
            case "1" -> billing.forCustomer(ask(in, "Customer ID: ")).forEach(b -> {
                System.out.println(b.getBillReference()+" | "+b.getBillingMonth()+" | total "+b.getMonthlyTotal()+" | "+b.getPaymentStatus()+" | docket "+(b.getDocketReference()==null?"awaiting next delivery":b.getDocketReference()));
                b.getItems().forEach(i -> System.out.println("  "+i.deliveryDate()+" | "+i.itemName()+" | "+i.amount()));
            });
            case "2" -> { var b=payments.markPaid(ask(in,"Bill reference: ")); System.out.println("Payment status: "+b.getPaymentStatus()); }
            case "3" -> payments.remindersFor(java.time.YearMonth.now()).forEach(b -> System.out.println("Unpaid bill reminder: "+b.getBillReference()+" for customer "+b.getCustomerId()));
            default -> System.out.println("Unknown billing action.");
        }
    }
    private static void customerMenu(Scanner in, CustomerService s) {
        switch (ask(in, "1 List  2 Create  3 View  4 Edit  5 Delete: ")) {
            case "1" -> s.getAll().forEach(c -> System.out.println(c.getCustomerId()+" | "+c.getName()+" | "+c.getPhoneNumber()+" | "+c.getAddress()+" | "+c.getEircode()+" | area "+c.getDeliveryArea()));
            case "2" -> { var c=s.create(ask(in,"Name: "),ask(in,"Phone: "),ask(in,"Address: "),ask(in,"Eircode: "),integer(in,"Delivery area: ")); System.out.println("Created customer "+c.getCustomerId()); }
            case "3" -> { var c=s.get(ask(in,"Customer ID: ")); System.out.println(c.getCustomerId()+" | "+c.getName()+" | "+c.getPhoneNumber()+" | "+c.getAddress()+" | "+c.getEircode()+" | area "+c.getDeliveryArea()); }
            case "4" -> { String id=ask(in,"Customer ID: "); var c=s.update(id,ask(in,"Name: "),ask(in,"Phone: "),ask(in,"Address: "),ask(in,"Eircode: "),integer(in,"Delivery area: ")); System.out.println("Updated "+c.getCustomerId()); }
            case "5" -> { s.delete(ask(in,"Customer ID: ")); System.out.println("Customer deleted."); }
            default -> System.out.println("Unknown customer action.");
        }
    }
    private static void driverMenu(Scanner in, DriverService s) {
        switch (ask(in,"1 List  2 Create  3 View  4 Edit  5 Delete: ")) {
            case "1" -> s.getAll().forEach(d -> System.out.println(d.getDriverId()+" | "+d.getName()));
            case "2" -> { var d=s.create(ask(in,"Driver ID: "),ask(in,"Name: "),ask(in,"Password hash: ")); System.out.println("Created "+d.getDriverId()); }
            case "3" -> { var d=s.get(ask(in,"Driver ID: ")); System.out.println(d.getDriverId()+" | "+d.getName()); }
            case "4" -> { var d=s.update(ask(in,"Driver ID: "),ask(in,"Name: "),ask(in,"Password hash: ")); System.out.println("Updated "+d.getDriverId()); }
            case "5" -> { s.delete(ask(in,"Driver ID: ")); System.out.println("Driver deleted."); }
            default -> System.out.println("Unknown driver action.");
        }
    }
    private static void publicationMenu(Scanner in, PublicationService s) {
        switch (ask(in,"1 List  2 Create  3 View  4 Update stock  5 Low stock: ")) {
            case "1" -> s.getAll().forEach(p -> System.out.println(p.getPublicationId()+" | "+p.getName()+" | stock "+p.getStockQuantity()));
            case "2" -> { var p=s.create(ask(in,"Publication ID: "),ask(in,"Name: "),integer(in,"Starting stock (10-1000): ")); System.out.println("Created "+p.getPublicationId()); }
            case "3" -> { var p=s.get(ask(in,"Publication ID: ")); System.out.println(p.getPublicationId()+" | "+p.getName()+" | stock "+p.getStockQuantity()); }
            case "4" -> { var p=s.updateStock(ask(in,"Publication ID: "),integer(in,"New stock (0-1000): ")); System.out.println("Stock updated to "+p.getStockQuantity()); }
            case "5" -> s.lowStock().forEach(p -> System.out.println(p.getPublicationId()+" | "+p.getName()+" | stock "+p.getStockQuantity()));
            default -> System.out.println("Unknown publication action.");
        }
    }
    private static void subscriptionMenu(Scanner in, SubscriptionService s) {
        switch (ask(in,"1 List  2 Create  3 View  4 Pause  5 Resume  6 Delete: ")) {
            case "1" -> s.getAll().forEach(x -> System.out.println(x.getSubscriptionId()+" | customer "+x.getCustomerId()+" | publication "+x.getPublicationId()+" | "+(x.isPaused()?"paused":"active")));
            case "2" -> { var x=s.create(ask(in,"Subscription ID: "),ask(in,"Customer ID: "),ask(in,"Publication ID: "),DeliveryFrequency.MONTHLY); System.out.println("Created monthly subscription "+x.getSubscriptionId()); }
            case "3" -> { var x=s.get(ask(in,"Subscription ID: ")); System.out.println(x.getSubscriptionId()+" | customer "+x.getCustomerId()+" | publication "+x.getPublicationId()+" | "+(x.isPaused()?"paused":"active")); }
            case "4" -> { var x=s.pause(ask(in,"Subscription ID: ")); System.out.println("Paused "+x.getSubscriptionId()); }
            case "5" -> { var x=s.resume(ask(in,"Subscription ID: ")); System.out.println("Resumed "+x.getSubscriptionId()); }
            case "6" -> { s.delete(ask(in,"Subscription ID: ")); System.out.println("Subscription deleted."); }
            default -> System.out.println("Unknown subscription action.");
        }
    }
    private static String ask(Scanner in, String prompt) { System.out.print(prompt); return in.nextLine().trim(); }
    private static int integer(Scanner in, String prompt) { try { return Integer.parseInt(ask(in,prompt)); } catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a whole number."); } }
}
