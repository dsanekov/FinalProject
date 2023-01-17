package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma,Integer> {

    @Query(value = "SELECT * FROM search_engine.lemma WHERE `site_id` = :siteId", nativeQuery = true)
    List<Lemma> findAllContains(int siteId);
    @Query(value = "DELETE FROM search_engine.lemma WHERE `site_id` = :siteId", nativeQuery = true)
    void deleteAllBySiteId(int siteId);
}
