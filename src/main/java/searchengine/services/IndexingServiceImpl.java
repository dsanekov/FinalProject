package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
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


@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService{
    @Autowired
    private final SitesList sites;
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
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

    @Override
    public boolean startIndexingAllSites(){
        if(isIndexing()){
            log.info("Индексация сайтов уже запущена!");
            return false;
        }
        log.info("Начата индексация сайтов из конфига");
        List<Site> sitesList = sites.getSites();
        executorService = Executors.newFixedThreadPool(processorCoreCount);
        for(Site site : sitesList){
                String url = site.getUrl();
                executorService.submit(new SiteIndexer(url,siteRepository,pageRepository,lemmaRepository,lemmaFinder, indexRepository, sites));
        }
        executorService.shutdown();
        return true;
    }
    @Override
    public boolean startIndexingByUrl(String url){
        if(urlCheck(url)){
            log.info("Начата переиндексация - " + url);
            executorService = Executors.newFixedThreadPool(processorCoreCount);
            executorService.submit(new SiteIndexer(url,siteRepository,pageRepository,lemmaRepository,lemmaFinder, indexRepository, sites));
            executorService.shutdown();
            return true;
        }
        return false;
    }
    private boolean urlCheck(String url) {
        List<Site> urlList = sites.getSites();
        for (Site site : urlList) {
            if (site.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }
    private boolean isIndexing(){
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        for(searchengine.model.Site site : siteIterable){
            if(site.getStatus() == SiteStatus.INDEXING){
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean stopIndexing(){
        if(isIndexing()){
            executorService.shutdownNow();
            Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
            for(searchengine.model.Site site : siteIterable){
                if (site.getStatus() == SiteStatus.INDEXING){
                    site.setStatus(SiteStatus.FAILED);
                    site.setLastError("Индексация остановлена пользователем");
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                    }
                }
            log.info("Индексация отановлена пользователем");
            return true;
        }
        return false;
    }

}
