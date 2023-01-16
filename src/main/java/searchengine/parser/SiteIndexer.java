package searchengine.parser;


import searchengine.config.SitesList;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.ClearHtmlTegs;

import java.time.LocalDateTime;
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
        Map<String,Integer> allLemmas = new HashMap<>();
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
