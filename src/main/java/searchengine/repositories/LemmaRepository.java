package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Set;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma,Integer> {

    @Query(value = "SELECT * FROM search_engine.lemma WHERE `site_id` = :siteId", nativeQuery = true)
    List<Lemma> findAllContains(int siteId);
    @Query(value = "SELECT * FROM search_engine.lemma WHERE lemma IN :lemmaList AND `site_id` = :siteId", nativeQuery = true)
    List<Lemma> findLemmaListBySetAndSite(Set<String> lemmaList, int siteId);


    long countBySiteId(Site siteId);
}
