package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.DBConnection;
import searchengine.model.Page;
import searchengine.model.PageLinksExtractor;
import searchengine.model.SiteStatus;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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


    public void startIndexing() throws SQLException {
        String[] result = { "true", "false" };
        String error = "Данная страница находится за пределами сайтов,указанных в конфигурационном файле";
        Set<String> allPages = new TreeSet<>();
        List<Site> sitesList = sites.getSites();
        for(Site site : sitesList){
            new Thread(()->{
                LocalDateTime statusTime = LocalDateTime.now();
                searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, statusTime,"NULL",site.getUrl(),site.getName());
                try {
                    deleteOldData(newSite);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                saveSite(newSite);
                int code = 0; //todo получать при запросе со страницы
                String content = ""; //todo получать при запросе со страницы. контент страницы (HTML-код)
                Page firstPage = new Page(newSite,site.getUrl(),code,content);
                pageRepository.save(firstPage);
                PageLinksExtractor extractor = new PageLinksExtractor(firstPage,newSite,pageRepository);
                Set<String> siteSet = new ForkJoinPool().invoke(extractor);
                allPages.addAll(siteSet);
            }).start();
        }
        //todo после индексации поменять у Site статус и время.
    }

    public void stopIndexing() throws SQLException{
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }
    private void deleteOldData(searchengine.model.Site site) throws SQLException{
        deleteSitesData(site);
    }
    private void deleteSitesData(searchengine.model.Site site) throws SQLException{
        List<Integer> idList = DBConnection.getSitesIdForDeletion(sites.getSites());
        for(Integer id : idList){
            siteRepository.deleteById(id);
        }
    }
    private void saveSite(searchengine.model.Site site){
        siteRepository.save(site);
    }
}
