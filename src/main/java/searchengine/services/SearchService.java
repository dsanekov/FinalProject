package searchengine.services;

import searchengine.dto.statistics.SearchObject;

import java.util.List;

public interface SearchService {
    List<SearchObject> searchOnSite(String query, String site, int offset, int limit);
    List<SearchObject> searchAllSites(String query,int offset, int limit);
}
