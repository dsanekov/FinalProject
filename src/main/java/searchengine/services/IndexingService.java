package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.parser.PageLinksExtractor;
import searchengine.model.SiteStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
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
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
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
                newSite.setStatus(SiteStatus.INDEXED);
                newSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(newSite);
            }).start();
        }
    }
    public void indexingByUrl(String url){
        System.out.println("индексируем по ссылке");
        if(urlExist(url)){
            System.out.println("Данный сайт уже индексировали");
        }
            LocalDateTime statusTime = LocalDateTime.now();
            searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",url,"Сайт без имени");
            saveSite(newSite);
            PageLinksExtractor extractor = new PageLinksExtractor(newSite.getUrl(), newSite,pageRepository);
            List<Page> allPages = new ForkJoinPool().invoke(extractor);
            newSite.setStatus(SiteStatus.INDEXED);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            findLemmas(allPages);
    }

    private Map<String, Integer> getLemmas(String content){
        Map<String, Integer> lemmasMap = new HashMap<>();
        lemmasMap.putAll(lemmaFinder.collectLemmas(content));
        return lemmasMap;
    }
    private void findLemmas(List<Page> allPages){
        for(Page page : allPages){
            String content = "";
            content += (ClearHtmlTegs.clear(page.getContent(),"title"));
            content += (ClearHtmlTegs.clear(page.getContent(),"body"));
            Map<String, Integer> lemmas = getLemmas(content);
            for(String key : lemmas.keySet()){
                Lemma newlemma = new Lemma(page.getSite(),key,lemmas.get(key));//TODO здесь надо проверять есть ли лемма в базе и потом менять число на +1.
                lemmaRepository.save(newlemma);
                Index newIndex = new Index(page,newlemma,0.5F);//TODO здесь надо ранк как то менять
                indexRepository.save(newIndex);
            }
        }
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
