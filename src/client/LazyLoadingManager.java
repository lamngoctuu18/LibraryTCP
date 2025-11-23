package client;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class LazyLoadingManager {
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "LazyLoad-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    private static volatile LazyLoadingManager instance;

    private LazyLoadingManager() {}

    public static LazyLoadingManager getInstance() {
        if (instance == null) {
            synchronized (LazyLoadingManager.class) {
                if (instance == null) {
                    instance = new LazyLoadingManager();
                }
            }
        }
        return instance;
    }

    public <T> CompletableFuture<T> loadAsync(Supplier<T> dataLoader,
                                             SwingWorker.StateValue onSuccess) {
        return CompletableFuture.supplyAsync(dataLoader, executor)
            .whenComplete((result, throwable) -> {
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (throwable == null && onSuccess != null) {

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
    }

    public void setupTableLazyLoading(JTable table, Supplier<Object[][]> dataLoader,
                                     int pageSize) {
        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                loadTableDataAsync(table, dataLoader, 0, pageSize);
            }
        });

        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(
            JScrollPane.class, table);
        if (scrollPane != null) {
            scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e) {
                    JScrollBar scrollBar = (JScrollBar) e.getAdjustable();
                    int extent = scrollBar.getModel().getExtent();
                    int maximum = scrollBar.getModel().getMaximum();
                    int value = scrollBar.getModel().getValue();

                    if (value + extent >= maximum * 0.9) {
                        int currentRows = table.getRowCount();
                        loadTableDataAsync(table, dataLoader, currentRows, pageSize);
                    }
                }
            });
        }
    }

    private void loadTableDataAsync(JTable table, Supplier<Object[][]> dataLoader,
                                   int offset, int limit) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return dataLoader.get();
            } catch (Exception e) {
                e.printStackTrace();
                return new Object[0][0];
            }
        }, executor).thenAccept(data -> {
            SwingUtilities.invokeLater(() -> {
                if (data != null && data.length > 0) {
                    javax.swing.table.DefaultTableModel model =
                        (javax.swing.table.DefaultTableModel) table.getModel();

                    for (Object[] row : data) {
                        model.addRow(row);
                    }
                }
            });
        });
    }

    public <T> void preCache(String key, Supplier<T> dataLoader) {
        CompletableFuture.supplyAsync(dataLoader, executor)
            .thenAccept(data -> {

                CacheManager.getInstance().put(key, data);
            });
    }

    public void debounceSearch(String query, long delayMs, Runnable searchAction) {
        Timer timer = new Timer((int) delayMs, e -> searchAction.run());
        timer.setRepeats(false);
        timer.start();
    }

    public void setupVisibilityLazyLoading(JComponent component, Runnable loadAction) {
        component.addComponentListener(new ComponentAdapter() {
            private boolean loaded = false;

            @Override
            public void componentShown(ComponentEvent e) {
                if (!loaded) {
                    loaded = true;
                    CompletableFuture.runAsync(loadAction, executor);
                }
            }
        });
    }

    public void shutdown() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    public static class CacheManager {
        private static final java.util.Map<String, Object> cache =
            new java.util.concurrent.ConcurrentHashMap<>();
        private static final CacheManager instance = new CacheManager();

        public static CacheManager getInstance() {
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
            System.out.println("🧹 LazyLoading cache cleared");
        }

        public int size() {
            return cache.size();
        }
    }
}
