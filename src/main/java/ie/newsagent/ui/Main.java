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
        var billDao=new SqliteBillDao(db);
        run(new Scanner(System.in), new CustomerService(new SqliteCustomerDao(db)),
                new DriverService(new SqliteDriverDao(db)), new PublicationService(new SqlitePublicationDao(db)),
                new SubscriptionService(new SqliteSubscriptionDao(db)), billing,
                new PaymentService(billDao), new DeliverySummaryService(customerDao,deliveryDao),
                new DeliveryDocketService(deliveryDao,billDao));
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
        run(in, customers, drivers, publications, subscriptions, billing, payments, summary, null);
    }
    static void run(Scanner in, CustomerService customers, DriverService drivers,
                    PublicationService publications, SubscriptionService subscriptions,
                    BillingService billing, PaymentService payments, DeliverySummaryService summary,
                    DeliveryDocketService dockets) {
        while (true) {
            System.out.println("\nNewsagent Management System\n1 Customers\n2 Delivery drivers\n3 Publications and stock\n4 Subscriptions\n5 Bills and payments\n6 Delivery summary\n7 Driver docket\n0 Exit");
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
                    case "7" -> { if (dockets == null) System.out.println("Driver dockets are unavailable."); else docketMenu(in, dockets); }
                    default -> System.out.println("Choose 0–7.");
                }
            } catch (IllegalArgumentException | IllegalStateException | DatabaseException e) { System.out.println("Error: " + e.getMessage()); }
        }
    }
    private static void docketMenu(Scanner in, DeliveryDocketService dockets) {
        String action = ask(in, "1 View daily docket  2 Print daily docket  3 Save delivery result: ");
        if (!action.equals("1") && !action.equals("2") && !action.equals("3")) {
            System.out.println("Unknown driver docket action.");
            return;
        }
        java.time.LocalDate date;
        try {
            date = java.time.LocalDate.parse(ask(in, "Delivery date (YYYY-MM-DD): "));
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a valid delivery date (YYYY-MM-DD).");
        }
        int area = integer(in, "Delivery area (1-24): ");
        var docket = dockets.generate(date, area);
        if (action.equals("1")) {
            System.out.print(dockets.printableOutput(docket));
        } else if (action.equals("2")) {
            System.out.println("Printable docket:");
            System.out.print(dockets.printableOutput(docket));
        } else {
            String reference = ask(in, "Delivery reference: ");
            String status = ask(in, "Status (1 Delivered, 2 Failed, 3 Out of Stock): ");
            var result = switch (status) {
                case "1" -> dockets.saveOutcome(docket.docketReference(), reference,
                        ie.newsagent.model.DeliveryStatus.DELIVERED);
                case "2" -> dockets.saveOutcome(docket.docketReference(), reference,
                        ie.newsagent.model.DeliveryStatus.FAILED);
                case "3" -> dockets.saveOutcome(docket.docketReference(), reference,
                        ie.newsagent.model.DeliveryStatus.OUT_OF_STOCK);
                default -> throw new IllegalArgumentException("Choose Delivered, Failed, or Out of Stock.");
            };
            System.out.println("Saved delivery status: " + result.getStatus());
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
            case "5" -> {
                String id = ask(in, "Customer ID: ");
                if (confirm(in, "Delete customer " + id + "? (y/n): ")) {
                    s.delete(id);
                    System.out.println("Customer deleted.");
                } else System.out.println("Deletion cancelled.");
            }
            default -> System.out.println("Unknown customer action.");
        }
    }
    private static void driverMenu(Scanner in, DriverService s) {
        switch (ask(in,"1 List  2 Create  3 View  4 Edit  5 Delete: ")) {
            case "1" -> s.getAll().forEach(d -> System.out.println(d.getDriverId()+" | "+d.getName()));
            case "2" -> { var d=s.create(ask(in,"Driver ID: "),ask(in,"Name: "),ask(in,"Password hash: ")); System.out.println("Created "+d.getDriverId()); }
            case "3" -> { var d=s.get(ask(in,"Driver ID: ")); System.out.println(d.getDriverId()+" | "+d.getName()); }
            case "4" -> { var d=s.update(ask(in,"Driver ID: "),ask(in,"Name: "),ask(in,"Password hash: ")); System.out.println("Updated "+d.getDriverId()); }
            case "5" -> {
                String id = ask(in, "Driver ID: ");
                if (confirm(in, "Delete driver " + id + "? (y/n): ")) {
                    s.delete(id);
                    System.out.println("Driver deleted.");
                } else System.out.println("Deletion cancelled.");
            }
            default -> System.out.println("Unknown driver action.");
        }
    }
    private static void publicationMenu(Scanner in, PublicationService s) {
        switch (ask(in,"1 List  2 Create  3 View  4 Update details  5 Update stock  6 Low stock  7 Delete: ")) {
            case "1" -> s.getAll().forEach(p -> System.out.println(p.getPublicationId()+" | "+p.getName()+" | stock "+p.getStockQuantity()));
            case "2" -> { var p=s.create(ask(in,"Publication ID: "),ask(in,"Name: "),integer(in,"Starting stock (10-1000): ")); System.out.println("Created "+p.getPublicationId()); }
            case "3" -> { var p=s.get(ask(in,"Publication ID: ")); System.out.println(p.getPublicationId()+" | "+p.getName()+" | stock "+p.getStockQuantity()); }
            case "4" -> { var p=s.updateName(ask(in,"Publication ID: "),ask(in,"New name: ")); System.out.println("Updated "+p.getPublicationId()); }
            case "5" -> { var p=s.updateStock(ask(in,"Publication ID: "),integer(in,"New stock (0-1000): ")); System.out.println("Stock updated to "+p.getStockQuantity()); }
            case "6" -> s.lowStock().forEach(p -> System.out.println(p.getPublicationId()+" | "+p.getName()+" | stock "+p.getStockQuantity()));
            case "7" -> {
                String id=ask(in,"Publication ID: ");
                if (confirm(in,"Delete publication "+id+"? (y/n): ")) { s.delete(id); System.out.println("Publication deleted."); }
                else System.out.println("Deletion cancelled.");
            }
            default -> System.out.println("Unknown publication action.");
        }
    }
    private static void subscriptionMenu(Scanner in, SubscriptionService s) {
        switch (ask(in,"1 List  2 Create  3 View  4 Pause  5 Resume  6 Update  7 Delete: ")) {
            case "1" -> s.getAll().forEach(x -> System.out.println(x.getSubscriptionId()+" | customer "+x.getCustomerId()+" | publication "+x.getPublicationId()+" | "+(x.isPaused()?"paused":"active")));
            case "2" -> { var x=s.create(ask(in,"Subscription ID: "),ask(in,"Customer ID: "),ask(in,"Publication ID: "),frequency(in)); System.out.println("Created subscription "+x.getSubscriptionId()); }
            case "3" -> { var x=s.get(ask(in,"Subscription ID: ")); System.out.println(x.getSubscriptionId()+" | customer "+x.getCustomerId()+" | publication "+x.getPublicationId()+" | "+(x.isPaused()?"paused":"active")); }
            case "4" -> { var x=s.pause(ask(in,"Subscription ID: ")); System.out.println("Paused "+x.getSubscriptionId()); }
            case "5" -> { var x=s.resume(ask(in,"Subscription ID: ")); System.out.println("Resumed "+x.getSubscriptionId()); }
            case "6" -> {
                var x=s.update(ask(in,"Subscription ID: "),ask(in,"Publication ID: "),frequency(in));
                System.out.println("Updated "+x.getSubscriptionId());
            }
            case "7" -> {
                String id=ask(in,"Subscription ID: ");
                if (confirm(in,"Delete subscription "+id+"? (y/n): ")) { s.delete(id); System.out.println("Subscription deleted."); }
                else System.out.println("Deletion cancelled.");
            }
            default -> System.out.println("Unknown subscription action.");
        }
    }
    private static DeliveryFrequency frequency(Scanner in) {
        String value = ask(in, "Delivery frequency (MONTHLY): ");
        try { return value.isBlank() ? DeliveryFrequency.MONTHLY : DeliveryFrequency.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Unsupported delivery frequency: " + value); }
    }
    private static boolean confirm(Scanner in, String prompt) {
        String answer = ask(in, prompt);
        if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) return true;
        if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) return false;
        throw new IllegalArgumentException("Enter y or n to confirm.");
    }
    private static String ask(Scanner in, String prompt) { System.out.print(prompt); return in.nextLine().trim(); }
    private static int integer(Scanner in, String prompt) { try { return Integer.parseInt(ask(in,prompt)); } catch (NumberFormatException e) { throw new IllegalArgumentException("Enter a whole number."); } }
}
