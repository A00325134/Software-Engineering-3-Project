package ie.newsagent.dao;

import ie.newsagent.model.Delivery;
import ie.newsagent.model.DeliveryDocket;
import ie.newsagent.model.DeliveryStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.time.YearMonth;

public interface DeliveryDao {
    Delivery save(Delivery delivery);
    Optional<Delivery> findByReference(String deliveryReference);
    List<Delivery> findAll();
    List<Delivery> findDeliveredForCustomer(String customerId, YearMonth month);
    DeliveryDocket generateDocket(LocalDate deliveryDate, int deliveryArea);
    DeliveryDocket findDocket(String docketReference);
    Delivery saveOutcome(String docketReference, String deliveryReference, DeliveryStatus status);
}
