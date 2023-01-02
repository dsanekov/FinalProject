package searchengine.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static Connection connection;
    private static String dbName = "search_engine";
    private static String dbUser = "root";
    private static String dbPass = "galaxyfit";
    private static StringBuilder insertQuery = new StringBuilder();
    private static StringBuilder deleteQuery = new StringBuilder();

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
        //todo поправить String sql ниже.
        String sql = "INSERT INTO voter_count(name, birthDate, `count`) " +
                "VALUES " + insertQuery.toString() +
                "ON DUPLICATE KEY UPDATE `count` = `count` + 1";
        DBConnection.getConnection().createStatement().execute(sql);
        DBConnection.clearInsertQuery();
    }

    public static void executeMultiDelete() throws SQLException{
        String sql = "";
        DBConnection.getConnection().createStatement().execute(sql);
        DBConnection.clearDeleteQuery();
    }
    public static void clearInsertQuery(){
        insertQuery = new StringBuilder();
    }

    public static void clearDeleteQuery(){
        deleteQuery = new StringBuilder();
    }

    public static StringBuilder getInsertQuery(){
        return insertQuery;
    }

    public static StringBuilder getDeleteQuery(){
        return deleteQuery;
    }


}
