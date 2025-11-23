package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibraryServer {
    private static final int PORT = ConfigManager.getInt("server.port");
    private static final int REST_API_PORT = ConfigManager.getInt("server.rest.api.port");
    private static final int THREAD_POOL_SIZE = ConfigManager.getInt("server.thread.pool.size");
    private static RestApiHandler restApiHandler;

    /**
     * Check if port is available
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Find available port starting from given port
     */
    private static int findAvailablePort(int startPort) {
        for (int port = startPort; port <= startPort + 100; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new RuntimeException("No available ports found in range " + startPort + "-" + (startPort + 100));
    }

    public static void main(String[] args) {
        // Validate configuration
        if (!ConfigManager.validateConfig()) {
            System.err.println("[ERROR] Invalid configuration detected. Please check server.properties");
            System.exit(1);
        }

        ConfigManager.printConfig();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Initialize backup manager if enabled
        if (ConfigManager.getBoolean("backup.enabled")) {
            BackupManager.initialize();
        }

        // Initialize REST API if enabled
        if (ConfigManager.getBoolean("rest.api.enabled")) {
            try {
                // Check if REST API port is available
                int actualRestPort = REST_API_PORT;
                if (!isPortAvailable(actualRestPort)) {
                    actualRestPort = findAvailablePort(actualRestPort);
                    System.out.println("[INFO] REST API port " + REST_API_PORT + " occupied, using " + actualRestPort);
                }

                EnhancedBookDAO bookDAO = new EnhancedBookDAO();
                EnhancedBorrowDAO borrowDAO = new EnhancedBorrowDAO();
                EnhancedUserDAO userDAO = new EnhancedUserDAO();
                SessionManager sessionManager = new SessionManager();
                RateLimiter rateLimiter = RateLimiter.getInstance();

                restApiHandler = new RestApiHandler(actualRestPort, bookDAO, borrowDAO,
                        userDAO, sessionManager, rateLimiter);
                restApiHandler.start();
                System.out.println("[INFO] REST API started on port " + actualRestPort);
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to start REST API: " + e.getMessage());
            }
        }

        // Check if main port is available
        int actualPort = PORT;
        if (!isPortAvailable(actualPort)) {
            actualPort = findAvailablePort(actualPort);
            System.out.println("[INFO] Main server port " + PORT + " occupied, using " + actualPort);
        }

        try (ServerSocket serverSocket = new ServerSocket(actualPort)) {
            System.out.println(
                    "LibraryServer started on port " + actualPort + " with thread pool size: " + THREAD_POOL_SIZE);
            System.out.println("[INFO] Configuration management enabled");
            System.out.println("[INFO] AI recommendation system enabled");
            System.out.println("[INFO] Multi-language support enabled (en/vi/zh/ja/ko)");
            if (ConfigManager.getBoolean("backup.enabled")) {
                System.out.println("[INFO] Backup manager initialized");
            }
            if (ConfigManager.getBoolean("metrics.enabled")) {
                System.out.println("[INFO] Metrics collection enabled");
            }
            if (ConfigManager.getBoolean("rest.api.enabled")) {
                System.out.println("[INFO] REST API enabled on port " + REST_API_PORT);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                executor.shutdown();
                DatabasePool.getInstance().shutdown();
                SessionManager.shutdown();
                if (ConfigManager.getBoolean("backup.enabled")) {
                    BackupManager.shutdown();
                }
                if (ConfigManager.getBoolean("metrics.enabled")) {
                    System.out.println("[METRICS] Final report:\n" + MetricsCollector.getMetricsReport());
                }
                if (restApiHandler != null) {
                    restApiHandler.stop();
                }
            }));

            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("Client connected: " + client.getRemoteSocketAddress());
                    MetricsCollector.connectionOpened();
                    executor.submit(new ClientHandler(client));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                        MetricsCollector.recordError("CONNECTION_ERROR");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
            DatabasePool.getInstance().shutdown();
            SessionManager.shutdown();
            BackupManager.shutdown();
        }
    }
}
