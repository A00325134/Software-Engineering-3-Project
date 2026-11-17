package ie.newsagent.service;

import ie.newsagent.dao.PublicationDao;
import ie.newsagent.model.Publication;
import java.util.List;
import java.util.Objects;

public class PublicationService {
    private final PublicationDao dao;
    public PublicationService(PublicationDao dao) { this.dao = Objects.requireNonNull(dao); }
    public Publication create(String id, String name, int stock) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Publication ID is required.");
        if (name == null || name.trim().length() < 3 || name.trim().length() > 50) throw new IllegalArgumentException("Publication name must be 3–50 characters.");
        if (stock < 10 || stock > 1000) throw new IllegalArgumentException("Starting stock must be 10–1000.");
        if (dao.findById(id.trim()).isPresent()) throw new IllegalArgumentException("Publication already exists: " + id);
        return dao.save(new Publication(id.trim(), name.trim(), stock));
    }
    public Publication get(String id) { return dao.findById(id).orElseThrow(() -> new IllegalArgumentException("Publication not found: " + id)); }
    public List<Publication> lowStock() { return dao.findLowStock(); }
    public List<Publication> getAll() { return dao.findAll(); }
    public Publication updateName(String id, String name) {
        if (name == null || name.trim().length() < 3 || name.trim().length() > 50) {
            throw new IllegalArgumentException("Publication name must be 3-50 characters.");
        }
        Publication publication = get(id);
        if (!dao.updateName(id, name.trim())) throw new IllegalArgumentException("Publication not found: " + id);
        publication.setName(name.trim());
        return publication;
    }
    public void delete(String id) {
        if (!dao.deleteById(id)) throw new IllegalArgumentException("Publication not found: " + id);
    }
    public Publication updateStock(String id, int stock) {
        if (stock < 0 || stock > 1000) throw new IllegalArgumentException("Stock quantity must be 0-1000.");
        Publication publication = get(id);
        if (!dao.updateStock(id, stock)) throw new IllegalArgumentException("Publication not found: " + id);
        publication.setStockQuantity(stock);
        return publication;
    }
}
