package ie.newsagent.service;

import ie.newsagent.dao.BillDao;
import ie.newsagent.dao.CustomerDao;
import ie.newsagent.dao.DeliveryDao;
import ie.newsagent.model.Bill;
import ie.newsagent.model.BillItem;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Creates and retrieves a customer's monthly bill. */
public final class BillingService {
    private final BillDao bills;
    private final CustomerDao customers;
    private final DeliveryDao deliveries;
    private final BigDecimal unitPrice;
    public BillingService(BillDao bills) { this(bills,null,null,BigDecimal.ONE); }
    public BillingService(BillDao bills,CustomerDao customers,DeliveryDao deliveries,BigDecimal unitPrice) {
        this.bills=Objects.requireNonNull(bills);this.customers=customers;this.deliveries=deliveries;this.unitPrice=Objects.requireNonNull(unitPrice);
    }
    public Bill generate(String customerId, YearMonth month, List<BillItem> deliveredItems) {
        if(customerId==null||customerId.isBlank())throw new IllegalArgumentException("Customer ID is required");
        Objects.requireNonNull(month,"Billing month is required"); Objects.requireNonNull(deliveredItems,"Bill items are required");
        if(!bills.findByCustomerId(customerId).stream().filter(b->b.getBillingMonth().equals(month)).toList().isEmpty())
            throw new IllegalStateException("A bill already exists for this customer and month");
        List<BillItem> items=List.copyOf(deliveredItems); BigDecimal total=BigDecimal.ZERO;
        for(BillItem item:items) {
            if(item==null||item.itemName()==null||item.itemName().trim().length()<3||item.itemName().trim().length()>50)
                throw new IllegalArgumentException("Item description must be 3 to 50 characters");
            Objects.requireNonNull(item.deliveryDate(),"Delivery date is required");
            if(item.amount()==null||item.amount().signum()<0)throw new IllegalArgumentException("Item amount must be non-negative");
            total=total.add(item.amount());
        }
        if(total.toPlainString().length()>8)throw new IllegalArgumentException("Monthly total must fit within 8 characters");
        return bills.save(new Bill("B"+UUID.randomUUID().toString().replace("-",""),customerId,month,items,total));
    }
    /** Bills every customer for delivered items in the month; customers without deliveries get a zero bill. */
    public List<Bill> generateMonthlyBills(YearMonth month, CustomerDao customers, DeliveryDao deliveries, BigDecimal unitPrice) {
        Objects.requireNonNull(month); Objects.requireNonNull(customers); Objects.requireNonNull(deliveries);
        if(unitPrice==null||unitPrice.signum()<0)throw new IllegalArgumentException("Unit price must be non-negative");
        List<Bill> generated=new java.util.ArrayList<>();
        for(var customer:customers.findAll()) {
            if(bills.findByCustomerId(customer.getCustomerId()).stream().anyMatch(b->b.getBillingMonth().equals(month))) continue;
            List<BillItem> items=deliveries.findDeliveredForCustomer(customer.getCustomerId(),month.minusMonths(1)).stream()
                    .map(d->new BillItem(d.getPublicationName(),d.getDeliveryDate(),unitPrice,d.getDeliveryReference())).toList();
            generated.add(generate(customer.getCustomerId(),month,items));
        }
        return List.copyOf(generated);
    }
    /** Invoked on the first calendar day to bill the previous month's completed deliveries. */
    public List<Bill> generateOnFirstOfMonth(java.time.LocalDate today) {
        if(today.getDayOfMonth()!=1)return List.of();
        if(customers==null||deliveries==null)throw new IllegalStateException("Delivery billing sources are not configured");
        return generateMonthlyBills(YearMonth.from(today),customers,deliveries,unitPrice);
    }
    public Bill get(String reference) { if(reference==null||reference.isBlank())throw new IllegalArgumentException("Bill reference is required"); return bills.findByReference(reference).orElseThrow(()->new IllegalArgumentException("Bill not found: "+reference)); }
    public List<Bill> forCustomer(String customerId) { if(customerId==null||customerId.isBlank())throw new IllegalArgumentException("Customer ID is required"); return bills.findByCustomerId(customerId); }
}
