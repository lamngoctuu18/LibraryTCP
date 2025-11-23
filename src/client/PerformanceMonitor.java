package client;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PerformanceMonitor {
    private static volatile PerformanceMonitor instance;
    private final ScheduledExecutorService scheduler;
    private final MemoryMXBean memoryBean;
    private long lastGCRun = 0;
    private boolean autoOptimize = true;

    private int dbConnections = 0;
    private long totalLoadingTime = 0;
    private int loadingOperations = 0;

    private PerformanceMonitor() {
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "PerformanceMonitor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        startMonitoring();
    }

    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor();
                }
            }
        }
        return instance;
    }

    private void startMonitoring() {

        scheduler.scheduleWithFixedDelay(this::checkMemoryUsage, 30, 30, TimeUnit.SECONDS);

        scheduler.scheduleWithFixedDelay(this::reportPerformance, 300, 300, TimeUnit.SECONDS);
    }

    private void checkMemoryUsage() {
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long used = heapUsage.getUsed();
            long max = heapUsage.getMax();

            double usagePercent = (double) used / max * 100;

            System.out.println("🔍 Memory Usage: " + String.format("%.1f%%", usagePercent) +
                             " (" + formatBytes(used) + "/" + formatBytes(max) + ")");

            if (autoOptimize && usagePercent > 80 &&
                System.currentTimeMillis() - lastGCRun > 60000) {

                System.out.println("🧹 Auto GC triggered - High memory usage");
                System.gc();
                lastGCRun = System.currentTimeMillis();

                SwingUtilities.invokeLater(() -> {

                    LazyLoadingManager.CacheManager.getInstance().clear();
                    PerformanceCacheManager.getInstance().clear();
                });
            }

        } catch (Exception e) {
            System.err.println("❌ Error checking memory: " + e.getMessage());
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public LoadingTracker trackLoading(String operation) {
        return new LoadingTracker(operation);
    }

    public void incrementDbConnection() {
        dbConnections++;
    }

    public void decrementDbConnection() {
        dbConnections--;
    }

    private void reportPerformance() {
        try {
            System.out.println("\n📊 === PERFORMANCE REPORT ===");
            System.out.println("🔗 Active DB Connections: " + dbConnections);
            System.out.println("⏱️  Total Loading Operations: " + loadingOperations);

            if (loadingOperations > 0) {
                double avgTime = (double) totalLoadingTime / loadingOperations;
                System.out.println("⚡ Average Loading Time: " + String.format("%.2f ms", avgTime));
            }

            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            System.out.println("💾 Heap Memory: " + formatBytes(heapUsage.getUsed()) +
                             "/" + formatBytes(heapUsage.getMax()));

            if (EventQueue.isDispatchThread()) {
                System.out.println("🎯 Running on EDT - Performance optimal");
            }

            System.out.println("================================\n");

        } catch (Exception e) {
            System.err.println("❌ Error generating performance report: " + e.getMessage());
        }
    }

    public void optimizeUI(JComponent component) {
        SwingUtilities.invokeLater(() -> {
            try {

                if (component instanceof JPanel) {
                    JPanel panel = (JPanel) component;
                    panel.setDoubleBuffered(true);
                    panel.setOpaque(true);
                }

                setupSmoothScrolling(component);

            } catch (Exception e) {
                System.err.println("❌ Error optimizing UI: " + e.getMessage());
            }
        });
    }

    private void setupSmoothScrolling(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) comp;
                scroll.getVerticalScrollBar().setUnitIncrement(16);
                scroll.getHorizontalScrollBar().setUnitIncrement(16);
                scroll.setWheelScrollingEnabled(true);
            }
            if (comp instanceof Container) {
                setupSmoothScrolling((Container) comp);
            }
        }
    }

    public void forceCleanup() {
        System.out.println("🧹 Force cleanup triggered");
        System.gc();
        lastGCRun = System.currentTimeMillis();

        SwingUtilities.invokeLater(() -> {
            LazyLoadingManager.CacheManager.getInstance().clear();
            PerformanceCacheManager.getInstance().clear();
        });
    }

    public void shutdown() {
        reportPerformance();
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public class LoadingTracker implements AutoCloseable {
        private final String operation;
        private final long startTime;

        public LoadingTracker(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
            System.out.println("⏳ Started: " + operation);
        }

        @Override
        public void close() {
            long duration = System.currentTimeMillis() - startTime;
            totalLoadingTime += duration;
            loadingOperations++;

            System.out.println("✅ Completed: " + operation + " (" + duration + "ms)");

            if (duration > 2000) {
                System.out.println("⚠️  Slow operation detected: " + operation + " took " + duration + "ms");
            }
        }
    }

    public void setAutoOptimize(boolean enabled) {
        this.autoOptimize = enabled;
        System.out.println("🔧 Auto optimization: " + (enabled ? "ENABLED" : "DISABLED"));
    }

    private static class PerformanceCacheManager {
        private static final java.util.Map<String, Object> cache =
            new java.util.concurrent.ConcurrentHashMap<>();
        private static final PerformanceCacheManager instance = new PerformanceCacheManager();

        public static PerformanceCacheManager getInstance() {
            return instance;
        }

        public void put(String key, Object value) {
            cache.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            Object value = cache.get(key);
            return type.isInstance(value) ? (T) value : null;
        }

        public void clear() {
            cache.clear();
            System.out.println("🧹 Performance cache cleared");
        }

        public int size() {
            return cache.size();
        }
    }
}
