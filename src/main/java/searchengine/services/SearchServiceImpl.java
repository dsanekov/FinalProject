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
        System.out.println("Начали поиск по запросу - " + query + " Для сайта - " + siteUrl);
        List<SearchObject> searchObjectList = new ArrayList<>();
        Map<String, Integer> lemmasOnPage = new TreeMap<>(lemmaFinder.collectLemmas(query));
        System.out.println("Размер мапы lemmasOnPage - " + lemmasOnPage.keySet().size());
        Site site = siteRepository.findSiteByUrl(siteUrl);
        List<Lemma> lemmaList = lemmaRepository.findLemmaListBySetAndSite(lemmasOnPage.keySet(),site.getId());
        lemmaList.sort(Comparator.comparing(Lemma::getFrequency));
        System.out.println("____________________________________");
        for(Lemma lemma : lemmaList){
            System.out.println(lemma.getLemma() + " " + lemma.getFrequency());
        }
        System.out.println("____________________________________");
        if(lemmaList.size() == 0){
            System.out.println("Размер lemmaList равен 0. Site - " + siteUrl);
            return searchObjectList;
        }
        System.out.println("Размер lemmaList равен - " + lemmaList.size());
        List<Page> pageListByRareLemma = pageRepository.findPagesByLemmaId(lemmaList.get(0).getId());
        System.out.println("_____________________");
        System.out.println("Размер pageListByRareLemma - " + pageListByRareLemma.size());
        for (Page page : pageListByRareLemma){
            System.out.println("PageId - " + page.getId());
        }
        System.out.println("___________________");
        if(pageListByRareLemma.size() == 0){
            return searchObjectList;
        }
        for (int i = 1; i < lemmaList.size(); i++) {
            for (int j = 0; j < pageListByRareLemma.size(); j++) {
                if(indexRepository.countByLemmaContentAndPageId(lemmaList.get(i).getLemma(), pageListByRareLemma.get(j).getId()).size() == 0){
                    pageListByRareLemma.remove(j);
                }
            }
        }
        //todo здесь косяк
        System.out.println("Размер pageListByRareLemma после иттерация - " + pageListByRareLemma.size());
        if(pageListByRareLemma.size() == 0){
            System.out.println("Размер pageListByRareLemma равен 0");
            return searchObjectList;
        }

        List<Index> indexList = indexRepository.findIndexListByLemmasAndPages(lemmasOnPage.keySet(),pageListByRareLemma);
        Map <Page, Float> pageRelevanceMap = getPageAbsRelevance(pageListByRareLemma,indexList);
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
