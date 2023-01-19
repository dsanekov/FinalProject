package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchObject;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.parser.LemmaFinder;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    private LemmaFinder lemmaFinder;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    {
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public List<SearchObject> searchOnSite(String query, String site, int offset, int limit) {

        return searchStarter(query, site, offset, limit);
    }

    @Override
    public List<SearchObject> searchAllSites(String query, int offset, int limit) {
        Iterable<Site> siteIterable = siteRepository.findAll();
        List<SearchObject> resultList = new ArrayList<>();

        for(Site site : siteIterable){
            resultList.addAll(searchStarter(query,site.getUrl(),offset,limit));
        }
        return resultList;
    }
    private List<SearchObject> searchStarter(String query, String siteUrl, int offset, int limit){
        List<SearchObject> searchObjectList = new ArrayList<>();
        Map<String, Integer> lemmasOnPage = new TreeMap<>(lemmaFinder.collectLemmas(query));
        Site site = siteRepository.findSiteByUrl(siteUrl);
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySetAndSite(lemmasOnPage.keySet(),site);
        lemmaList.sort(Comparator.comparing(Lemma::getFrequency));
        List<Page> pageListByLemmas = pageRepository.findPagesByLemmas(lemmaList);
        System.out.println("Проверка! Размер pageListByLemmas - " + pageListByLemmas.size());

        for(Lemma lemma : lemmaList){
            pageListByLemmas = pageRepository.findPagesByLemma(lemma, pageListByLemmas);
            System.out.println("Проверка! Размер pageListByLemmas в цикле - " + pageListByLemmas.size());
        }
        if(pageListByLemmas.size() == 0){
            System.out.println("Размер pageListByLemmas равен 0");
            return searchObjectList;
        }

        List<Index> indexList = indexRepository.findIndexListByLemmasAndPages(lemmasOnPage.keySet(),pageListByLemmas);
        Map <Page, Float> pageRelevanceMap = getPageAbsRelevance(pageListByLemmas,indexList);
        //todo здесь конец
        return searchObjectList;
    }
    private Map <Page, Float> getPageAbsRelevance(List<Page> pageList, List<Index> indexList){
        Map <Page, Float> pageRelevanceMap = new HashMap<>();
        for(Page page : pageList){
            float relevancy = 0;
            for(Index index : indexList){
                if (index.getPage() == page) {
                    relevancy += index.getRank();
                }
            }
            pageRelevanceMap.put(page,relevancy);
        }
        Map <Page, Float> pageAbsRelevanceMap = new HashMap<>();
        for(Page page : pageRelevanceMap.keySet()){
            float absRelevancy = pageRelevanceMap.get(page) / Collections.max(pageRelevanceMap.values());
            pageAbsRelevanceMap.put(page,absRelevancy);
        }
        return pageAbsRelevanceMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }


}
