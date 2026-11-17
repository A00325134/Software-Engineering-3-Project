package ie.newsagent.service;

import ie.newsagent.dao.PublicationDao;
import ie.newsagent.model.Publication;
import java.util.List;

public class PublicationService {
    private final PublicationDao dao;
    public PublicationService(PublicationDao dao) { this.dao = dao; }
    public Publication create(String id, String name, int stock) {
        if (name == null || name.trim().length() < 3 || name.trim().length() > 50) throw new IllegalArgumentException("Publication name must be 3–50 characters.");
        if (stock < 10 || stock > 1000) throw new IllegalArgumentException("Starting stock must be 10–1000.");
        return dao.save(new Publication(id, name.trim(), stock));
    }
    public Publication get(String id) { return dao.findById(id).orElseThrow(() -> new IllegalArgumentException("Publication not found: " + id)); }
    public List<Publication> lowStock() { return dao.findLowStock(); }
}
