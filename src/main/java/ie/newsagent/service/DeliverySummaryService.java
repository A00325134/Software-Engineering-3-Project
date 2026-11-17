package ie.newsagent.service;

import ie.newsagent.dao.CustomerDao;
import ie.newsagent.dao.DeliveryDao;
import ie.newsagent.model.Delivery;
import ie.newsagent.model.DeliveryStatus;
import ie.newsagent.model.DeliverySummary;
import java.util.List;
import java.util.Objects;

/** Builds a per-customer delivery summary from saved delivery history. */
public final class DeliverySummaryService {
    private final CustomerDao customers; private final DeliveryDao deliveries;
    public DeliverySummaryService(CustomerDao customers,DeliveryDao deliveries){this.customers=Objects.requireNonNull(customers);this.deliveries=Objects.requireNonNull(deliveries);}
    public List<DeliverySummary> getSummary(){
        List<Delivery> rows=deliveries.findAll();
        return customers.findAll().stream().map(c->new DeliverySummary(c.getCustomerId(),c.getName(),rows.stream().filter(d->d.getCustomerId().equals(c.getCustomerId())).toList())).toList();
    }
    public List<DeliverySummary> completed(){return getSummary().stream().map(s->new DeliverySummary(s.customerId(),s.customerName(),s.deliveries().stream().filter(d->d.getStatus()==DeliveryStatus.DELIVERED).toList())).toList();}
}
