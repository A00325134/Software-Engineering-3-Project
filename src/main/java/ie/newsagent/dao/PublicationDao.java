package ie.newsagent.dao;

import ie.newsagent.model.Publication;
import java.util.List;
import java.util.Optional;

public interface PublicationDao {
    Publication save(Publication publication);
    Optional<Publication> findById(String publicationId);
    List<Publication> findAll();
    boolean deleteById(String publicationId);
    List<Publication> findLowStock();
}
