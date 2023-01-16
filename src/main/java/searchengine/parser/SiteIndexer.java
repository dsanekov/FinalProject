package searchengine.parser;


import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;


public class SiteIndexer implements Runnable{
private String url;
private SiteRepository siteRepository;
private PageRepository pageRepository;
private LemmaRepository lemmaRepository;
private LemmaFinder lemmaFinder;
private IndexRepository indexRepository;
private final SitesList sites;
private searchengine.model.Site newSite;


    public SiteIndexer(String url, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, LemmaFinder lemmaFinder, IndexRepository indexRepository, SitesList sites, searchengine.model.Site newSite) {
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.lemmaFinder = lemmaFinder;
        this.indexRepository = indexRepository;
        this.sites = sites;
        this.newSite = newSite;
    }

    @Override
    public void run() {
        //deleteOldData(newSite); TODO проблема с удаланеним
        siteRepository.save(newSite);
        PageLinksExtractor extractor = new PageLinksExtractor(newSite.getUrl(), newSite,pageRepository);
        List<Page> allPages = new ForkJoinPool().invoke(extractor);
        newSite.setStatus(SiteStatus.INDEXED);
        newSite.setStatusTime(LocalDateTime.now());
        findLemmas(allPages);
    }
    private void findLemmas(List<Page> allPages){
        List<String> allLemmasList = new ArrayList<>();

        for(Page page : allPages){
            String content = "";
            content += (ClearHtmlTegs.clear(page.getContent(),"title"));
            content += " ";
            content += (ClearHtmlTegs.clear(page.getContent(),"body"));
            Map<String, Integer> lemmasOnPage = getLemmas(content);
            allLemmasList.addAll(lemmasOnPage.keySet());
            for(String key : lemmasOnPage.keySet()){
                int frequency = 0;
                Lemma newLemma = new Lemma(newSite,key,frequency);//todo сделать сохраннение списком а не по штучно
                lemmaRepository.save(newLemma);
                Index newIndex = new Index(page,newLemma,lemmasOnPage.get(key));
                indexRepository.save(newIndex);
            }
        }
        System.out.println("Начинаем обновлять частоту лемм");

        List<Lemma> lemmaIterable = lemmaRepository.findAllContains(newSite.getId());//TODO частоту сделать не по базе а по спискам. и сохранять тоже пачкой. Можно например по 1000 лемм разом.
        for(Lemma lFromDB : lemmaIterable){//TODO можно еще сделать тримап и проверять не с начала списка а с данного места, там же упорядоченно все
            int frequency = 0;
            for(Lemma lFromDB2 : lemmaIterable){
                if(lFromDB.getLemma().equals(lFromDB2.getLemma())){
                    frequency++;
                }
            }
            lFromDB.setFrequency(frequency);
            lemmaRepository.save(lFromDB);
        }
        System.out.println("Конец");
        newSite.setStatus(SiteStatus.INDEXED);
        newSite.setStatusTime(LocalDateTime.now());
    }
    private Map<String, Integer> getLemmas(String content){
        Map<String, Integer> lemmasMap = new HashMap<>();
        lemmasMap.putAll(lemmaFinder.collectLemmas(content));
        return lemmasMap;
    }

    private void deleteOldData(Site newSite){
        Iterable<searchengine.model.Site> siteIterable = siteRepository.findAll();
        for(searchengine.model.Site s : siteIterable){
                if(newSite.getUrl().equals(s.getUrl())){
                    System.out.println("Удаляем из базы сайт с url - " + s.getUrl() + " id - " + s.getId());
                    siteRepository.delete(s);
                    break;
                }
        }
    }

}
