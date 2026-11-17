package ie.newsagent.dao;

import ie.newsagent.model.Delivery;
import java.util.List;
import java.util.Optional;
import java.time.YearMonth;

public interface DeliveryDao {
    Delivery save(Delivery delivery);
    Optional<Delivery> findByReference(String deliveryReference);
    List<Delivery> findAll();
    List<Delivery> findDeliveredForCustomer(String customerId, YearMonth month);
}
