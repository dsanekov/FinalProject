package searchengine.parser;


import lombok.extern.slf4j.Slf4j;
import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@Slf4j
public class SiteIndexer implements Runnable{
private String url;
private SiteRepository siteRepository;
private PageRepository pageRepository;
private LemmaRepository lemmaRepository;
private LemmaFinder lemmaFinder;
private IndexRepository indexRepository;
private SitesList sites;


    public SiteIndexer(String url, SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, LemmaFinder lemmaFinder, IndexRepository indexRepository, SitesList sites) {
        this.url = url;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.lemmaFinder = lemmaFinder;
        this.indexRepository = indexRepository;
        this.sites = sites;
    }

    @Override
    public void run() {
        if(siteRepository.findSiteByUrl(url) != null){
            log.info("Удаление информации из базы о сайте" + url);
            System.out.println("Удаляем старые данные из базы");
            deleteOldData();
        }
        Site newSite = new Site(SiteStatus.INDEXING,LocalDateTime.now(),"NULL",url,getSiteNameByUrl());
        siteRepository.save(newSite);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PageLinksExtractor extractor = new PageLinksExtractor(url,newSite,pageRepository,siteRepository);
        List<Page> allPages = forkJoinPool.invoke(extractor);
        forkJoinPool.shutdown();
        extractor.clearUrlList();
        findLemmas(allPages, newSite);
    }
    private void findLemmas(List<Page> allPages, Site newSite){
        log.info("Начат поиск лемм по сайту - " + newSite.getUrl());

        for(Page page : allPages){
            if(page.getCode()/100 == 4 || page.getCode()/100 == 5){
                continue;
            }
            List<Lemma> lemmaList = new CopyOnWriteArrayList<>();
            List<Index> indexList = new CopyOnWriteArrayList<>();
            String content = "";
            content += (ClearHtmlTegs.clear(page.getContent(),"title"));
            content += " ";
            content += (ClearHtmlTegs.clear(page.getContent(),"body"));
            Map<String, Integer> lemmasOnPage = new TreeMap<>(lemmaFinder.collectLemmas(content));
            for(String key : lemmasOnPage.keySet()){
                int frequency = 0;
                Lemma newLemma = new Lemma(newSite,key,frequency);
                lemmaList.add(newLemma);
                Index newIndex = new Index(page,newLemma,lemmasOnPage.get(key));
                indexList.add(newIndex);
            }
            lemmaRepository.saveAll(lemmaList);
            indexRepository.saveAll(indexList);
        }
        log.info("Поиск лемм закончен. Сайт - " + newSite.getUrl());
        updateLemmasFrequency(newSite);
    }
    private void updateLemmasFrequency(Site newSite){
        log.info("Начат процесс обнолвения лемм по сайту - " + newSite.getUrl());
        System.out.println("Начинаем обновлять частоту лемм");

        List<Lemma> lemmaIterable = lemmaRepository.findAllContains(newSite.getId());
        List<Lemma> lemmaListForSave = new CopyOnWriteArrayList<>();
        int counter = 0;
        for(Lemma lFromDB : lemmaIterable){
            int frequency = 0;
            for(Lemma lFromDB2 : lemmaIterable){
                if(lFromDB.getLemma().equals(lFromDB2.getLemma())){
                    frequency++;
                }
            }
            lFromDB.setFrequency(frequency);
            lemmaListForSave.add(lFromDB);
            counter++;
            if(counter == 1000){
                counter = 0;
                lemmaRepository.saveAll(lemmaListForSave);
                lemmaListForSave.clear();
            }
        }
        if(counter != 0){
            lemmaRepository.saveAll(lemmaListForSave);
            lemmaListForSave.clear();
        }
        System.out.println("Конец");
        newSite.setStatus(SiteStatus.INDEXED);
        newSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(newSite);
        log.info("Обновление частоты лемм закончено. Сайт - " + newSite.getUrl());
    }

    private void deleteOldData(){
        Site site = siteRepository.findSiteByUrl(url);
        site.setStatus(SiteStatus.INDEXING);
        site.setName(getSiteNameByUrl());
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        siteRepository.delete(site);
    }
    private String getSiteNameByUrl() {
        List<searchengine.config.Site> siteList = sites.getSites();
        for (searchengine.config.Site site : siteList) {
            if (site.getUrl().equals(url)) {
                return site.getName();
            }
        }
        return "";
    }

}
