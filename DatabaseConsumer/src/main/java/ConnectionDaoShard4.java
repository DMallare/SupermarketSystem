import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionDaoShard4 {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;
    private static final String HOST_NAME = System.getProperty("MySQL_IP_ADDRESS4");
    private static final String PORT = System.getProperty("MySQL_PORT");
    private static final String DATABASE = System.getProperty("DATABASE");
    private static final String USERNAME = System.getProperty("DB_USERNAME4");
    private static final String PASSWORD = System.getProperty("DB_PASSWORD4");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC", HOST_NAME, PORT, DATABASE);
        config.setJdbcUrl(url);
        config.setUsername(USERNAME);
        config.setPassword(PASSWORD);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        config.setMaximumPoolSize(20);
        ds = new HikariDataSource(config);
    }

    private ConnectionDaoShard4() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
