package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.DBConnection;
import searchengine.model.Page;
import searchengine.model.PageLinksExtractor;
import searchengine.model.SiteStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;


    public void startIndexing() throws SQLException {
        DBConnection.deleteInfoAboutSitesAndPages(sites.getSites());
        String[] result = { "true", "false" };
        String error = "Данная страница находится за пределами сайтов,указанных в конфигурационном файле";
        Set<String> allPages = new TreeSet<>();
        List<Site> sitesList = sites.getSites();
        for(Site site : sitesList){
            new Thread(()->{
                LocalDateTime statusTime = LocalDateTime.now();//todo сделать формат времени "2022-09-25 10:15:34"
                searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",site.getUrl(),site.getName());
                DBConnection.executeInsertSiteIndexing(newSite);
                int code = 0; //todo получать при запросе со страницы
                String content = ""; //todo получать при запросе со страницы. контент страницы (HTML-код)
                Page firstPage = new Page(newSite,site.getUrl(),code,content);
                try {
                    DBConnection.insertInfoAboutPage(firstPage);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                PageLinksExtractor extractor = new PageLinksExtractor(firstPage,newSite);
                Set<String> siteSet = new ForkJoinPool().invoke(extractor);
                allPages.addAll(siteSet);
            }).start();
        }
    }

    public void stopIndexing() throws SQLException{
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }
}
