package searchengine.services;

import searchengine.dto.statistics.SearchObject;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    List<SearchObject> searchOnSite(String query, String site, int offset, int limit) throws IOException;
    List<SearchObject> searchAllSites(String query,int offset, int limit) throws IOException;
}
