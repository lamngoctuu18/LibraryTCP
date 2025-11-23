package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple database connection pool implementation
 */
public class DatabasePool {
    private static final String DB_URL = "jdbc:sqlite:C:/data/library.db";
    private static final int INITIAL_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 20;
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    
    private static DatabasePool instance;
    private final BlockingQueue<Connection> availableConnections;
    private final BlockingQueue<Connection> usedConnections;
    private volatile boolean isShutdown = false;
    
    private DatabasePool() {
        availableConnections = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
        usedConnections = new ArrayBlockingQueue<>(MAX_POOL_SIZE);
        initializePool();
    }
    
    public static synchronized DatabasePool getInstance() {
        if (instance == null) {
            instance = new DatabasePool();
        }
        return instance;
    }
    
    private void initializePool() {
        try {
            Class.forName("org.sqlite.JDBC");
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                Connection conn = DriverManager.getConnection(DB_URL);
                availableConnections.offer(conn);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (isShutdown) {
            throw new SQLException("Connection pool is shutdown");
        }
        
        try {
            Connection conn = availableConnections.poll(CONNECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (conn == null) {
                // Try to create new connection if pool not at max capacity
                if (usedConnections.size() + availableConnections.size() < MAX_POOL_SIZE) {
                    conn = DriverManager.getConnection(DB_URL);
                } else {
                    throw new SQLException("Unable to obtain connection from pool within timeout");
                }
            }
            
            // Validate connection
            if (conn.isClosed()) {
                conn = DriverManager.getConnection(DB_URL);
            }
            
            usedConnections.offer(conn);
            return new PooledConnection(conn, this);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        }
    }
    
    public void returnConnection(Connection connection) {
        if (connection == null || isShutdown) return;
        
        try {
            if (usedConnections.remove(connection)) {
                if (!connection.isClosed()) {
                    connection.setAutoCommit(true); // Reset to default
                    availableConnections.offer(connection);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error returning connection to pool: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        isShutdown = true;
        
        // Close all available connections
        Connection conn;
        while ((conn = availableConnections.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
        
        // Close all used connections
        while ((conn = usedConnections.poll()) != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    public int getAvailableConnections() {
        return availableConnections.size();
    }
    
    public int getUsedConnections() {
        return usedConnections.size();
    }
}