package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page,Integer> {
    @Query(value = "SELECT * FROM search_engine.page WHERE `site_id` = :siteId", nativeQuery = true)
    List<Page> findAllBySiteId(int siteId);

    long countBySiteId(Site siteId);
}
