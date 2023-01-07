package searchengine.model;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DBConnection {
    private static Connection connection;
    private static final String dbName = "search_engine";
    private static final String dbUser = "root";
    private static final String dbPass = "galaxyfit";
    private static StringBuffer deleteQuery = new StringBuffer();
    private static StringBuffer selectQuery = new StringBuffer();
    private static SitesList sites;
    private static StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure("hibernate.cfg.xml").build();
    private static Metadata metadata = new MetadataSources(registry).getMetadataBuilder().build();
    private static SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();

    private static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?user=" + dbUser + "&password=" + dbPass);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
    public static boolean thisSiteExists(String path) throws SQLException{
        String query = "EXISTS(SELECT id FROM page WHERE path = " + path + ")";
        ResultSet rs = DBConnection.getConnection().createStatement().executeQuery(query);
        System.out.println("Запрос на наличте страницы с path '" + path + "' в базе - " + rs.next());
        return rs.next();
    }

    public static void executeInsertSitesAreIndexing(){
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        for(Site site : sites.getSites()){
            searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, LocalDateTime.now(),"NULL",site.getUrl(),site.getName());
            session.save(newSite);
        }
        transaction.commit();
        sessionFactory.close();
    }

    public static List<Integer> getSitesIdForDeletion() throws SQLException{
        List<Integer> idList = new ArrayList<>();
        for(Site site : sites.getSites()) {
            boolean isStart = selectQuery.length() == 0;
            selectQuery.append((isStart ? "" :",")+("SELECT id FROM site WHERE url = " + site.getUrl()));
        }
        ResultSet rs = DBConnection.getConnection().createStatement().executeQuery(selectQuery.toString());
        while (rs.next()) {
            System.out.println("Id сайта на удаление - " + rs.getInt("id"));
            idList.add(rs.getInt("id"));
        }
        clearSelectQuery();
        return idList;
    }

    public static void deleteInfoAboutSites () throws SQLException{
        List<Integer> idList = getSitesIdForDeletion();
        for(Integer id : idList) {
            boolean isStart = deleteQuery.length() == 0;
            deleteQuery.append((isStart ? "" :",")+("DELETE FROM site WHERE id = " + id +", "+ "DELETE FROM page WHERE site_id = " + id));
        }
        DBConnection.getConnection().createStatement().execute(deleteQuery.toString());
        DBConnection.clearDeleteQuery();
        clearDeleteQuery();
    }

    public static void writeInfoAboutPagesToDB(Set<String> pages) throws SQLException{
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        for(String page : pages){
            int code = 0; //todo получать при запросе со страницы
            String content = ""; //todo получать при запросе со страницы. контент страницы (HTML-код)
            searchengine.model.Site site = searchengine.model.Site.getSiteById(getSiteId(page));
            Page newPage = new Page(site,page,code,content);
            session.save(newPage);
        }
        transaction.commit();
        sessionFactory.close();
    }
    private static int getSiteId(String page) throws SQLException{
        String query = "SELECT id FROM site WHERE CONTAINS(url, '" + page +"')";
        ResultSet rs = DBConnection.getConnection().createStatement().executeQuery(query);
        return rs.getInt("id");
    }
    private static void createQueryAboutPages(String page){
        //TODO через транзакции делаем

    }
    private static void clearDeleteQuery(){
        deleteQuery = new StringBuffer();
    }
    private static void clearSelectQuery(){
        selectQuery = new StringBuffer();
    }
}
