package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.parser.PageLinksExtractor;
import searchengine.model.SiteStatus;
import searchengine.parser.SiteIndexer;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.parser.LemmaFinder;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private LemmaFinder lemmaFinder;
    private ExecutorService executorService;
    private static final int processorCoreCount = Runtime.getRuntime().availableProcessors();

    {
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void startIndexing(){
        System.out.println("Индексируем сайты из конфига");
        List<Site> sitesList = sites.getSites();
        executorService = Executors.newFixedThreadPool(processorCoreCount);
        for(Site site : sitesList){
            new Thread(()->{
                LocalDateTime statusTime = LocalDateTime.now();
                searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",site.getUrl(),site.getName());
                String url = newSite.getUrl();
                executorService.submit(new SiteIndexer(url,siteRepository,pageRepository,lemmaRepository,lemmaFinder, indexRepository, sites, newSite));
                executorService.shutdown();
            }).start();
        }
    }
    public void indexingByUrl(String url){
        System.out.println("индексируем по ссылке");
        executorService = Executors.newFixedThreadPool(processorCoreCount);
        LocalDateTime statusTime = LocalDateTime.now();
        searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",url,"Сайт без имени");
        executorService.submit(new SiteIndexer(url,siteRepository,pageRepository,lemmaRepository,lemmaFinder, indexRepository, sites, newSite));
        executorService.shutdown();
    }

    public void stopIndexing() throws SQLException{
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }

}
