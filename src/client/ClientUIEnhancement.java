package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.DriverManager;

public class ClientUIEnhancement {

    private static ClientUIEnhancement instance;
    private Timer keepAliveTimer;
    private boolean isActive = false;

    public static ClientUIEnhancement getInstance() {
        if (instance == null) {
            instance = new ClientUIEnhancement();
        }
        return instance;
    }

    public void initializeKeepAlive(JFrame parentFrame) {
        if (isActive) return;

        isActive = true;
        System.out.println("Initializing ClientUI enhancements...");

        keepAliveTimer = new Timer(300000, e -> {
            SwingUtilities.invokeLater(() -> {
                try {

                    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                        conn.createStatement().executeQuery("SELECT 1").close();
                        System.out.println("Keep-alive ping successful");
                    }
                } catch (Exception ex) {
                    System.err.println("Keep-alive ping failed: " + ex.getMessage());
                }
            });
        });
        keepAliveTimer.start();

        Timer cleanupTimer = new Timer(600000, e -> {
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            System.out.println(String.format("Memory: %dMB used, %dMB free",
                             totalMemory - freeMemory, freeMemory));
        });
        cleanupTimer.start();

        parentFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        System.out.println("ClientUI enhancements initialized successfully");
    }

    public static void executeWithLoading(JFrame parent, String message, Runnable task) {

        JDialog loadingDialog = new JDialog(parent, "Đang xử lý...", true);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel("⏳", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);

        panel.add(iconLabel, BorderLayout.NORTH);
        panel.add(messageLabel, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);

        loadingDialog.setContentPane(panel);
        loadingDialog.setSize(300, 120);
        loadingDialog.setLocationRelativeTo(parent);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                loadingDialog.setVisible(false);
                loadingDialog.dispose();
            }
        };

        SwingUtilities.invokeLater(() -> {
            worker.execute();
            loadingDialog.setVisible(true);
        });
    }

    public void cleanup() {
        if (!isActive) return;

        System.out.println("Cleaning up ClientUI enhancements...");

        if (keepAliveTimer != null && keepAliveTimer.isRunning()) {
            keepAliveTimer.stop();
        }

        isActive = false;
        System.out.println("ClientUI enhancements cleanup completed");
    }

    public boolean isActive() {
        return isActive;
    }

    public String getStatus() {
        if (!isActive) {
            return "ClientUI enhancements are inactive";
        }

        boolean timerActive = keepAliveTimer != null && keepAliveTimer.isRunning();
        return String.format("ClientUI enhancements are %s",
                           timerActive ? "running" : "stopped");
    }
}
