package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;

@Repository
public interface PageRepository extends CrudRepository<Page,Integer> {
    @Query(value = "SELECT * FROM search_engine.page WHERE `site_id` = :siteId", nativeQuery = true)
    List<Page> findAllBySiteId(int siteId);

    long countBySiteId(Site siteId);
    @Query(value = "SELECT * FROM Page p JOIN Words_index i ON p.id = i.page_id WHERE i.lemma_id = :lemma", nativeQuery = true)
    List<Page> findPagesByLemmas(Lemma lemma);
    @Query(value = "SELECT * FROM Page p JOIN Words_index i ON p.id = i.page_id WHERE i.lemma_id = :lemma AND p.page_id IN :pages", nativeQuery = true)
    List<Page> findPagesByLemma(Lemma lemma, List<Page> pages);
}
