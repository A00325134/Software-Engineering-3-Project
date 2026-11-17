package ie.newsagent.service;

import ie.newsagent.dao.SubscriptionDao;
import ie.newsagent.model.DeliveryFrequency;
import ie.newsagent.model.Subscription;
import java.util.List;
import java.util.Objects;

public final class SubscriptionService {
    private final SubscriptionDao dao;
    public SubscriptionService(SubscriptionDao dao) { this.dao = Objects.requireNonNull(dao); }
    public Subscription create(String id, String customerId, String publicationId, DeliveryFrequency frequency) {
        validate(id, customerId, publicationId, frequency);
        if (dao.findById(id.trim()).isPresent()) throw new IllegalArgumentException("Subscription already exists: " + id);
        return dao.save(new Subscription(id.trim(), customerId.trim(), publicationId.trim(), frequency));
    }
    public Subscription get(String id) { return dao.findById(id).orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id)); }
    public List<Subscription> getAll() { return dao.findAll(); }
    public List<Subscription> getForCustomer(String customerId) { return dao.findByCustomerId(customerId); }
    public Subscription pause(String id) { return setPaused(id, true); }
    public Subscription resume(String id) { return setPaused(id, false); }
    public void delete(String id) { if (!dao.deleteById(id)) throw new IllegalArgumentException("Subscription not found: " + id); }
    private Subscription setPaused(String id, boolean paused) {
        Subscription subscription = get(id); subscription.setPaused(paused);
        if (!dao.update(subscription)) throw new IllegalArgumentException("Subscription not found: " + id);
        return subscription;
    }
    private void validate(String id, String customerId, String publicationId, DeliveryFrequency frequency) {
        if (id == null || id.isBlank() || customerId == null || customerId.isBlank() || publicationId == null || publicationId.isBlank())
            throw new IllegalArgumentException("Subscription, customer, and publication IDs are required.");
        if (frequency == null) throw new IllegalArgumentException("Delivery frequency is required.");
    }
}
