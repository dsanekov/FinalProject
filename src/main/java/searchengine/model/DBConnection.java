package searchengine.model;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DBConnection {
    private static Connection connection;
    private static String dbName = "search_engine";
    private static String dbUser = "root";
    private static String dbPass = "galaxyfit";
    private static StringBuilder insertQuery = new StringBuilder();
    private static StringBuilder deleteQuery = new StringBuilder();
    private static StringBuilder selectQuery = new StringBuilder();
    private static SitesList sites;

    public static Connection getConnection() {
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

    public static void executeMultiInsert() throws SQLException{
        for(Site site : sites.getSites()){
            boolean isStart = insertQuery.length() == 0;
            insertQuery.append((isStart ? "" :",") + "('" + site.getName() + "', '" + INDEXING + site.getUrl());//todo не верно написан sql запрос
        }
        String query = "INSERT INTO 'site'(name, status, url) " +
                "VALUES " + insertQuery.toString();
        DBConnection.getConnection().createStatement().execute(query);
        DBConnection.clearInsertQuery();
        clearInsertQuery();
    }

    public static List<Integer> getSitesIdForDeletion() throws SQLException{
        List<Integer> idList = new ArrayList<>();
        for(Site site : sites.getSites()) {
            selectQuery.append("SELECT 'id' FROM 'site' WHERE 'url' = " + site.getUrl() + "\n");
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
            deleteQuery.append("DELETE FROM `site` WHERE 'id' = " + id + "DELETE FROM `page` WHERE 'site_id' = " + id);
        }
        DBConnection.getConnection().createStatement().execute(deleteQuery.toString());
        DBConnection.clearDeleteQuery();
        clearDeleteQuery();
    }

    public static void clearInsertQuery(){
        insertQuery = new StringBuilder();
    }
    public static void clearDeleteQuery(){
        deleteQuery = new StringBuilder();
    }
    public static void clearSelectQuery(){
        selectQuery = new StringBuilder();
    }



}
