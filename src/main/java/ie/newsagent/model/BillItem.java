package ie.newsagent.model;

import java.time.LocalDate;

public record BillItem(String itemName, LocalDate deliveryDate, java.math.BigDecimal amount, String deliveryReference) {
    public BillItem(String itemName, LocalDate deliveryDate, java.math.BigDecimal amount) { this(itemName,deliveryDate,amount,null); }
}
