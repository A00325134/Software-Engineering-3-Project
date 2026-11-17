package ie.newsagent.dao;

import ie.newsagent.model.Bill;
import java.util.List;
import java.util.Optional;

public interface BillDao {
    Bill save(Bill bill);
    Optional<Bill> findByReference(String billReference);
    List<Bill> findByCustomerId(String customerId);
    List<Bill> findAll();
}
