package searchengine.services;

public interface IndexingService {
    boolean startIndexingAllSites();
    boolean startIndexingByUrl(String url);
    boolean stopIndexing();

}
