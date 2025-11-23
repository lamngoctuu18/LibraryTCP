package client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:C:/data/library.db";
    private static final int MAX_CONNECTIONS = 5;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int VALIDATION_TIMEOUT = 5;

    private static DatabaseManager instance;
    private BlockingQueue<Connection> connectionPool;
    private volatile boolean isShutdown = false;

    private DatabaseManager() {
        initializeConnectionPool();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeConnectionPool() {
        connectionPool = new LinkedBlockingQueue<>(MAX_CONNECTIONS);

        for (int i = 0; i < MAX_CONNECTIONS; i++) {
            try {
                Connection conn = createNewConnection();
                connectionPool.offer(conn);
            } catch (SQLException e) {
                System.err.println("Failed to create initial connection: " + e.getMessage());
            }
        }

        System.out.println("Database connection pool initialized with " + connectionPool.size() + " connections");
    }

    private Connection createNewConnection() throws SQLException {
        String url = DB_URL + "?busy_timeout=" + CONNECTION_TIMEOUT
                   + "&journal_mode=WAL"
                   + "&synchronous=NORMAL"
                   + "&cache_size=10000"
                   + "&temp_store=memory";

        Connection conn = DriverManager.getConnection(url);

        conn.setAutoCommit(true);

        return conn;
    }

    public Connection getConnection() throws SQLException {
        if (isShutdown) {
            throw new SQLException("Database manager is shutdown");
        }

        Connection conn = null;

        try {

            conn = connectionPool.poll(5, TimeUnit.SECONDS);

            if (conn == null) {

                conn = createNewConnection();
            } else {

                if (!isConnectionValid(conn)) {
                    try {
                        conn.close();
                    } catch (SQLException e) {

                    }
                    conn = createNewConnection();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        }

        return conn;
    }

    public void returnConnection(Connection conn) {
        if (conn == null || isShutdown) {
            return;
        }

        try {
            if (isConnectionValid(conn)) {

                conn.setAutoCommit(true);

                if (!connectionPool.offer(conn)) {

                    conn.close();
                }
            } else {

                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error returning connection to pool: " + e.getMessage());
            try {
                conn.close();
            } catch (SQLException closeError) {

            }
        }
    }

    private boolean isConnectionValid(Connection conn) {
        try {
            return conn != null &&
                   !conn.isClosed() &&
                   conn.isValid(VALIDATION_TIMEOUT);
        } catch (SQLException e) {
            return false;
        }
    }

    public <T> T executeWithConnection(DatabaseOperation<T> operation) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            return operation.execute(conn);
        } finally {
            if (conn != null) {
                returnConnection(conn);
            }
        }
    }

    public void executeWithConnection(DatabaseVoidOperation operation) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            operation.execute(conn);
        } finally {
            if (conn != null) {
                returnConnection(conn);
            }
        }
    }

    public synchronized void shutdown() {
        if (isShutdown) {
            return;
        }

        isShutdown = true;

        Connection conn;
        while ((conn = connectionPool.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection during shutdown: " + e.getMessage());
            }
        }

        System.out.println("Database connection pool shutdown completed");
    }

    public String getPoolStatus() {
        return String.format("Pool Status - Available: %d, Max: %d, Shutdown: %s",
                           connectionPool.size(), MAX_CONNECTIONS, isShutdown);
    }

    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    public interface DatabaseVoidOperation {
        void execute(Connection conn) throws SQLException;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null) {
                instance.shutdown();
            }
        }));
    }
}
