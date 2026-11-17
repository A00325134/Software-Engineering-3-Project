package ie.newsagent.service;

import ie.newsagent.dao.BillDao;
import ie.newsagent.dao.DeliveryDao;
import ie.newsagent.model.Delivery;
import ie.newsagent.model.DeliveryDocket;
import ie.newsagent.model.DeliveryStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Coordinates docket generation, delivery outcomes, and printable driver output. */
public final class DeliveryDocketService {
    private final DeliveryDao deliveries;
    private final BillDao bills;

    public DeliveryDocketService(DeliveryDao deliveries, BillDao bills) {
        this.deliveries = Objects.requireNonNull(deliveries);
        this.bills = Objects.requireNonNull(bills);
    }

    public DeliveryDocket generate(LocalDate date, int deliveryArea) {
        Objects.requireNonNull(date, "Delivery date is required");
        validateArea(deliveryArea);
        DeliveryDocket docket = deliveries.generateDocket(date, deliveryArea);
        return withBills(docket);
    }

    public List<DeliveryDocket> generateAll(LocalDate date) {
        Objects.requireNonNull(date, "Delivery date is required");
        List<DeliveryDocket> dockets = new ArrayList<>();
        for (int area = 1; area <= 24; area++) dockets.add(generate(date, area));
        return List.copyOf(dockets);
    }

    public DeliveryDocket get(String docketReference) {
        return withBills(deliveries.findDocket(docketReference));
    }

    public Delivery saveOutcome(String docketReference, String deliveryReference, DeliveryStatus status) {
        if (status == null) throw new IllegalArgumentException("Delivery status is required");
        return deliveries.saveOutcome(docketReference, deliveryReference, status);
    }

    public String printableOutput(DeliveryDocket docket) {
        Objects.requireNonNull(docket, "Docket is required");
        StringBuilder output = new StringBuilder()
                .append("Daily delivery docket ")
                .append(docket.deliveryDate())
                .append(" | Area ")
                .append(docket.deliveryArea())
                .append(" | ")
                .append(docket.docketReference())
                .append(System.lineSeparator());
        if (docket.items().isEmpty()) output.append("No scheduled deliveries").append(System.lineSeparator());
        for (var item : docket.items()) {
            output.append(item.customerName()).append(" | ").append(item.customerAddress())
                    .append(System.lineSeparator())
                    .append("  ").append(item.publicationName()).append(" [")
                    .append(item.deliveryReference()).append("] | stock ")
                    .append(item.stockQuantity()).append(" | ")
                    .append(item.status() == null ? "PENDING" : item.status())
                    .append(System.lineSeparator());
        }
        for (var bill : docket.bills()) {
            output.append("Bill ").append(bill.getBillReference()).append(" | Customer ")
                    .append(bill.getCustomerId()).append(" | ")
                    .append(bill.getBillingMonth()).append(" | total ")
                    .append(bill.getMonthlyTotal()).append(" | ")
                    .append(bill.getPaymentStatus()).append(System.lineSeparator());
            for (var item : bill.getItems()) {
                output.append("  ").append(item.deliveryDate()).append(" | ")
                        .append(item.itemName()).append(" | ").append(item.amount())
                        .append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    private DeliveryDocket withBills(DeliveryDocket docket) {
        List<String> customerIds = docket.items().stream().map(item -> item.customerId()).distinct().toList();
        List<ie.newsagent.model.Bill> assigned = bills.findAll().stream()
                .filter(bill -> docket.docketReference().equals(bill.getDocketReference()))
                .filter(bill -> customerIds.contains(bill.getCustomerId()))
                .toList();
        return docket.withBills(assigned);
    }

    private void validateArea(int deliveryArea) {
        if (deliveryArea < 1 || deliveryArea > 24) {
            throw new IllegalArgumentException("Delivery area must be between 1 and 24");
        }
    }
}
