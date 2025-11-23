package client;

import javax.swing.*;
import java.awt.*;

public class AdminLauncher {
    private static JWindow loadingWindow;
    private static DarkModeManager darkModeManager;
    private static JProgressBar progressBar;
    private static JLabel statusLabel;

    public static void main(String[] args) {

        darkModeManager = DarkModeManager.getInstance();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Cannot set system look and feel: " + e.getMessage());
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            showLoadingDialog();

            SwingWorker<AdminUI, Integer> worker = new SwingWorker<AdminUI, Integer>() {
                @Override
                protected AdminUI doInBackground() throws Exception {

                    publish(10);
                    Thread.sleep(200);

                    publish(30);
                    System.out.println("Initializing AdminUI...");
                    AdminUI adminUI = new AdminUI();
                    Thread.sleep(300);

                    publish(70);
                    Thread.sleep(200);

                    publish(100);
                    Thread.sleep(100);

                    return adminUI;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    if (!chunks.isEmpty()) {
                        updateLoadingProgress(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    try {
                        AdminUI adminUI = get();
                        hideLoadingDialog();
                        adminUI.setVisible(true);
                        System.out.println("AdminUI started successfully with Dark Mode support!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        hideLoadingDialog();
                        JOptionPane.showMessageDialog(null,
                            "Failed to start Admin UI: " + e.getMessage(),
                            "Startup Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            worker.execute();
        });
    }

    private static void showLoadingDialog() {
        loadingWindow = new JWindow();
        loadingWindow.setSize(450, 220);
        loadingWindow.setLocationRelativeTo(null);

        JPanel loadingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient;
                if (darkModeManager.isDarkMode()) {
                    gradient = new GradientPaint(0, 0, DarkModeManager.DarkTheme.SECONDARY_BG,
                                               0, getHeight(), DarkModeManager.DarkTheme.TERTIARY_BG);
                } else {
                    gradient = new GradientPaint(0, 0, new Color(52, 152, 219),
                                               0, getHeight(), new Color(41, 128, 185));
                }
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRect(0, getHeight()-3, getWidth(), 3);
            }
        };
        loadingPanel.setLayout(new BorderLayout());
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel("Hệ thống quản lý thư viện", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(darkModeManager.isDarkMode() ?
            DarkModeManager.DarkTheme.PRIMARY_TEXT : Color.WHITE);

        JLabel subtitleLabel = new JLabel("Giao diện quản trị với Dark Mode", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(darkModeManager.isDarkMode() ?
            DarkModeManager.DarkTheme.SECONDARY_TEXT : new Color(220, 230, 240));

        statusLabel = new JLabel("Đang khởi tạo...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        statusLabel.setForeground(darkModeManager.isDarkMode() ?
            DarkModeManager.DarkTheme.SECONDARY_TEXT : new Color(240, 248, 255));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        if (darkModeManager.isDarkMode()) {
            progressBar.setBackground(new Color(73, 80, 87));
            progressBar.setForeground(DarkModeManager.DarkTheme.PRIMARY_ACCENT);
        } else {
            progressBar.setBackground(new Color(255, 255, 255, 80));
            progressBar.setForeground(new Color(46, 204, 113));
        }
        progressBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        progressBar.setPreferredSize(new Dimension(320, 28));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 5, 0);
        centerPanel.add(titleLabel, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 20, 0);
        centerPanel.add(subtitleLabel, gbc);

        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 15, 0);
        centerPanel.add(statusLabel, gbc);

        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(progressBar, gbc);

        loadingPanel.add(centerPanel, BorderLayout.CENTER);

        loadingWindow.add(loadingPanel);
        loadingWindow.setVisible(true);
    }

    private static void updateLoadingProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            if (progressBar != null) {
                progressBar.setValue(progress);
                progressBar.setString(progress + "%");
            }

            if (statusLabel != null) {
                if (progress <= 30) {
                    statusLabel.setText("Đang tải resource managers & Dark Mode...");
                } else if (progress <= 70) {
                    statusLabel.setText("Đang khởi tạo giao diện admin...");
                } else if (progress < 100) {
                    statusLabel.setText("Đang hoàn tất thiết lập...");
                } else {
                    statusLabel.setText("Hoàn tất! ✓");
                }
            }
        });
    }

    private static void hideLoadingDialog() {
        SwingUtilities.invokeLater(() -> {
            if (loadingWindow != null) {
                loadingWindow.setVisible(false);
                loadingWindow.dispose();
                loadingWindow = null;
            }
        });
    }
}
