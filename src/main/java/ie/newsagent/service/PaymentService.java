package ie.newsagent.service;

import ie.newsagent.dao.BillDao;
import ie.newsagent.model.Bill;
import ie.newsagent.model.BillPaymentStatus;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/** Records payments and returns reminders for unpaid bills from the prior month. */
public final class PaymentService {
    private final BillDao bills;
    public PaymentService(BillDao bills) { this.bills=Objects.requireNonNull(bills); }
    public Bill markPaid(String reference) {
        if(reference==null||reference.isBlank())throw new IllegalArgumentException("Bill reference is required");
        if(!bills.markPaid(reference))throw new IllegalArgumentException("Bill not found: "+reference);
        return bills.findByReference(reference).orElseThrow(()->new IllegalArgumentException("Bill not found: "+reference));
    }
    public List<Bill> remindersFor(YearMonth month) { Objects.requireNonNull(month); return bills.findAll().stream().filter(b->b.getBillingMonth().plusMonths(1).equals(month)&&b.getPaymentStatus()==BillPaymentStatus.UNPAID).toList(); }
}
