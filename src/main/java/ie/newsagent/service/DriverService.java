package ie.newsagent.service;

import ie.newsagent.dao.DriverDao;
import ie.newsagent.model.DeliveryDriver;
import java.util.List;
import java.util.Objects;

public final class DriverService {
    private final DriverDao dao;
    public DriverService(DriverDao dao) { this.dao = Objects.requireNonNull(dao); }
    public DeliveryDriver create(String id, String name, String passwordHash) {
        validate(id, name, passwordHash);
        if (dao.findById(id.trim()).isPresent()) throw new IllegalArgumentException("Driver already exists: " + id);
        return dao.save(new DeliveryDriver(id.trim(), name.trim(), passwordHash.trim()));
    }
    public DeliveryDriver get(String id) { return dao.findById(id).orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id)); }
    public List<DeliveryDriver> getAll() { return dao.findAll(); }
    public DeliveryDriver update(String id, String name, String passwordHash) {
        validate(id, name, passwordHash);
        DeliveryDriver driver = new DeliveryDriver(id.trim(), name.trim(), passwordHash.trim());
        if (!dao.updateById(id.trim(), driver)) throw new IllegalArgumentException("Driver not found: " + id);
        return driver;
    }
    public void delete(String id) { if (!dao.deleteById(id)) throw new IllegalArgumentException("Driver not found: " + id); }
    private void validate(String id, String name, String hash) {
        if (id == null || id.isBlank() || name == null || name.isBlank() || hash == null || hash.isBlank())
            throw new IllegalArgumentException("Driver ID, name, and password hash are required.");
    }
}
