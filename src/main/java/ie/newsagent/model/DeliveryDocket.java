package ie.newsagent.model;

import java.time.LocalDate;
import java.util.List;

/** A persisted daily docket for one delivery area, including its assigned bills. */
public record DeliveryDocket(
        String docketReference,
        LocalDate deliveryDate,
        int deliveryArea,
        List<DeliveryDocketItem> items,
        List<Bill> bills) {

    public DeliveryDocket {
        items = List.copyOf(items);
        bills = List.copyOf(bills);
    }

    public DeliveryDocket withBills(List<Bill> assignedBills) {
        return new DeliveryDocket(docketReference, deliveryDate, deliveryArea, items, assignedBills);
    }
}
