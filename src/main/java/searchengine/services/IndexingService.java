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
        Set<String> allPages = new TreeSet<>();//TODO здесь будут все страницы со всех сайтов, указаных в конфиге .yaml. НО!!!! надо хранить этот сэт не здесь!!!! убрать! Оставить только старт
        List<Site> sitesList = sites.getSites();
        for(Site site : sitesList){
            new Thread(()->{
                searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, LocalDateTime.now(),"NULL",site.getUrl(),site.getName());
                DBConnection.executeInsertSiteIndexing(newSite);
                int code = 0; //todo получать при запросе со страницы
                String content = ""; //todo получать при запросе со страницы. контент страницы (HTML-код)
                Page firstPage = new Page(newSite,site.getUrl(),code,content);//todo пофиксить констурктор
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
