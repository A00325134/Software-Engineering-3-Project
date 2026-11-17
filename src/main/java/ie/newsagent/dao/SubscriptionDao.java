package ie.newsagent.dao;

import ie.newsagent.model.Subscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionDao {
    Subscription save(Subscription subscription);
    boolean update(Subscription subscription);
    Optional<Subscription> findById(String subscriptionId);
    List<Subscription> findByCustomerId(String customerId);
    List<Subscription> findAll();
    boolean deleteById(String subscriptionId);
}
