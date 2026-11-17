package ie.newsagent.dao.memory;

import ie.newsagent.dao.PublicationDao;
import ie.newsagent.model.Publication;
import java.util.*;

public class InMemoryPublicationDao implements PublicationDao {
    private final Map<String, Publication> records = new LinkedHashMap<>();
    public Publication save(Publication p) { records.put(p.getPublicationId(), p); return p; }
    public boolean updateName(String id, String name) { Publication p = records.get(id); if (p == null) return false; p.setName(name); return true; }
    public boolean updateStock(String id, int quantity) { Publication p = records.get(id); if (p == null) return false; p.setStockQuantity(quantity); return true; }
    public Optional<Publication> findById(String id) { return Optional.ofNullable(records.get(id)); }
    public List<Publication> findAll() { return List.copyOf(records.values()); }
    public boolean deleteById(String id) { return records.remove(id) != null; }
    public List<Publication> findLowStock() { return records.values().stream().filter(Publication::isLowStock).toList(); }
}
