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
    @Query(value = "DELETE FROM search_engine.words_index WHERE `page_id` = :pageId", nativeQuery = true)
    void deleteAllBySiteId(int pageId);
    @Query(value = "SELECT i.* FROM Words_index i WHERE i.lemma IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<Index> findIndexListByLemmasAndPages(Set<String> lemmas, List<Page> pages);
}
