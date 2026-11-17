package ie.newsagent.ui;

import java.util.Scanner;
import ie.newsagent.database.DatabaseInitializer;

/** Minimal console entry point; keep UI concerns out of service classes. */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        DatabaseInitializer.initialiseApplicationDatabase();
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Newsagent Management System");
            System.out.println("1. Customer management");
            System.out.println("2. Driver management");
            System.out.println("3. Publications and stock");
            System.out.println("4. Subscriptions");
            System.out.println("5. Billing and delivery summaries");
            System.out.println("6. Delivery driver");
            System.out.println("0. Exit");
            System.out.print("Choose an area: ");
            String choice = scanner.nextLine().trim();
            System.out.println("Menu selection " + choice + " is a placeholder for the next increment.");
        }
    }
}
