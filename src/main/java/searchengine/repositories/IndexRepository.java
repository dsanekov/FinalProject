package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;

@Repository
public interface IndexRepository extends CrudRepository<Index,Integer> {
    @Query(value = "DELETE FROM search_engine.index WHERE `page_id` = :pageId", nativeQuery = true)
    void deleteAllBySiteId(int pageId);
}
