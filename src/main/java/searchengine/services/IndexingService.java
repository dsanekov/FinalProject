package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.DBConnection;
import searchengine.model.Page;
import searchengine.parser.PageLinksExtractor;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.ClearHtmlTegs;
import searchengine.utils.LemmaFinder;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
public class IndexingService {
    @Autowired
    private final SitesList sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    private LemmaFinder lemmaFinder;

    {
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void startIndexing() throws SQLException {
        String[] result = { "true", "false" };
        String error = "Данная страница находится за пределами сайтов,указанных в конфигурационном файле";
        List<Page> allPages = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        deleteOldData();
        for(Site site : sitesList){
            new Thread(()->{
                LocalDateTime statusTime = LocalDateTime.now();
                searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",site.getUrl(),site.getName());
                saveSite(newSite);
                PageLinksExtractor extractor = new PageLinksExtractor(newSite.getUrl(), newSite,pageRepository);
                List<Page> siteSet = new ForkJoinPool().invoke(extractor);
                allPages.addAll(siteSet);
            }).start();
        }
        //todo после индексации поменять у Site статус и время.
    }
    public void indexingByUrl(String url){
        System.out.println("индексируем по ссылке");
        if(urlExist(url)){
            System.out.println("Данный сайт уже индексировали");
        }
            List<Page> allPages = new ArrayList<>();
            LocalDateTime statusTime = LocalDateTime.now();
            searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",url,"Сайт без имени");
            saveSite(newSite);
            PageLinksExtractor extractor = new PageLinksExtractor(newSite.getUrl(), newSite,pageRepository);
            allPages = new ForkJoinPool().invoke(extractor);
            Set<String> lines = new TreeSet<>();

            for(Page page : allPages){
                lines.add(page.getContent());
            }
            Map<String, Integer> map = new HashMap<>();
            map = letsIndexPages(lines);
            for (String k : map.keySet()){
                System.out.println(k + " - " + map.get(k));
            }
    }
    private Map<String, Integer> letsIndexPages(Set<String> pages){
        Map<String, Integer> lemmasMap = new HashMap<>();
        for(String url : pages) {
            lemmasMap.putAll(lemmaFinder.collectLemmas(url));
            url = ClearHtmlTegs.clear(url,"title");
        }
        return lemmasMap;
    }

    public void stopIndexing() throws SQLException{
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }
    private void deleteOldData(){
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        for(searchengine.model.Site s : siteIterable){
            for(Site site : sites.getSites()) {
                if (site.getUrl().equals(s.getUrl())) {
                    siteRepository.delete(s);
                }
            }
        }
    }
    private void saveSite(searchengine.model.Site site){
        siteRepository.save(site);
    }
    private boolean urlExist(String url){
        for(Site site : sites.getSites()){
            if(site.getUrl().equals(url)){
                return true;
            }
        }
        return false;
    }
}
