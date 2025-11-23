package client;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KeepAliveManager {
    private static final String DB_URL = "jdbc:sqlite:C:/data/library.db?busy_timeout=30000";
    private static KeepAliveManager instance;
    private ScheduledExecutorService scheduler;
    private boolean isActive = false;

    private KeepAliveManager() {
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "KeepAlive");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static synchronized KeepAliveManager getInstance() {
        if (instance == null) {
            instance = new KeepAliveManager();
        }
        return instance;
    }

    public void start() {
        if (isActive) return;

        isActive = true;
        System.out.println("Starting keep-alive system...");

        scheduler.scheduleAtFixedRate(this::pingDatabase, 5, 5, TimeUnit.MINUTES);

        scheduler.scheduleAtFixedRate(this::cleanupMemory, 10, 10, TimeUnit.MINUTES);

        System.out.println("Keep-alive system started successfully");
    }

    public void stop() {
        if (!isActive) return;

        isActive = false;
        System.out.println("Stopping keep-alive system...");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Keep-alive system stopped");
    }

    private void pingDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            conn.createStatement().executeQuery("SELECT 1").close();
            System.out.println("Database ping successful");
        } catch (Exception e) {
            System.err.println("Database ping failed: " + e.getMessage());
        }
    }

    private void cleanupMemory() {
        try {

            System.gc();

            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;

            System.out.println(String.format("Memory: %dMB used, %dMB free, %dMB total",
                             usedMemory, freeMemory, totalMemory));

        } catch (Exception e) {
            System.err.println("Memory cleanup failed: " + e.getMessage());
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public String getStatus() {
        if (!isActive) {
            return "Keep-alive system is stopped";
        }

        boolean schedulerActive = scheduler != null && !scheduler.isShutdown();
        return String.format("Keep-alive system is %s",
                           schedulerActive ? "running" : "inactive");
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null) {
                instance.stop();
            }
        }));
    }
}
