package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;
import java.util.Set;

@Repository
public interface IndexRepository extends CrudRepository<Index,Integer> {
    long countByLemmaId(int lemmaId);
    @Query(value = "DELETE FROM search_engine.words_index WHERE `page_id` = :pageId", nativeQuery = true)
    void deleteAllBySiteId(int pageId);
    @Query(value = "SELECT i.* FROM search_engine.words_index i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<Index> findIndexListByLemmasAndPages(List<Lemma> lemmas, List<Page> pages);
    @Query(value = "SELECT COUNT(*) FROM search_engine.words_index WHERE lemma_id = :lemmaId AND page_id = :pageId", nativeQuery = true)
    long countByLemmaIdAndPageId(int lemmaId, int pageId);
    @Query(value = "SELECT i.* FROM search_engine.words_index i JOIN search_engine.lemma l ON i.lemma_id = l.id WHERE l.lemma = ':lemmaContent' AND i.page_id = :pageId", nativeQuery = true)
    List<Index> countByLemmaContentAndPageId(String lemmaContent, int pageId);
}
