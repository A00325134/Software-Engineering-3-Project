package ie.newsagent.dao;

import ie.newsagent.model.Delivery;
import java.util.List;
import java.util.Optional;

public interface DeliveryDao {
    Delivery save(Delivery delivery);
    Optional<Delivery> findByReference(String deliveryReference);
    List<Delivery> findAll();
}
