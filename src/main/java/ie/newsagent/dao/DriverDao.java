package ie.newsagent.dao;

import ie.newsagent.model.DeliveryDriver;
import java.util.List;
import java.util.Optional;

public interface DriverDao {
    DeliveryDriver save(DeliveryDriver driver);
    boolean updateById(String driverId, DeliveryDriver driver);
    Optional<DeliveryDriver> findById(String driverId);
    List<DeliveryDriver> findAll();
    boolean deleteById(String driverId);
}
