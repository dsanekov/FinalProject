package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.PageLinksExtractor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;


@Service
@RequiredArgsConstructor
public class IndexingService {
    private final SitesList sites;


    public void startIndexing(){
        deleteInfoAboutSites();
        String[] result = { "true", "false" };
        String error = "Данная страница находится за пределами сайтов,указанных в конфигурационном файле";
        Set<String> allPages = new TreeSet<>();//TODO здесь все страницы со всех сайтов, указаных в конфиге .yaml. НО!!!! надо хранить этот сэт не здесь!!!! убрать! Оставить только старт
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

    public void deleteInfoAboutSites () {
        List<Integer> idList = getSitesIdForDeletion();
        String deleteQuery = "";

        for(Integer id : idList) {
            deleteQuery += "DELETE FROM `site` WHERE 'id' = " + id + "DELETE FROM `page` WHERE 'site_id' = " + id;
        }
        System.out.println("Удаление из базы - " + statement.execute(deleteQuery)); //todo сделать класс подключения к базе!
    }

    public List<Integer> getSitesIdForDeletion(){
        //todo перенести код про базу в класс DBConnections.
        List<Integer> idList = new ArrayList<>();
        String selectQuery = "";

        for(Site site : sites.getSites()) {
            selectQuery += "SELECT 'id' FROM 'site' WHERE 'url' = " + site.getUrl() + "\n";
        }
        String url = "jdbc:mysql://localhost:3306/search_engine";
        String user = "root";
        String pass = "galaxyfit";

        try {
            Connection connection = DriverManager.getConnection(url,user,pass);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(selectQuery);
            while (resultSet.next()){
                int id = resultSet.getInt("id");
                idList.add(id);
                System.out.println(id);
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return idList;
    }
    public void stopIndexing(){
        String[] result = { "true", "false" };
        String error = "Индексация не запущена";
    }
}
