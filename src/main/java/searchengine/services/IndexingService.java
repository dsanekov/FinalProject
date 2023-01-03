package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.DBConnection;
import searchengine.model.Page;
import searchengine.model.PageLinksExtractor;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;


    public void startIndexing() throws SQLException {
        DBConnection.deleteInfoAboutSites();
        String[] result = { "true", "false" };
        String error = "Данная страница находится за пределами сайтов,указанных в конфигурационном файле";
        Set<String> allPages = new TreeSet<>();//TODO здесь будут все страницы со всех сайтов, указаных в конфиге .yaml. НО!!!! надо хранить этот сэт не здесь!!!! убрать! Оставить только старт
        List<Site> sitesList = sites.getSites();
        for(Site site : sitesList){
            new Thread(()->{
                Page firstPage = new Page(site.getUrl());
                PageLinksExtractor extractor = new PageLinksExtractor(firstPage);
                Set<String> siteSet = new ForkJoinPool().invoke(extractor);
                allPages.addAll(siteSet);
            }).start();
        }
    }

    public void stopIndexing(){
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }
}
