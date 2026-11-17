package ie.newsagent.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class Bill {
    private final String billReference;
    private final String customerId;
    private final YearMonth billingMonth;
    private final List<BillItem> items;
    private BigDecimal monthlyTotal;
    private BillPaymentStatus paymentStatus = BillPaymentStatus.UNPAID;
    private String docketReference;

    public Bill(String billReference, String customerId, YearMonth billingMonth, List<BillItem> items, BigDecimal monthlyTotal) {
        this.billReference = billReference; this.customerId = customerId; this.billingMonth = billingMonth;
        this.items = List.copyOf(items); this.monthlyTotal = monthlyTotal;
    }
    public String getBillReference() { return billReference; }
    public String getCustomerId() { return customerId; }
    public YearMonth getBillingMonth() { return billingMonth; }
    public List<BillItem> getItems() { return items; }
    public BigDecimal getMonthlyTotal() { return monthlyTotal; }
    public void setMonthlyTotal(BigDecimal monthlyTotal) { this.monthlyTotal = monthlyTotal; }
    public BillPaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(BillPaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getDocketReference() { return docketReference; }
    public void setDocketReference(String docketReference) { this.docketReference=docketReference; }
}
