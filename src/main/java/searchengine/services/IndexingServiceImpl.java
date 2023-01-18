package searchengine.services;

import lombok.RequiredArgsConstructor;
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
            System.out.println("Индексация сайтов уже запущена!");
            return false;
        }
        System.out.println("Начинаем индексировать сайты из конфига");
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
            System.out.println("Переиндексация - " + url);
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
            System.out.println("Останавливаем индексацию");
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
            return true;
        }
        return false;
    }

}
