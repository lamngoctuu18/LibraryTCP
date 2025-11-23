package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;

public class ClientUI extends JFrame implements DarkModeManager.DarkModeListener {
    private JTextField txtSearch, txtAuthor, txtPublisher;
    private JComboBox<String> cbCategory;
    private JButton btnSearch, btnBorrow, btnFavorite, btnActivity;
    private JLabel lblUser;
    private JLabel lblAvatar;
    private JButton btnNotification;
    private int userId = -1;

    private int currentPage = 1;
    private int itemsPerPage = 18;
    private int totalItems = 0;
    private int totalPages = 0;
    private JPanel booksGridPanel;
    private JLabel lblPageInfo;
    private JButton btnPrevPage, btnNextPage, btnFirstPage, btnLastPage;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JPopupMenu suggestPopup;

    private DatabaseManager dbManager;
    private BackgroundTaskManager taskManager;
    private ScheduledFuture<?> keepAliveTask;
    private Timer refreshTimer;

    private DarkModeManager darkModeManager;

    private volatile boolean isLoading = false;

    private Timer searchDebounceTimer;
    private Timer filterDebounceTimer;
    private static final int DEBOUNCE_DELAY = 500;

    private static final String[] CATEGORIES = {
        "Tất cả",
        "Văn học – Tiểu thuyết",
        "Khoa học – Công nghệ",
        "Kinh tế – Quản trị",
        "Tâm lý – Kỹ năng sống",
        "Giáo trình – Học thuật",
        "Trẻ em – Thiếu nhi",
        "Lịch sử – Địa lý",
        "Tôn giáo – Triết học",
        "Ngoại ngữ – Từ điển",
        "Nghệ thuật – Âm nhạc"
    };

    public ClientUI() {
        setTitle("Hệ thống quản lý thư viện - Giao diện người dùng");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        setPreferredSize(new Dimension(1900, 1000));

        initializeResourceManagers();

        initializeDarkMode();

        initializeBorrowRequestsTable();

        createMainInterface();

        setDefaultAvatar();

        connectToServer();

        try {
            ClientUIEnhancement enhancement = ClientUIEnhancement.getInstance();
            enhancement.initializeKeepAlive(this);

            setupPeriodicTasks();
            setupShutdownHook();
        } catch (Exception e) {
            System.err.println("Failed to initialize enhancement: " + e.getMessage());
        }

        pack();
        setLocationRelativeTo(null);
    }

    private void initializeResourceManagers() {
        try {
            dbManager = DatabaseManager.getInstance();
            taskManager = BackgroundTaskManager.getInstance();
            System.out.println("Resource managers initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize resource managers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeDarkMode() {
        try {
            darkModeManager = DarkModeManager.getInstance();
            darkModeManager.addDarkModeListener(this);

            applyCurrentTheme();

            System.out.println("✅ Dark Mode support initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize Dark Mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyCurrentTheme() {
        SwingUtilities.invokeLater(() -> {
            if (darkModeManager != null) {

                JPanel mainPanel = (JPanel) getContentPane();
                if (mainPanel != null) {
                    if (darkModeManager.isDarkMode()) {

                        mainPanel.setBackground(DarkModeManager.DarkTheme.PRIMARY_BG);
                    } else {

                        mainPanel.setBackground(DarkModeManager.LightTheme.SECONDARY_BG);
                    }

                    darkModeManager.applyDarkMode(mainPanel);
                }

                if (booksGridPanel != null) {
                    if (darkModeManager.isDarkMode()) {
                        booksGridPanel.setBackground(DarkModeManager.DarkTheme.SECONDARY_BG);
                    } else {
                        booksGridPanel.setBackground(DarkModeManager.LightTheme.PRIMARY_BG);
                    }
                }

                updateSearchButtonColors();

                updateComponentColors(getContentPane());

                String currentTitle = getTitle();
                if (!currentTitle.contains("Dark Mode") && !currentTitle.contains("Light Mode")) {
                    String mode = darkModeManager.isDarkMode() ? " (Dark Mode)" : " (Light Mode)";
                    setTitle(currentTitle + mode);
                } else {

                    String baseTitle = currentTitle.replaceAll(" \\((Dark|Light) Mode\\)", "");
                    String mode = darkModeManager.isDarkMode() ? " (Dark Mode)" : " (Light Mode)";
                    setTitle(baseTitle + mode);
                }

                repaint();
            }
        });
    }

    @Override
    public void onDarkModeChanged(boolean isDarkMode) {
        applyCurrentTheme();
    }

    private void setupPeriodicTasks() {

        refreshTimer = new Timer(300000, e -> {
            if (dbManager != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        dbManager.executeWithConnection(conn -> {

                            conn.createStatement().executeQuery("SELECT 1").close();
                            return null;
                        });
                    } catch (Exception ex) {
                        System.err.println("Keep-alive query failed: " + ex.getMessage());
                    }
                });
            }
        });
        refreshTimer.start();

        Timer notificationTimer = new Timer(120000, e -> {
            if (userId != -1) {
                SwingUtilities.invokeLater(() -> {
                    if (btnNotification != null) {
                        updateNotificationBadge(btnNotification);
                    }
                });
            }
        });
        notificationTimer.start();

        System.out.println("Periodic tasks setup completed");
    }

    private void configureSmoothScrolling(JScrollPane scrollPane) {

        scrollPane.getVerticalScrollBar().setUnitIncrement(8);
        scrollPane.getVerticalScrollBar().setBlockIncrement(24);

        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    return;
                }

                JScrollBar scrollBar = scrollPane.getVerticalScrollBar();

                int scrollAmount = e.getUnitsToScroll() * 6;
                int currentValue = scrollBar.getValue();
                int newValue = Math.max(0, Math.min(
                    currentValue + scrollAmount,
                    scrollBar.getMaximum() - scrollBar.getVisibleAmount()
                ));

                scrollBar.setValue(newValue);
                e.consume();
            }
        });
    }

    private void setupShutdownHook() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
                System.exit(0);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
    }

    private void cleanup() {
        try {
            System.out.println("Starting application cleanup...");

            if (darkModeManager != null) {
                darkModeManager.removeDarkModeListener(this);
            }

            if (refreshTimer != null && refreshTimer.isRunning()) {
                refreshTimer.stop();
            }

            if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
                searchDebounceTimer.stop();
            }

            if (filterDebounceTimer != null && filterDebounceTimer.isRunning()) {
                filterDebounceTimer.stop();
            }

            if (keepAliveTask != null && !keepAliveTask.isCancelled()) {
                keepAliveTask.cancel(true);
            }

            if (socket != null && !socket.isClosed()) {
                try {
                    if (out != null) out.close();
                    if (in != null) in.close();
                    socket.close();
                } catch (Exception e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }

            if (taskManager != null) {
                taskManager.shutdown();
            }

            if (dbManager != null) {
                dbManager.shutdown();
            }

            System.out.println("Application cleanup completed");

        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createMainInterface() {

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    gradient = new GradientPaint(
                        0, 0, new Color(17, 24, 39),
                        0, getHeight(), new Color(31, 41, 55)
                    );
                } else {
                    gradient = new GradientPaint(
                        0, 0, new Color(240, 244, 248),
                        0, getHeight(), new Color(255, 255, 255)
                    );
                }
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setContentPane(mainPanel);

        JPanel topPanel = createModernTopPanel();

        JPanel centerPanel = createCenterPanel();

        JPanel bottomPanel = createEnhancedBottomPanel();

        JPanel contentWrapper = new JPanel(new BorderLayout(0, 15));
        contentWrapper.setOpaque(false);
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        contentWrapper.add(centerPanel, BorderLayout.CENTER);
        contentWrapper.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(contentWrapper, BorderLayout.CENTER);

        addEventListeners();
    }

    private JPanel createModernTopPanel() {

        JPanel headerContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    gradient = new GradientPaint(
                        0, 0, new Color(30, 58, 138),
                        getWidth(), 0, new Color(79, 70, 229)
                    );
                } else {
                    gradient = new GradientPaint(
                        0, 0, new Color(59, 130, 246),
                        getWidth(), 0, new Color(147, 51, 234)
                    );
                }
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
            }
        };
        headerContainer.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        titleSection.setOpaque(false);

        JLabel logoLabel = new JLabel("📚");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JPanel titleTextPanel = new JPanel();
        titleTextPanel.setLayout(new BoxLayout(titleTextPanel, BoxLayout.Y_AXIS));
        titleTextPanel.setOpaque(false);

        JLabel mainTitle = new JLabel("Thư Viện Điện Tử");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainTitle.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Khám phá tri thức - Nâng tầm giá trị");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(255, 255, 255, 180));

        titleTextPanel.add(mainTitle);
        titleTextPanel.add(Box.createVerticalStrut(2));
        titleTextPanel.add(subtitle);

        titleSection.add(logoLabel);
        titleSection.add(titleTextPanel);

        JPanel userSection = createModernUserSection();

        JPanel searchFilterWrapper = new JPanel(new BorderLayout(0, 15));
        searchFilterWrapper.setOpaque(false);
        searchFilterWrapper.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JPanel searchSection = createModernSearchSection();
        JPanel filterSection = createModernFilterSection();

        searchFilterWrapper.add(searchSection, BorderLayout.NORTH);
        searchFilterWrapper.add(filterSection, BorderLayout.CENTER);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(titleSection, BorderLayout.WEST);
        topRow.add(userSection, BorderLayout.EAST);

        headerContainer.add(topRow, BorderLayout.NORTH);
        headerContainer.add(searchFilterWrapper, BorderLayout.CENTER);

        return headerContainer;
    }

    private JPanel createModernUserSection() {
        JPanel userSection = new JPanel();
        userSection.setLayout(new BoxLayout(userSection, BoxLayout.X_AXIS));
        userSection.setOpaque(false);

        JPanel profileCard = createModernProfileCard();

        JButton btnTheme = createModernThemeButton();

        JButton btnNotif = createModernNotificationButton();

        userSection.add(profileCard);
        userSection.add(Box.createRigidArea(new Dimension(10, 0)));
        userSection.add(btnTheme);
        userSection.add(Box.createRigidArea(new Dimension(10, 0)));
        userSection.add(btnNotif);

        return userSection;
    }

    private JPanel createModernProfileCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

                g2d.dispose();
            }
        };

        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        card.setPreferredSize(new Dimension(220, 50));
        card.setMaximumSize(new Dimension(280, 50));
        card.setMinimumSize(new Dimension(200, 50));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel avatarContainer = new JPanel(new BorderLayout());
        avatarContainer.setOpaque(false);

        lblAvatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(88, 166, 255));
                g2d.fillOval(0, 0, 36, 36);

                super.paintComponent(g);
                g2d.dispose();
            }
        };
        lblAvatar.setPreferredSize(new Dimension(36, 36));
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatar.setVerticalAlignment(SwingConstants.CENTER);
        avatarContainer.add(lblAvatar, BorderLayout.CENTER);

        lblUser = new JLabel("Chưa đăng nhập");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUser.setForeground(Color.WHITE);

        card.add(avatarContainer, BorderLayout.WEST);
        card.add(lblUser, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserProfile();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.repaint();
            }
        });

        return card;
    }

    private JButton createModernThemeButton() {
        boolean isDark = (darkModeManager != null && darkModeManager.isDarkMode());
        String text = isDark ? "Light" : "Dark";

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(255, 255, 255, 120));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 255, 255, 100));
                } else {
                    g2d.setColor(new Color(255, 255, 255, 70));
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(90, 50));
        btn.setMaximumSize(new Dimension(90, 50));
        btn.setMinimumSize(new Dimension(90, 50));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Chuyển đổi giao diện");

        btn.addActionListener(e -> {
            if (darkModeManager != null) {
                darkModeManager.toggleDarkMode();
                boolean newIsDark = darkModeManager.isDarkMode();
                btn.setText(newIsDark ? "Light" : "Dark");
                btn.repaint();
            }
        });

        return btn;
    }

    private JButton createModernNotificationButton() {

        if (btnNotification != null) {
            for (ActionListener al : btnNotification.getActionListeners()) {
                btnNotification.removeActionListener(al);
            }
            for (MouseListener ml : btnNotification.getMouseListeners()) {
                btnNotification.removeMouseListener(ml);
            }
        }

        btnNotification = new JButton("Thông báo") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(255, 107, 107, 200));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 107, 107, 180));
                } else {
                    g2d.setColor(new Color(255, 107, 107, 160));
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);

                g2d.dispose();
                super.paintComponent(g);
            }
        };

        btnNotification.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNotification.setForeground(Color.WHITE);
        btnNotification.setPreferredSize(new Dimension(110, 50));
        btnNotification.setMaximumSize(new Dimension(110, 50));
        btnNotification.setMinimumSize(new Dimension(110, 50));
        btnNotification.setFocusPainted(false);
        btnNotification.setBorderPainted(false);
        btnNotification.setContentAreaFilled(false);
        btnNotification.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNotification.setToolTipText("Xem thông báo của bạn");

        btnNotification.addActionListener(e -> showNotifications());

        return btnNotification;
    }

    @Deprecated
    private JPanel createTopPanel_DEPRECATED() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel searchSection = createModernSearchSection();

        JPanel filterSection = createModernFilterSection();

        JPanel userSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        userSection.setOpaque(false);

        JPanel userProfilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        userProfilePanel.setOpaque(true);
        userProfilePanel.setBackground(Color.WHITE);
        userProfilePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        userProfilePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        lblAvatar = new JLabel();
        lblAvatar.setPreferredSize(new Dimension(28, 28));
        lblAvatar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblAvatar.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 100), 1));
        lblAvatar.setOpaque(false);

        lblUser = new JLabel("Chưa đăng nhập");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(new Color(0, 123, 255));
        lblUser.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblUser.setOpaque(false);

        userProfilePanel.add(lblAvatar);
        userProfilePanel.add(lblUser);

        userProfilePanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showUserProfile();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                userProfilePanel.setBackground(new Color(0, 123, 255, 20));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                userProfilePanel.setBackground(Color.WHITE);
            }
        });

        JButton btnDarkMode = darkModeManager.createDarkModeToggleButton();

        btnNotification = new JButton("Thông báo");
        btnNotification.setPreferredSize(new Dimension(120, 40));
        btnNotification.setBackground(new Color(255, 140, 0));
        btnNotification.setForeground(Color.WHITE);
        btnNotification.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnNotification.setFocusPainted(false);
        btnNotification.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btnNotification.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNotification.setToolTipText("Xem thông báo của bạn");
        btnNotification.addActionListener(e -> showNotifications());

        btnNotification.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Color currentBg = btnNotification.getBackground();
                if (currentBg.equals(new Color(255, 140, 0))) {

                    btnNotification.setBackground(new Color(230, 120, 0));
                } else if (currentBg.equals(new Color(231, 76, 60))) {

                    btnNotification.setBackground(new Color(200, 60, 45));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {

                if (userId != -1) {
                    int unreadCount = NotificationUI.getUnreadNotificationCount(userId);
                    if (unreadCount > 0) {
                        btnNotification.setBackground(new Color(231, 76, 60));
                    } else {
                        btnNotification.setBackground(new Color(255, 140, 0));
                    }
                }
            }
        });

        updateNotificationBadge(btnNotification);

        userSection.add(userProfilePanel);
        userSection.add(btnDarkMode);
        userSection.add(btnNotification);

        JPanel searchFilterPanel = new JPanel(new BorderLayout(0, 15));
        searchFilterPanel.setOpaque(false);
        searchFilterPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        searchFilterPanel.add(searchSection, BorderLayout.NORTH);
        searchFilterPanel.add(filterSection, BorderLayout.CENTER);

        topPanel.add(searchFilterPanel, BorderLayout.CENTER);
        topPanel.add(userSection, BorderLayout.EAST);

        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);

        booksGridPanel = new JPanel(new GridLayout(0, 6, 15, 15));
        booksGridPanel.setBackground(new Color(248, 249, 250));
        booksGridPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JScrollPane scrollPane = new JScrollPane(booksGridPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        configureSmoothScrolling(scrollPane);

        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel paginationPanel = createPaginationPanel();
        centerPanel.add(paginationPanel, BorderLayout.SOUTH);

        loadBooksGrid();

        return centerPanel;
    }

    private JPanel createPaginationPanel() {
        JPanel paginationPanel = new JPanel(new BorderLayout());
        paginationPanel.setOpaque(false);
        paginationPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        navPanel.setOpaque(false);

        btnFirstPage = createPaginationButton("<<", "Trang đầu");
        btnPrevPage = createPaginationButton("<", "Trang trước");
        btnNextPage = createPaginationButton(">", "Trang sau");
        btnLastPage = createPaginationButton(">>", "Trang cuối");

        lblPageInfo = new JLabel("Trang 1 / 1");
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPageInfo.setForeground(new Color(52, 73, 94));
        lblPageInfo.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));

        btnFirstPage.addActionListener(e -> navigateToPage(1));
        btnPrevPage.addActionListener(e -> navigateToPage(currentPage - 1));
        btnNextPage.addActionListener(e -> navigateToPage(currentPage + 1));
        btnLastPage.addActionListener(e -> navigateToPage(totalPages));

        navPanel.add(btnFirstPage);
        navPanel.add(btnPrevPage);
        navPanel.add(lblPageInfo);
        navPanel.add(btnNextPage);
        navPanel.add(btnLastPage);

        paginationPanel.add(navPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        infoPanel.setOpaque(false);

        JLabel lblItemsInfo = new JLabel();
        lblItemsInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblItemsInfo.setForeground(new Color(108, 117, 125));

        infoPanel.add(lblItemsInfo);
        paginationPanel.add(infoPanel, BorderLayout.EAST);

        return paginationPanel;
    }

    private JButton createPaginationButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(40, 32));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(41, 128, 185));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(new Color(52, 152, 219));
                }
            }
        });

        return button;
    }

    private void navigateToPage(int page) {
        if (page < 1 || page > totalPages) return;

        currentPage = page;
        loadBooksGrid();
        updatePaginationUI();
    }

    private void updatePaginationUI() {

        lblPageInfo.setText("Trang " + currentPage + " / " + totalPages);

        btnFirstPage.setEnabled(currentPage > 1);
        btnPrevPage.setEnabled(currentPage > 1);
        btnNextPage.setEnabled(currentPage < totalPages);
        btnLastPage.setEnabled(currentPage < totalPages);

        updateButtonAppearance(btnFirstPage);
        updateButtonAppearance(btnPrevPage);
        updateButtonAppearance(btnNextPage);
        updateButtonAppearance(btnLastPage);
    }

    private void updateButtonAppearance(JButton button) {
        if (button.isEnabled()) {
            button.setBackground(new Color(52, 152, 219));
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(176, 190, 197));
            button.setForeground(new Color(127, 140, 141));
        }
    }

    private JPanel createEnhancedBottomPanel() {
        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setOpaque(false);
        bottomWrapper.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        bottomPanel.setOpaque(false);

        btnFavorite = createModernNavButton("Sách yêu thích", new Color(239, 68, 68), new Color(220, 38, 38));
        btnActivity = createModernNavButton("Hoạt động", new Color(34, 197, 94), new Color(22, 163, 74));
        btnBorrow = createModernNavButton("Đăng ký mượn", new Color(59, 130, 246), new Color(37, 99, 235));
        JButton btnRefresh = createModernNavButton("Làm mới", new Color(14, 165, 233), new Color(2, 132, 199));
        JButton btnBorrowedBooks = createModernNavButton("Sách đã mượn", new Color(168, 85, 247), new Color(147, 51, 234));

        btnRefresh.addActionListener(e -> refreshBookDisplay());

        btnBorrowedBooks.addActionListener(e -> {
            if (userId == -1) {
                showModernMessage("Vui lòng đăng nhập để xem sách đã mượn!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            new BorrowListUI(userId).setVisible(true);
        });

        bottomPanel.add(btnFavorite);
        bottomPanel.add(btnActivity);
        bottomPanel.add(btnBorrow);
        bottomPanel.add(btnBorrowedBooks);
        bottomPanel.add(btnRefresh);

        bottomWrapper.add(bottomPanel, BorderLayout.CENTER);

        return bottomWrapper;
    }

    private JButton createModernNavButton(String text, Color baseColor, Color hoverColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 4, 12, 12);

                Color currentColor = getModel().isRollover() ? hoverColor : baseColor;
                g2d.setColor(currentColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };

        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setPreferredSize(new Dimension(170, 52));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    private void showModernMessage(String message, String title, int messageType) {
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", new Color(51, 51, 51));
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));

        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 15));
        bottomPanel.setOpaque(false);

        btnFavorite = createNavigationButton("Sách yêu thích", new Color(255, 140, 0));
        btnActivity = createNavigationButton("Hoạt động", new Color(40, 167, 69));
        btnBorrow = createNavigationButton("Đăng ký mượn sách", new Color(0, 123, 255));
        JButton btnRefresh = createNavigationButton("Làm mới", new Color(23, 162, 184));

        btnRefresh.addActionListener(e -> refreshBookDisplay());

        JButton btnBorrowedBooks = createNavigationButton("Sách đã mượn", new Color(220, 53, 69));
        btnBorrowedBooks.addActionListener(e -> {
            if (userId == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem sách đã mượn!");
                return;
            }
            new BorrowListUI(userId).setVisible(true);
        });

        bottomPanel.add(btnFavorite);
        bottomPanel.add(btnActivity);
        bottomPanel.add(btnBorrow);
        bottomPanel.add(btnBorrowedBooks);
        bottomPanel.add(btnRefresh);

        return bottomPanel;
    }

    private JButton createNavigationButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(180, 50));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void addEventListeners() {

        btnSearch.addActionListener(e -> refreshBookDisplay());
        txtSearch.addActionListener(e -> refreshBookDisplay());

        addDebouncedListener(txtAuthor);
        addDebouncedListener(txtPublisher);

        cbCategory.addActionListener(e -> {

            debounceSearch(() -> refreshBookDisplay());
        });

        btnFavorite.addActionListener(e -> showFavoriteBooks());
        btnActivity.addActionListener(e -> showActivities());
        btnBorrow.addActionListener(e -> showBorrowRequestsDialog());

        setupSearchSuggestions();
    }

    private void refreshBookDisplay() {

        currentPage = 1;
        loadBooksGrid();
    }

    private void setupSearchSuggestions() {
        suggestPopup = new JPopupMenu();

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String keyword = txtSearch.getText().trim();

                if (keyword.length() < 2 || "Nhập tên sách hoặc tác giả...".equals(keyword)) {
                    suggestPopup.setVisible(false);
                    return;
                }

                debounceSuggestions(keyword);
            }
        });

        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {

                Timer timer = new Timer(200, ev -> suggestPopup.setVisible(false));
                timer.setRepeats(false);
                timer.start();
            }
        });
    }

    private void debounceSuggestions(String keyword) {
        if (searchDebounceTimer != null) {
            searchDebounceTimer.stop();
        }

        searchDebounceTimer = new Timer(300, e -> {
            showSearchSuggestions(keyword);
        });
        searchDebounceTimer.setRepeats(false);
        searchDebounceTimer.start();
    }

    private void showSearchSuggestions(String keyword) {
        suggestPopup.removeAll();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT title FROM books WHERE title LIKE ? LIMIT 8");
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();

            boolean hasSuggestions = false;
            while (rs.next()) {
                String title = rs.getString("title");
                JMenuItem item = new JMenuItem(title);
                item.addActionListener(ev -> {
                    txtSearch.setText(title);
                    suggestPopup.setVisible(false);
                    refreshBookDisplay();
                });
                suggestPopup.add(item);
                hasSuggestions = true;
            }

            if (hasSuggestions) {
                suggestPopup.show(txtSearch, 0, txtSearch.getHeight());
            } else {
                suggestPopup.setVisible(false);
            }
        } catch (Exception ex) {

            System.err.println("Error loading suggestions: " + ex.getMessage());
        }
    }

    private void addDebouncedListener(JTextField textField) {
        textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                debounceSearch(() -> refreshBookDisplay());
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                debounceSearch(() -> refreshBookDisplay());
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                debounceSearch(() -> refreshBookDisplay());
            }
        });
    }

    private void debounceSearch(Runnable action) {
        if (filterDebounceTimer != null) {
            filterDebounceTimer.stop();
        }

        filterDebounceTimer = new Timer(DEBOUNCE_DELAY, e -> {
            action.run();
        });
        filterDebounceTimer.setRepeats(false);
        filterDebounceTimer.start();
    }

    private void connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            in.readLine();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Không thể kết nối đến server: " + ex.getMessage(),
                "Lỗi kết nối",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setUserInfo(int id, String username) {
        this.userId = id;
        lblUser.setText("Xin chào, " + username);

        System.out.println("🚀 Setting user info - ID: " + id + ", Username: " + username);

        if (lblAvatar != null) {
            lblAvatar.setVisible(true);
            System.out.println("📍 Avatar label visibility: " + lblAvatar.isVisible());
        }

        loadUserAvatarFromDB(id);

        if (btnNotification != null) {
            updateNotificationBadge(btnNotification);
        }

        showImportantNotificationsOnLogin();

        SwingUtilities.invokeLater(() -> {
            if (lblAvatar != null && lblAvatar.getParent() != null) {
                lblAvatar.getParent().repaint();
                System.out.println("🔄 Repainted user profile panel");
            }
        });
    }

    private void addToFavorite(String bookId) {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để sử dụng tính năng này!");
            return;
        }

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String checkQuery = "SELECT id FROM favorites WHERE user_id = ? AND book_id = ?";
            PreparedStatement checkPs = conn.prepareStatement(checkQuery);
            checkPs.setInt(1, userId);
            checkPs.setString(2, bookId);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Sách này đã có trong danh sách yêu thích!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String insertQuery = "INSERT INTO favorites (user_id, book_id) VALUES (?, ?)";
            PreparedStatement insertPs = conn.prepareStatement(insertQuery);
            insertPs.setInt(1, userId);
            insertPs.setString(2, bookId);

            int rowsInserted = insertPs.executeUpdate();

            if (rowsInserted > 0) {

                recordActivity(bookId, "add_favorite");

                JOptionPane.showMessageDialog(this, "Đã thêm vào danh sách yêu thích!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                refreshBookDisplay();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể thêm vào yêu thích!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void borrowBook(String bookId, String title) {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để đăng ký mượn sách!");
            return;
        }

        if (checkBorrowingLimit()) {
            JOptionPane.showMessageDialog(this,
                "Bạn đã đạt giới hạn mượn sách (10 cuốn). Vui lòng trả sách trước khi mượn thêm!",
                "Giới hạn mượn sách",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        showBorrowRequestDialog(bookId, title);
    }

    private boolean checkBorrowingLimit() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String countQuery = "SELECT COUNT(*) FROM borrows " +
                "WHERE user_id = ? AND (return_date IS NULL OR return_date = '' OR return_date = 'null')";
            PreparedStatement ps = conn.prepareStatement(countQuery);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int currentBorrowCount = rs.getInt(1);
                return currentBorrowCount >= 10;
            }
        } catch (Exception e) {
            System.err.println("Error checking borrowing limit: " + e.getMessage());
        }
        return false;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(52, 73, 94));
        return label;
    }

    private void showBorrowRequestDialog(String bookId, String title) {
        JDialog dialog = new JDialog(this, "Đăng ký mượn sách", true);
        dialog.setSize(750, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);

        JPanel containerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(59, 130, 246),
                    0, getHeight(), new Color(37, 99, 235)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        containerPanel.setOpaque(false);
        containerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 130, 246), 3),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));

        JLabel headerLabel = new JLabel("ĐĂNG KÝ MƯỢN SÁCH", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        headerLabel.setForeground(new Color(37, 99, 235));

        JLabel subHeaderLabel = new JLabel("Vui lòng điền đầy đủ thông tin bên dưới", SwingConstants.CENTER);
        subHeaderLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subHeaderLabel.setForeground(new Color(107, 114, 128));

        headerPanel.add(headerLabel, BorderLayout.NORTH);
        headerPanel.add(subHeaderLabel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        formPanel.add(createModernInfoRow("Tên sách:", title, new Color(37, 99, 235), true));
        formPanel.add(Box.createVerticalStrut(18));

        formPanel.add(createModernInfoRow("Người mượn:", lblUser.getText().replace("Xin chào, ", ""),
            new Color(55, 65, 81), false));
        formPanel.add(Box.createVerticalStrut(18));

        formPanel.add(createModernInfoRow("Ngày mượn:", java.time.LocalDate.now().toString(),
            new Color(55, 65, 81), false));
        formPanel.add(Box.createVerticalStrut(18));

        JPanel returnDatePanel = new JPanel(new BorderLayout(0, 8));
        returnDatePanel.setBackground(Color.WHITE);
        returnDatePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel returnDateLabel = new JLabel("Ngày trả dự kiến:");
        returnDateLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        returnDateLabel.setForeground(new Color(31, 41, 55));

        JComboBox<String> returnDateCombo = new JComboBox<>();
        int[] borrowDays = {7, 14, 21, 30};
        String[] labels = {"1 tuần", "2 tuần", "3 tuần", "1 tháng"};

        for (int i = 0; i < borrowDays.length; i++) {
            java.time.LocalDate returnDate = java.time.LocalDate.now().plusDays(borrowDays[i]);
            returnDateCombo.addItem(returnDate.toString() + " (" + labels[i] + ")");
        }

        returnDateCombo.setSelectedIndex(1);
        returnDateCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        returnDateCombo.setBackground(Color.WHITE);
        returnDateCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        returnDateCombo.setPreferredSize(new Dimension(0, 45));
        returnDateCombo.setFocusable(false);

        returnDatePanel.add(returnDateLabel, BorderLayout.NORTH);
        returnDatePanel.add(returnDateCombo, BorderLayout.CENTER);

        formPanel.add(returnDatePanel);
        formPanel.add(Box.createVerticalStrut(18));

        JPanel notesPanel = new JPanel(new BorderLayout(0, 8));
        notesPanel.setBackground(Color.WHITE);
        notesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JLabel notesLabel = new JLabel("Ghi chú:");
        notesLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        notesLabel.setForeground(new Color(31, 41, 55));

        JTextArea notesArea = new JTextArea(4, 20);
        notesArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        notesArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBackground(new Color(249, 250, 251));

        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
        notesScroll.setPreferredSize(new Dimension(0, 90));
        configureSmoothScrolling(notesScroll);

        notesPanel.add(notesLabel, BorderLayout.NORTH);
        notesPanel.add(notesScroll, BorderLayout.CENTER);

        formPanel.add(notesPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JButton cancelBtn = createModernButton("HỦY", new Color(156, 163, 175), new Color(107, 114, 128));
        cancelBtn.setPreferredSize(new Dimension(110, 45));
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton submitBtn = createModernButton("GỬI ĐĂNG KÝ", new Color(16, 185, 129), new Color(5, 150, 105));
        submitBtn.setPreferredSize(new Dimension(150, 45));
        submitBtn.addActionListener(e -> {
            String returnDate = returnDateCombo.getSelectedItem().toString().split(" ")[0];
            String notes = notesArea.getText().trim();
            submitBorrowRequest(bookId, returnDate, notes, dialog);
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(submitBtn);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        containerPanel.add(mainPanel, BorderLayout.CENTER);
        dialog.add(containerPanel);

        final Point[] dragOffset = new Point[1];
        headerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getPoint();
            }
        });
        headerPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point current = dialog.getLocationOnScreen();
                dialog.setLocation(current.x + e.getX() - dragOffset[0].x,
                                  current.y + e.getY() - dragOffset[0].y);
            }
        });

        dialog.setVisible(true);
    }

    private JPanel createModernInfoRow(String label, String value, Color valueColor, boolean isBold) {
        JPanel row = new JPanel(new BorderLayout(0, 6));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLabel.setForeground(new Color(31, 41, 55));

        JLabel lblValue = new JLabel("<html><div style='width:500px'>" + value + "</div></html>");
        lblValue.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 14));
        lblValue.setForeground(valueColor);
        lblValue.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        lblValue.setBackground(new Color(249, 250, 251));
        lblValue.setOpaque(true);

        row.add(lblLabel, BorderLayout.NORTH);
        row.add(lblValue, BorderLayout.CENTER);

        return row;
    }

    private JButton createModernButton(String text, Color normalColor, Color hoverColor) {
        JButton button = new JButton(text) {
            private Color currentColor = normalColor;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(currentColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        currentColor = hoverColor;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        currentColor = normalColor;
                        repaint();
                    }
                });
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private void submitBorrowRequest(String bookId, String returnDate, String notes, JDialog dialog) {
        Connection conn = null;
        PreparedStatement checkPs = null;
        PreparedStatement insertPs = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000");

            String checkQuery = "SELECT id FROM borrow_requests WHERE user_id = ? AND book_id = ? AND status = 'PENDING'";
            checkPs = conn.prepareStatement(checkQuery);
            checkPs.setInt(1, userId);
            checkPs.setString(2, bookId);
            rs = checkPs.executeQuery();

            if (rs.next()) {
                JOptionPane.showMessageDialog(dialog,
                    "Bạn đã có đăng ký mượn sách này đang chờ duyệt!",
                    "Thông báo",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (rs != null) rs.close();
            if (checkPs != null) checkPs.close();

            String insertQuery = "INSERT INTO borrow_requests (user_id, book_id, request_date, expected_return_date, notes, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";
            insertPs = conn.prepareStatement(insertQuery);
            insertPs.setInt(1, userId);
            insertPs.setString(2, bookId);
            insertPs.setString(3, java.time.LocalDateTime.now().toString());
            insertPs.setString(4, returnDate);
            insertPs.setString(5, notes);

            int rowsInserted = insertPs.executeUpdate();

            if (rowsInserted > 0) {

                recordActivity(bookId, "borrow_request");

                NotificationUI.addNotification(
                    userId,
                    "borrow_request",
                    "Đăng ký mượn sách",
                    "Bạn đã đăng ký mượn sách thành công. Vui lòng chờ quản trị viên duyệt."
                );

                updateNotificationBadge(btnNotification);

                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                    "Đăng ký mượn sách thành công!\n" +
                    "Ngày trả dự kiến: " + returnDate + "\n" +
                    "Vui lòng chờ quản trị viên duyệt đăng ký.",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
                refreshBookDisplay();
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "Không thể tạo đăng ký mượn sách!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception dbEx) {
            JOptionPane.showMessageDialog(dialog,
                "Lỗi database: " + dbEx.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        } finally {

            try {
                if (rs != null) rs.close();
                if (checkPs != null) checkPs.close();
                if (insertPs != null) insertPs.close();
                if (conn != null) conn.close();
            } catch (Exception ex) {
                System.err.println("Error closing database resources: " + ex.getMessage());
            }
        }
    }

    private void loadBooksGrid() {
        if (booksGridPanel == null) return;

        if (isLoading) {
            System.out.println("⚠️ Đang load sách, bỏ qua request mới");
            return;
        }

        isLoading = true;

        JDialog loadingDialog = null;
        try {
            if (taskManager != null) {
                loadingDialog = LoadingUtils.showLoadingDialog(this, "Đang tải danh sách sách");
            }
        } catch (Throwable t) {

            loadingDialog = null;
        }

        final JDialog finalLoadingDialog = loadingDialog;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                loadBooksDirectly();
                return null;
            }

            @Override
            protected void done() {
                if (finalLoadingDialog != null) {
                    try {
                        finalLoadingDialog.setVisible(false);
                        finalLoadingDialog.dispose();
                    } catch (Exception ignored) {
                    }
                }

                updatePaginationUI();

                isLoading = false;
            }
        };

        worker.execute();
    }

    private void loadBooksDirectly() {

        java.util.List<JPanel> bookPanels = new java.util.ArrayList<>();

        try {
            if (dbManager != null) {
                dbManager.executeWithConnection(conn -> {
                    loadBooksFromDatabase(conn, bookPanels);
                    return null;
                });
            } else {

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                    loadBooksFromDatabase(conn, bookPanels);
                }
            }
        } catch (SQLException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                    "Lỗi tải sách: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            });
            return;
        }

        SwingUtilities.invokeLater(() -> {
            booksGridPanel.removeAll();
            for (JPanel bookPanel : bookPanels) {
                booksGridPanel.add(bookPanel);
            }
            booksGridPanel.revalidate();
            booksGridPanel.repaint();
        });
    }

    private void loadBooksFromDatabase(Connection conn, java.util.List<JPanel> bookPanels) throws SQLException {
        try {
            Statement stmt = conn.createStatement();
            stmt.execute("ALTER TABLE books ADD COLUMN status TEXT DEFAULT 'approved'");

            stmt.execute("UPDATE books SET status = 'approved' WHERE status IS NULL OR status = 'pending'");
        } catch (SQLException e) {

            try {
                Statement updateStmt = conn.createStatement();
                updateStmt.execute("UPDATE books SET status = 'approved' WHERE status IS NULL OR status = 'pending'");
            } catch (SQLException ex) {

            }
        }

        StringBuilder baseQuery = new StringBuilder("SELECT COUNT(*) FROM books WHERE 1=1");

        String searchText = txtSearch.getText().trim();
        if ("Nhập tên sách hoặc tác giả...".equals(searchText)) searchText = "";

        String authorText = txtAuthor.getText().trim();
        if ("Nhập tên tác giả...".equals(authorText)) authorText = "";

        String publisherText = txtPublisher.getText().trim();
        if ("Nhập nhà xuất bản...".equals(publisherText)) publisherText = "";

        String categoryText = cbCategory.getSelectedItem().toString();

        if (!searchText.isEmpty()) {
            baseQuery.append(" AND (title LIKE ? OR author LIKE ? OR publisher LIKE ?)");
        }
        if (!authorText.isEmpty()) {
            baseQuery.append(" AND author LIKE ?");
        }
        if (!publisherText.isEmpty()) {
            baseQuery.append(" AND publisher LIKE ?");
        }
        if (!"Tất cả".equals(categoryText)) {
            baseQuery.append(" AND category = ?");
        }

        PreparedStatement countPs = conn.prepareStatement(baseQuery.toString());
        int paramIndex = 1;

        if (!searchText.isEmpty()) {
            String searchPattern = "%" + searchText + "%";
            countPs.setString(paramIndex++, searchPattern);
            countPs.setString(paramIndex++, searchPattern);
            countPs.setString(paramIndex++, searchPattern);
        }
        if (!authorText.isEmpty()) {
            countPs.setString(paramIndex++, "%" + authorText + "%");
        }
        if (!publisherText.isEmpty()) {
            countPs.setString(paramIndex++, "%" + publisherText + "%");
        }
        if (!"Tất cả".equals(categoryText)) {
            countPs.setString(paramIndex++, categoryText);
        }

        ResultSet countRs = countPs.executeQuery();
        totalItems = countRs.getInt(1);
        totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        countRs.close();
        countPs.close();

        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        } else if (currentPage < 1) {
            currentPage = 1;
        }

        StringBuilder dataQuery = new StringBuilder("SELECT id, title, author, publisher, year, category, quantity, cover_image, status FROM books WHERE 1=1");

        if (!searchText.isEmpty()) {
            dataQuery.append(" AND (title LIKE ? OR author LIKE ? OR publisher LIKE ?)");
        }
        if (!authorText.isEmpty()) {
            dataQuery.append(" AND author LIKE ?");
        }
        if (!publisherText.isEmpty()) {
            dataQuery.append(" AND publisher LIKE ?");
        }
        if (!"Tất cả".equals(categoryText)) {
            dataQuery.append(" AND category = ?");
        }

        dataQuery.append(" ORDER BY id LIMIT ? OFFSET ?");

        PreparedStatement dataPs = conn.prepareStatement(dataQuery.toString());
        paramIndex = 1;

        if (!searchText.isEmpty()) {
            String searchPattern = "%" + searchText + "%";
            dataPs.setString(paramIndex++, searchPattern);
            dataPs.setString(paramIndex++, searchPattern);
            dataPs.setString(paramIndex++, searchPattern);
        }
        if (!authorText.isEmpty()) {
            dataPs.setString(paramIndex++, "%" + authorText + "%");
        }
        if (!publisherText.isEmpty()) {
            dataPs.setString(paramIndex++, "%" + publisherText + "%");
        }
        if (!"Tất cả".equals(categoryText)) {
            dataPs.setString(paramIndex++, categoryText);
        }

        dataPs.setInt(paramIndex++, itemsPerPage);
        dataPs.setInt(paramIndex++, (currentPage - 1) * itemsPerPage);

        ResultSet rs = dataPs.executeQuery();

        while (rs.next()) {
            String bookId = rs.getString("id");
            String title = rs.getString("title");
            String author = rs.getString("author");
            String category = rs.getString("category");
            int quantity = rs.getInt("quantity");
            String coverImage = rs.getString("cover_image");
            String status = rs.getString("status");

            JPanel bookPanel = createBookPanelWithStatus(bookId, title, author, category, quantity, coverImage, status);
            bookPanels.add(bookPanel);
        }

        rs.close();
        dataPs.close();
    }

    private JPanel createBookPanelWithStatus(String bookId, String title, String author, String category, int quantity, String coverImage, String status) {

        JPanel bookPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(0, 0, 0, 8));
                g2d.fillRoundRect(4, 6, getWidth() - 8, getHeight() - 8, 16, 16);
                g2d.setColor(new Color(0, 0, 0, 5));
                g2d.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 4, 16, 16);

                Color cardBg;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    cardBg = new Color(31, 41, 55);
                } else {
                    cardBg = Color.WHITE;
                }
                g2d.setColor(cardBg);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                g2d.setColor(new Color(229, 231, 235));
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            }
        };

        bookPanel.setLayout(new BoxLayout(bookPanel, BoxLayout.Y_AXIS));
        bookPanel.setOpaque(false);
        bookPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        bookPanel.setPreferredSize(new Dimension(210, 420));

        bookPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                bookPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                bookPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                bookPanel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                bookPanel.repaint();
            }
        });

        JPanel imagePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(243, 244, 246),
                    0, getHeight(), new Color(229, 231, 235)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        };
        imagePanel.setOpaque(false);
        imagePanel.setPreferredSize(new Dimension(180, 200));
        imagePanel.setMaximumSize(new Dimension(180, 200));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel imageLabel;
        if (coverImage != null && !coverImage.trim().isEmpty()) {
            try {

                ImageCacheManager cacheManager = ImageCacheManager.getInstance();
                ImageIcon cachedIcon = cacheManager.getImage(coverImage, bookId, 164, 184);

                if (cachedIcon != null && cachedIcon.getIconWidth() > 0) {
                    imageLabel = new JLabel(cachedIcon, SwingConstants.CENTER);
                } else {
                    imageLabel = createFallbackImageLabel(category);
                }
            } catch (Exception e) {
                imageLabel = createFallbackImageLabel(category);
            }
        } else {
            imageLabel = createFallbackImageLabel(category);
        }

        imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showBookDetails(bookId, title, author, category, quantity, coverImage);
            }
        });

        imagePanel.add(imageLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));

        JLabel titleLabel = new JLabel("<html><div style='width:180px;text-align:center;line-height:1.3'><b>" +
            (title.length() > 50 ? title.substring(0, 50) + "..." : title) + "</b></div></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            titleLabel.setForeground(new Color(243, 244, 246));
        } else {
            titleLabel.setForeground(new Color(17, 24, 39));
        }

        JLabel authorLabel = new JLabel("<html><div style='width:180px;text-align:center'>" +
            (author.length() > 40 ? author.substring(0, 40) + "..." : author) + "</div></html>");
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authorLabel.setForeground(new Color(107, 114, 128));
        authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel categoryBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        categoryBadge.setOpaque(false);
        categoryBadge.setMaximumSize(new Dimension(180, 25));

        JLabel categoryLabel = new JLabel(category);
        categoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        categoryLabel.setForeground(Color.WHITE);
        categoryLabel.setOpaque(true);
        categoryLabel.setBackground(getCategoryColor(category));
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        categoryBadge.add(categoryLabel);

        String quantityIcon = quantity > 5 ? "" : (quantity > 0 ? "" : "");
        Color quantityColor = quantity > 5 ? new Color(16, 185, 129) :
                             (quantity > 0 ? new Color(251, 146, 60) : new Color(239, 68, 68));

        JLabel quantityLabel = new JLabel(quantityIcon + " Còn " + quantity + " cuốn");
        quantityLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        quantityLabel.setForeground(quantityColor);
        quantityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(authorLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(categoryBadge);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(quantityLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton favoriteBtn = createModernActionButton("♥", new Color(239, 68, 68), new Color(220, 38, 38));
        favoriteBtn.setToolTipText("Thêm vào yêu thích");
        favoriteBtn.addActionListener(e -> addToFavorite(bookId));

        JButton infoBtn = createModernActionButton("i", new Color(59, 130, 246), new Color(37, 99, 235));
        infoBtn.setToolTipText("Xem chi tiết");
        infoBtn.addActionListener(e -> showBookDetails(bookId, title, author, category, quantity, coverImage));

        JButton borrowBtn = createModernActionButton("M", new Color(16, 185, 129), new Color(5, 150, 105));
        borrowBtn.setToolTipText("Đăng ký mượn");
        borrowBtn.setEnabled(quantity > 0);
        if (quantity <= 0) {
            borrowBtn.setBackground(new Color(156, 163, 175));
        }
        borrowBtn.addActionListener(e -> borrowBook(bookId, title));

        buttonPanel.add(favoriteBtn);
        buttonPanel.add(infoBtn);
        buttonPanel.add(borrowBtn);

        bookPanel.add(imagePanel);
        bookPanel.add(infoPanel);
        bookPanel.add(buttonPanel);

        return bookPanel;
    }

    private JLabel createFallbackImageLabel(String category) {
        JLabel label = new JLabel(getBookIcon(category), SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI Emoji", Font.BOLD, 48));
        label.setForeground(getCategoryColor(category));
        return label;
    }

    private JButton createModernActionButton(String icon, Color baseColor, Color hoverColor) {
        JButton button = new JButton(icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color currentColor = getModel().isRollover() && isEnabled() ? hoverColor :
                                   isEnabled() ? baseColor : new Color(156, 163, 175);

                if (isEnabled()) {
                    g2d.setColor(new Color(0, 0, 0, 20));
                    g2d.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                }

                g2d.setColor(currentColor);
                g2d.fillOval(0, 0, getWidth(), getHeight());

                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(42, 42));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    private void showFavoriteBooks() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem sách yêu thích!");
            return;
        }

        try {
            if (socket == null || socket.isClosed() || out == null || in == null) {
                connectToServer();
            }

            out.println("LIST_FAVORITES|" + userId);
            String resp = in.readLine();

            if (resp != null && resp.startsWith("FAVORITES_LIST|")) {
                String data = resp.substring("FAVORITES_LIST|".length());
                if (!data.trim().isEmpty()) {
                    showFavoriteBooksDialog(data);
                } else {
                    JOptionPane.showMessageDialog(this, "Bạn chưa có sách yêu thích nào!", "Sách yêu thích", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không thể tải danh sách sách yêu thích!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showFavoriteBooksDialog(String data) {
        JDialog dialog = new JDialog(this, "Danh sách sách yêu thích", true);
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(255, 248, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel headerLabel = new JLabel("SÁCH YÊU THÍCH CỦA BẠN", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerLabel.setForeground(new Color(255, 140, 0));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Tên sách", "Tác giả", "Yêu thích", "Thao tác"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String[] books = data.split(";");
        System.out.println("DEBUG: Favorites data: " + data);

        for (String book : books) {
            if (!book.trim().isEmpty()) {
                String[] parts = book.split(" - ");
                System.out.println("DEBUG: Book parts: " + java.util.Arrays.toString(parts));

                if (parts.length >= 3) {
                    model.addRow(new Object[]{parts[0], parts[1], parts[2], "Yêu thích", "Xóa"});
                }
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(220, 220, 220));
        table.getTableHeader().setBackground(new Color(255, 140, 0));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setSelectionBackground(new Color(255, 140, 0, 50));

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        int column = table.columnAtPoint(e.getPoint());
                        if (column == 4) {
                            deleteFavoriteBook(table, model, row, dialog);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(255, 140, 0), 2));
        configureSmoothScrolling(scrollPane);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton deleteBtn = new JButton("Xóa đã chọn");
        deleteBtn.setPreferredSize(new Dimension(140, 35));
        deleteBtn.setBackground(new Color(220, 53, 69));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                deleteFavoriteBook(table, model, selectedRow, dialog);
            } else {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn một sách để xóa!");
            }
        });

        JButton deleteAllBtn = new JButton("Xóa tất cả");
        deleteAllBtn.setPreferredSize(new Dimension(120, 35));
        deleteAllBtn.setBackground(new Color(255, 193, 7));
        deleteAllBtn.setForeground(Color.WHITE);
        deleteAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteAllBtn.setFocusPainted(false);
        deleteAllBtn.addActionListener(e -> deleteAllFavorites(dialog));

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setPreferredSize(new Dimension(100, 35));
        closeBtn.setBackground(new Color(108, 117, 125));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(deleteBtn);
        buttonPanel.add(deleteAllBtn);
        buttonPanel.add(closeBtn);

        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void deleteFavoriteBook(JTable table, DefaultTableModel model, int row, JDialog dialog) {
        String bookId = model.getValueAt(row, 0).toString();
        String bookTitle = model.getValueAt(row, 1).toString();

        int choice = JOptionPane.showConfirmDialog(dialog,
            "Bạn có chắc muốn xóa sách '" + bookTitle + "' khỏi danh sách yêu thích?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String deleteQuery = "DELETE FROM favorites WHERE user_id = ? AND book_id = ?";
                PreparedStatement ps = conn.prepareStatement(deleteQuery);
                ps.setInt(1, userId);
                ps.setString(2, bookId);

                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    model.removeRow(row);
                    JOptionPane.showMessageDialog(dialog, "Đã xóa sách khỏi danh sách yêu thích!");

                    recordActivity(bookId, "remove_favorite");
                } else {
                    JOptionPane.showMessageDialog(dialog, "Không thể xóa sách!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog, "Lỗi xóa: " + e.getMessage());
            }
        }
    }

    private void deleteAllFavorites(JDialog dialog) {
        int choice = JOptionPane.showConfirmDialog(dialog,
            "Bạn có chắc muốn xóa TẤT CẢ sách khỏi danh sách yêu thích?",
            "Xác nhận xóa tất cả",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String deleteQuery = "DELETE FROM favorites WHERE user_id = ?";
                PreparedStatement ps = conn.prepareStatement(deleteQuery);
                ps.setInt(1, userId);

                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(dialog, "Đã xóa tất cả sách khỏi danh sách yêu thích!");
                    dialog.dispose();
                    showFavoriteBooks();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Không có sách nào để xóa!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog, "Lỗi xóa: " + e.getMessage());
            }
        }
    }

    private void showActivities() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem hoạt động!");
            return;
        }

        try {
            if (socket == null || socket.isClosed() || out == null || in == null) {
                connectToServer();
            }

            out.println("LIST_ACTIVITIES|" + userId);
            String resp = in.readLine();

            if (resp != null && resp.startsWith("ACTIVITIES_LIST|")) {
                String data = resp.substring("ACTIVITIES_LIST|".length());
                if (!data.trim().isEmpty()) {
                    showActivitiesDialog(data);
                } else {
                    JOptionPane.showMessageDialog(this, "Chưa có hoạt động nào!", "Lịch sử hoạt động", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không thể tải lịch sử hoạt động!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showActivitiesDialog(String data) {
        JDialog dialog = new JDialog(this, "Lịch sử hoạt động", true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(240, 248, 255));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel headerLabel = new JLabel("LỊCH SỬ HOẠT ĐỘNG CỦA BẠN", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerLabel.setForeground(new Color(40, 167, 69));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Sách", "Hoạt động", "Thời gian", "Thao tác"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String[] activities = data.split(";");
        System.out.println("DEBUG: Activities data: " + data);

        for (String activity : activities) {
            if (!activity.trim().isEmpty()) {
                String[] parts = activity.split(" - ");
                System.out.println("DEBUG: Activity parts: " + java.util.Arrays.toString(parts));

                if (parts.length >= 4) {
                    String actionText = getActionDisplayText(parts[2]);
                    model.addRow(new Object[]{parts[0], parts[1], actionText, parts[3], "Xóa"});
                }
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(220, 220, 220));
        table.getTableHeader().setBackground(new Color(40, 167, 69));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setSelectionBackground(new Color(40, 167, 69, 50));

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        int column = table.columnAtPoint(e.getPoint());
                        if (column == 4) {
                            deleteActivity(table, model, row, dialog);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(40, 167, 69), 2));
        configureSmoothScrolling(scrollPane);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton deleteBtn = new JButton("Xóa đã chọn");
        deleteBtn.setPreferredSize(new Dimension(140, 40));
        deleteBtn.setBackground(new Color(220, 53, 69));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                deleteActivity(table, model, selectedRow, dialog);
            } else {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn một hoạt động để xóa!");
            }
        });

        JButton clearAllBtn = new JButton("Xóa tất cả");
        clearAllBtn.setPreferredSize(new Dimension(120, 40));
        clearAllBtn.setBackground(new Color(255, 193, 7));
        clearAllBtn.setForeground(Color.WHITE);
        clearAllBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clearAllBtn.setFocusPainted(false);
        clearAllBtn.addActionListener(e -> clearAllActivities(dialog));

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setPreferredSize(new Dimension(100, 40));
        closeBtn.setBackground(new Color(108, 117, 125));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearAllBtn);
        buttonPanel.add(closeBtn);

        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private String getActionDisplayText(String action) {
        switch (action.toLowerCase()) {
            case "favorite":
            case "add_favorite":
                return "Thêm yêu thích";
            case "remove_favorite":
                return "Bỏ yêu thích";
            case "borrow":
            case "borrow_request":
                return "Đăng ký mượn";
            case "return":
                return "Trả sách";
            case "view":
                return "Xem chi tiết";
            default:
                return action;
        }
    }

    private void deleteActivity(JTable table, DefaultTableModel model, int row, JDialog dialog) {
        String activityId = model.getValueAt(row, 0).toString();
        String bookTitle = model.getValueAt(row, 1).toString();

        int choice = JOptionPane.showConfirmDialog(dialog,
            "Bạn có chắc muốn xóa hoạt động với sách '" + bookTitle + "'?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String deleteQuery = "DELETE FROM activities WHERE id = ? AND user_id = ?";
                PreparedStatement ps = conn.prepareStatement(deleteQuery);
                ps.setString(1, activityId);
                ps.setInt(2, userId);

                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    model.removeRow(row);
                    JOptionPane.showMessageDialog(dialog, "Đã xóa hoạt động!");
                } else {
                    JOptionPane.showMessageDialog(dialog, "Không thể xóa hoạt động!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog, "Lỗi xóa: " + e.getMessage());
            }
        }
    }

    private void clearAllActivities(JDialog dialog) {
        int choice = JOptionPane.showConfirmDialog(dialog,
            "Bạn có chắc muốn xóa TẤT CẢ lịch sử hoạt động?",
            "Xác nhận xóa tất cả",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String deleteQuery = "DELETE FROM activities WHERE user_id = ?";
                PreparedStatement ps = conn.prepareStatement(deleteQuery);
                ps.setInt(1, userId);

                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    JOptionPane.showMessageDialog(dialog, "Đã xóa tất cả lịch sử hoạt động!");
                    dialog.dispose();
                    showActivities();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Không có hoạt động nào để xóa!");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(dialog, "Lỗi xóa: " + e.getMessage());
            }
        }
    }

    private void recordActivity(String bookId, String action) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String insertQuery = "INSERT INTO activities (user_id, book_id, action, action_time) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(insertQuery);
            ps.setInt(1, userId);
            ps.setString(2, bookId);
            ps.setString(3, action);
            ps.setString(4, java.time.LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error recording activity: " + e.getMessage());
        }
    }

    private void showBorrowRequestsDialog() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem đăng ký mượn sách!");
            return;
        }

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "SELECT br.id, b.title, b.author, br.request_date, br.status, br.admin_notes " +
                "FROM borrow_requests br " +
                "JOIN books b ON br.book_id = b.id " +
                "WHERE br.user_id = ? " +
                "ORDER BY br.request_date DESC";

            java.sql.PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            java.sql.ResultSet rs = ps.executeQuery();

            JDialog dialog = new JDialog(this, "Đăng ký mượn sách của bạn", true);
            dialog.setSize(900, 600);
            dialog.setLocationRelativeTo(this);

            JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    Color startColor = new Color(240, 248, 255);
                    Color endColor = new Color(230, 240, 250);
                    GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel headerLabel = new JLabel("Danh sách đăng ký mượn sách", SwingConstants.CENTER);
            headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            headerLabel.setForeground(new Color(0, 123, 255));
            headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

            String[] columnNames = {"ID", "Tên sách", "Tác giả", "Ngày đăng ký", "Trạng thái", "Ghi chú"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            while (rs.next()) {
                String status = rs.getString("status");
                String statusDisplay;
                switch (status) {
                    case "PENDING":
                        statusDisplay = "Chờ duyệt";
                        break;
                    case "APPROVED":
                        statusDisplay = "Đã duyệt";
                        break;
                    case "REJECTED":
                        statusDisplay = "Đã từ chối";
                        break;
                    default:
                        statusDisplay = status;
                        break;
                }

                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("request_date"),
                    statusDisplay,
                    rs.getString("admin_notes") != null ? rs.getString("admin_notes") : ""
                };
                model.addRow(row);
            }

            JTable table = new JTable(model);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            table.setRowHeight(45);
            table.setSelectionBackground(new Color(0, 123, 255, 50));
            table.setSelectionForeground(new Color(0, 123, 255));
            table.setGridColor(new Color(222, 226, 230));
            table.setShowGrid(true);

            JTableHeader header = table.getTableHeader();
            header.setBackground(new Color(0, 123, 255));
            header.setForeground(Color.WHITE);
            header.setFont(new Font("Segoe UI", Font.BOLD, 14));
            header.setPreferredSize(new Dimension(0, 45));

            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(1).setPreferredWidth(200);
            table.getColumnModel().getColumn(2).setPreferredWidth(150);
            table.getColumnModel().getColumn(3).setPreferredWidth(130);
            table.getColumnModel().getColumn(4).setPreferredWidth(120);
            table.getColumnModel().getColumn(5).setPreferredWidth(200);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0, 123, 255), 2));
            configureSmoothScrolling(scrollPane);

            JPanel infoPanel = new JPanel(new GridLayout(1, 3, 15, 0));
            infoPanel.setOpaque(false);

            int totalRequests = model.getRowCount();
            int pendingCount = 0, rejectedCount = 0;

            for (int i = 0; i < totalRequests; i++) {
                String status = model.getValueAt(i, 4).toString();
                if (status.contains("Chờ duyệt")) pendingCount++;
                else if (status.contains("Đã từ chối")) rejectedCount++;
            }

            JPanel totalPanel = createRequestInfoCard("Tổng số", String.valueOf(totalRequests), new Color(0, 123, 255));
            JPanel pendingPanel = createRequestInfoCard("Chờ duyệt", String.valueOf(pendingCount), new Color(255, 193, 7));
            JPanel rejectedPanel = createRequestInfoCard("Từ chối", String.valueOf(rejectedCount), new Color(220, 53, 69));

            infoPanel.add(totalPanel);
            infoPanel.add(pendingPanel);
            infoPanel.add(rejectedPanel);

            JButton closeBtn = new JButton("Đóng");
            closeBtn.setPreferredSize(new Dimension(100, 40));
            closeBtn.setBackground(new Color(0, 123, 255));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            closeBtn.setFocusPainted(false);
            closeBtn.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setOpaque(false);
            buttonPanel.add(closeBtn);

            JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
            centerPanel.setOpaque(false);
            centerPanel.add(infoPanel, BorderLayout.NORTH);
            centerPanel.add(scrollPane, BorderLayout.CENTER);

            mainPanel.add(headerLabel, BorderLayout.NORTH);
            mainPanel.add(centerPanel, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(mainPanel);
            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải đăng ký: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createRequestInfoCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color startColor = Color.WHITE;
                Color endColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 20);
                GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(new Color(33, 37, 41));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void showBookDetails(String bookId, String title, String author, String category, int quantity, String coverImage) {

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM books WHERE id = ?");
            ps.setString(1, bookId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String publisher = rs.getString("publisher");
                int year = rs.getInt("year");
                String description = rs.getString("description");

                showBookDetailsDialog(bookId, title, author, publisher, year, category, quantity, coverImage, description);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải thông tin sách: " + ex.getMessage());
        }
    }

    private void showBookDetailsDialog(String bookId, String title, String author, String publisher,
                                     int year, String category, int quantity, String coverImage, String description) {
        JDialog dialog = new JDialog(this, "Thông tin chi tiết sách", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color startColor = new Color(248, 249, 250);
                Color endColor = new Color(233, 236, 239);
                GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel headerLabel = new JLabel("Thông tin chi tiết", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(new Color(0, 123, 255));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setOpaque(false);

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setPreferredSize(new Dimension(230, 320));
        imagePanel.setBackground(Color.WHITE);
        imagePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JLabel bookImageLabel;
        if (coverImage != null && !coverImage.trim().isEmpty()) {
            try {

                ImageCacheManager cacheManager = ImageCacheManager.getInstance();
                ImageIcon cachedIcon = cacheManager.getImage(coverImage, bookId, 214, 304);

                if (cachedIcon != null && cachedIcon.getIconWidth() > 0) {
                    bookImageLabel = new JLabel(cachedIcon, SwingConstants.CENTER);
                } else {
                    String bookIcon = getBookIcon(category);
                    bookImageLabel = new JLabel(bookIcon, SwingConstants.CENTER);
                    bookImageLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                    bookImageLabel.setForeground(getCategoryColor(category));
                }
            } catch (Exception e) {
                String bookIcon = getBookIcon(category);
                bookImageLabel = new JLabel(bookIcon, SwingConstants.CENTER);
                bookImageLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                bookImageLabel.setForeground(getCategoryColor(category));
            }
        } else {
            String bookIcon = getBookIcon(category);
            bookImageLabel = new JLabel(bookIcon, SwingConstants.CENTER);
            bookImageLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            bookImageLabel.setForeground(getCategoryColor(category));
        }
        imagePanel.add(bookImageLabel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("<html><div style='width:400px'><b style='font-size:18px'>" + title + "</b></div></html>");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(33, 37, 41));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel authorPanel = createInfoRow("Tác giả:", author);

        JPanel publisherPanel = createInfoRow("Nhà xuất bản:", publisher);

        JPanel yearPanel = createInfoRow("Năm xuất bản:", String.valueOf(year));

        JPanel categoryPanel = createInfoRow("Thể loại:", category);

        JPanel quantityPanel = createInfoRow("Số lượng:", quantity + " cuốn");

        String statusText = quantity > 0 ? "Có sẵn" : "Hết sách";
        Color statusColor = quantity > 0 ? new Color(40, 167, 69) : new Color(220, 53, 69);
        JPanel statusPanel = createInfoRow("Trạng thái:", statusText, statusColor);

        JLabel descLabel = new JLabel("Mô tả:");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        descLabel.setForeground(new Color(52, 58, 64));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String descText = (description != null && !description.trim().isEmpty()) ? description : "Chưa có mô tả cho sách này.";
        JTextArea descArea = new JTextArea(descText);
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descArea.setForeground(new Color(73, 80, 87));
        descArea.setBackground(Color.WHITE);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JScrollPane descScrollPane = new JScrollPane(descArea);
        descScrollPane.setPreferredSize(new Dimension(400, 100));
        descScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        configureSmoothScrolling(descScrollPane);

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(authorPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(publisherPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(yearPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(categoryPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(quantityPanel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(statusPanel);
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(descLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(descScrollPane);

        contentPanel.add(imagePanel, BorderLayout.WEST);
        contentPanel.add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);

        JButton favoriteBtn = new JButton("Thêm yêu thích");
        favoriteBtn.setPreferredSize(new Dimension(150, 40));
        favoriteBtn.setBackground(new Color(255, 140, 0));
        favoriteBtn.setForeground(Color.WHITE);
        favoriteBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        favoriteBtn.setFocusPainted(false);
        favoriteBtn.addActionListener(e -> {
            addToFavorite(bookId);
            dialog.dispose();
        });

        JButton borrowBtn = new JButton("Đăng ký mượn");
        borrowBtn.setPreferredSize(new Dimension(160, 40));
        borrowBtn.setBackground(new Color(0, 123, 255));
        borrowBtn.setForeground(Color.WHITE);
        borrowBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        borrowBtn.setFocusPainted(false);
        borrowBtn.setEnabled(quantity > 0);
        borrowBtn.addActionListener(e -> {
            borrowBook(bookId, title);
            dialog.dispose();
        });

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setPreferredSize(new Dimension(100, 40));
        closeBtn.setBackground(new Color(108, 117, 125));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(favoriteBtn);
        buttonPanel.add(borrowBtn);
        buttonPanel.add(closeBtn);

        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private JPanel createInfoRow(String label, String value) {
        return createInfoRow(label, value, new Color(73, 80, 87));
    }

    private JPanel createInfoRow(String label, String value, Color valueColor) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelComponent.setForeground(new Color(52, 58, 64));
        labelComponent.setPreferredSize(new Dimension(140, 25));

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueComponent.setForeground(valueColor);

        panel.add(labelComponent);
        panel.add(valueComponent);

        return panel;
    }

    private String getBookIcon(String category) {
        switch (category) {
            case "Văn học – Tiểu thuyết": return "Văn học";
            case "Khoa học – Công nghệ": return "Khoa học";
            case "Kinh tế – Quản trị": return "Kinh tế";
            case "Tâm lý – Kỹ năng sống": return "Tâm lý";
            case "Giáo trình – Học thuật": return "Giáo trình";
            case "Trẻ em – Thiếu nhi": return "Trẻ em";
            case "Lịch sử – Địa lý": return "Lịch sử";
            case "Tôn giáo – Triết học": return "Tôn giáo";
            case "Ngoại ngữ – Từ điển": return "Ngoại ngữ";
            case "Nghệ thuật – Âm nhạc": return "Nghệ thuật";
            default: return "Sách";
        }
    }

    private Color getCategoryColor(String category) {
        switch (category) {
            case "Văn học – Tiểu thuyết": return new Color(139, 69, 19);
            case "Khoa học – Công nghệ": return new Color(0, 100, 200);
            case "Kinh tế – Quản trị": return new Color(255, 140, 0);
            case "Tâm lý – Kỹ năng sống": return new Color(255, 20, 147);
            case "Giáo trình – Học thuật": return new Color(34, 139, 34);
            case "Trẻ em – Thiếu nhi": return new Color(255, 105, 180);
            case "Lịch sử – Địa lý": return new Color(128, 128, 0);
            case "Tôn giáo – Triết học": return new Color(75, 0, 130);
            case "Ngoại ngữ – Từ điển": return new Color(255, 0, 255);
            case "Nghệ thuật – Âm nhạc": return new Color(255, 69, 0);
            default: return new Color(150, 150, 150);
        }
    }

    private void initializeBorrowRequestsTable() {
        Connection conn = null;
        java.sql.Statement stmt = null;
        try {

            conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000");

            stmt = conn.createStatement();

            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA busy_timeout=30000");

            String createBorrowRequestsTable = "CREATE TABLE IF NOT EXISTS borrow_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "book_id INTEGER NOT NULL, " +
                "request_date TEXT NOT NULL, " +
                "status TEXT DEFAULT 'PENDING', " +
                "admin_notes TEXT, " +
                "approved_date TEXT, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (book_id) REFERENCES books(id)" +
                ")";

            stmt.execute(createBorrowRequestsTable);

            String createNotificationsTable = "CREATE TABLE IF NOT EXISTS notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "is_read INTEGER DEFAULT 0, " +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")";

            stmt.execute(createNotificationsTable);

        } catch (Exception e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                    System.err.println("Error closing statement: " + e.getMessage());
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    private static UserProfileUI openUserProfileUI = null;

    private void showUserProfile() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem thông tin cá nhân!");
            return;
        }

        if (openUserProfileUI != null && openUserProfileUI.isDisplayable()) {

            openUserProfileUI.toFront();
            openUserProfileUI.requestFocus();
            return;
        }

        String currentUsername = lblUser.getText().replace("Xin chào, ", "");
        openUserProfileUI = new UserProfileUI(userId, currentUsername);

        openUserProfileUI.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                openUserProfileUI = null;
            }
        });

        openUserProfileUI.setVisible(true);
    }

    private void showNotifications() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem thông báo!");
            return;
        }

        new NotificationUI(userId).setVisible(true);
    }

    private void updateNotificationBadge(JButton btnNotification) {

        SwingUtilities.invokeLater(() -> {
            if (userId != -1) {
                int unreadCount = NotificationUI.getUnreadNotificationCount(userId);
                if (unreadCount > 0) {
                    btnNotification.setText("Thông báo (" + unreadCount + ")");
                    btnNotification.setPreferredSize(new Dimension(150, 40));
                    btnNotification.setBackground(new Color(231, 76, 60));
                    btnNotification.setToolTipText("Bạn có " + unreadCount + " thông báo chưa đọc");
                } else {
                    btnNotification.setText("Thông báo");
                    btnNotification.setPreferredSize(new Dimension(120, 40));
                    btnNotification.setBackground(new Color(255, 140, 0));
                    btnNotification.setToolTipText("Xem thông báo của bạn");
                }
            }
        });

        Timer notificationTimer = new Timer(30000, e -> updateNotificationBadge(btnNotification));
        notificationTimer.start();
    }

    private void setDefaultAvatar() {
        System.out.println("🔵 Setting default avatar");

        lblAvatar.setIcon(createDefaultAvatarIcon());
        lblAvatar.setToolTipText("Click để xem thông tin cá nhân");
        lblAvatar.repaint();

        if (lblAvatar.getParent() != null) {
            lblAvatar.getParent().revalidate();
            lblAvatar.getParent().repaint();
        }
        System.out.println("✅ Default avatar set successfully");
    }

    private ImageIcon createDefaultAvatarIcon() {

        int size = 32;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Color bgColor1, bgColor2, borderColor, textColor;
        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            bgColor1 = new Color(88, 166, 255);
            bgColor2 = new Color(74, 144, 226);
            borderColor = new Color(88, 166, 255, 200);
            textColor = Color.WHITE;
        } else {
            bgColor1 = new Color(52, 152, 219);
            bgColor2 = new Color(41, 128, 185);
            borderColor = new Color(255, 255, 255, 200);
            textColor = Color.WHITE;
        }

        GradientPaint gradient = new GradientPaint(0, 0, bgColor1, size, size, bgColor2);
        g2d.setPaint(gradient);
        g2d.fillOval(0, 0, size, size);

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawOval(1, 1, size-3, size-3);

        g2d.setColor(textColor);
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "👤";

        try {
            int x = (size - fm.stringWidth(text)) / 2;
            int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(text, x, y);
        } catch (Exception e) {

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
            fm = g2d.getFontMetrics();
            text = "U";
            int x = (size - fm.stringWidth(text)) / 2;
            int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(text, x, y);
        }

        g2d.dispose();
        return new ImageIcon(image);
    }

    private JPanel createModernSearchSection() {

        JPanel searchContainer = new JPanel(new BorderLayout(0, 10));
        searchContainer.setOpaque(false);

        JLabel searchTitle = new JLabel("Tìm kiếm sách");
        searchTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchTitle.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            searchTitle.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
        } else {
            searchTitle.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
        }

        JPanel searchInputPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor, borderColor;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    bgColor = DarkModeManager.DarkTheme.SECONDARY_BG;
                    borderColor = new Color(75, 85, 99);
                } else {
                    bgColor = Color.WHITE;
                    borderColor = new Color(206, 212, 218);
                }

                g2d.setColor(new Color(0, 0, 0, 10));
                g2d.fillRoundRect(2, 2, getWidth()-2, getHeight()-2, 14, 14);

                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                if (darkModeManager != null && darkModeManager.isDarkMode()) {

                    g2d.setColor(new Color(59, 130, 246, 30));
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                }

                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
            }
        };
        searchInputPanel.setOpaque(false);
        searchInputPanel.setPreferredSize(new Dimension(500, 48));
        searchInputPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 8));

        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            searchIcon.setForeground(DarkModeManager.DarkTheme.SECONDARY_TEXT);
        } else {
            searchIcon.setForeground(DarkModeManager.LightTheme.SECONDARY_TEXT);
        }

        txtSearch = new JTextField(30);
        txtSearch.setPreferredSize(new Dimension(450, 46));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setBorder(BorderFactory.createEmptyBorder(12, 5, 12, 12));
        txtSearch.setOpaque(false);
        txtSearch.setBackground(new Color(0, 0, 0, 0));

        String placeholder = "Nhập tên sách hoặc tác giả...";
        Color placeholderColor = new Color(156, 163, 175);
        txtSearch.setForeground(placeholderColor);
        txtSearch.setText(placeholder);

        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtSearch.getText().equals(placeholder)) {
                    txtSearch.setText("");

                    if (darkModeManager != null && darkModeManager.isDarkMode()) {
                        txtSearch.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
                    } else {
                        txtSearch.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtSearch.getText().isEmpty()) {
                    txtSearch.setForeground(placeholderColor);
                    txtSearch.setText(placeholder);
                }
            }
        });

        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setPreferredSize(new Dimension(110, 46));
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSearch.setFocusPainted(false);
        btnSearch.setBorder(BorderFactory.createEmptyBorder());
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));

        updateSearchButtonColors();

        searchInputPanel.add(searchIcon, BorderLayout.WEST);
        searchInputPanel.add(txtSearch, BorderLayout.CENTER);
        searchInputPanel.add(btnSearch, BorderLayout.EAST);

        searchContainer.add(searchTitle, BorderLayout.NORTH);
        searchContainer.add(searchInputPanel, BorderLayout.CENTER);

        return searchContainer;
    }

    private JPanel createModernFilterSection() {

        JPanel filterContainer = new JPanel(new BorderLayout(0, 10));
        filterContainer.setOpaque(false);

        JLabel filterTitle = new JLabel("Bộ lọc nâng cao");
        filterTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        filterTitle.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 0));

        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            filterTitle.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
        } else {
            filterTitle.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
        }

        JPanel filterPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bgColor, borderColor, shadowColor;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    bgColor = DarkModeManager.DarkTheme.CARD_BG;
                    borderColor = DarkModeManager.DarkTheme.BORDER_COLOR;
                    shadowColor = new Color(0, 0, 0, 20);
                } else {
                    bgColor = DarkModeManager.LightTheme.CARD_BG;
                    borderColor = DarkModeManager.LightTheme.BORDER_COLOR;
                    shadowColor = new Color(0, 0, 0, 8);
                }

                g2d.setColor(shadowColor);
                g2d.fillRoundRect(3, 3, getWidth()-3, getHeight()-3, 16, 16);

                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth()-3, getHeight()-3, 15, 15);

                if (darkModeManager != null && darkModeManager.isDarkMode()) {

                    g2d.setColor(new Color(88, 166, 255, 15));
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawRoundRect(1, 1, getWidth()-5, getHeight()-5, 15, 15);
                }

                g2d.setColor(borderColor);
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawRoundRect(0, 0, getWidth()-4, getHeight()-4, 15, 15);
            }
        };
        filterPanel.setOpaque(false);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblAuthor = createDarkModeLabel("Tác giả:");
        filterPanel.add(lblAuthor, gbc);

        gbc.gridx = 1;
        txtAuthor = createDarkModeTextField("Nhập tên tác giả...", 200);
        filterPanel.add(txtAuthor, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        JLabel lblPublisher = createDarkModeLabel("Nhà XB:");
        filterPanel.add(lblPublisher, gbc);

        gbc.gridx = 3;
        txtPublisher = createDarkModeTextField("Nhập nhà xuất bản...", 200);
        filterPanel.add(txtPublisher, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        JLabel lblCategory = createDarkModeLabel("Thể loại:");
        filterPanel.add(lblCategory, gbc);

        gbc.gridx = 5;
        cbCategory = createDarkModeComboBox(CATEGORIES);
        filterPanel.add(cbCategory, gbc);

        gbc.gridx = 6; gbc.gridy = 0;
        gbc.insets = new Insets(8, 20, 8, 10);
        JButton btnClearFilter = createDarkModeClearButton();
        filterPanel.add(btnClearFilter, gbc);

        filterContainer.add(filterTitle, BorderLayout.NORTH);
        filterContainer.add(filterPanel, BorderLayout.CENTER);

        return filterContainer;
    }

    private void updateSearchButtonColors() {
        if (btnSearch == null) return;

        for (MouseListener ml : btnSearch.getMouseListeners()) {
            btnSearch.removeMouseListener(ml);
        }

        if (darkModeManager != null && darkModeManager.isDarkMode()) {

            btnSearch.setBackground(DarkModeManager.DarkTheme.PRIMARY_ACCENT);
            btnSearch.setForeground(Color.WHITE);

            btnSearch.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btnSearch.setBackground(DarkModeManager.DarkTheme.HOVER_BORDER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    btnSearch.setBackground(DarkModeManager.DarkTheme.PRIMARY_ACCENT);
                }
            });
        } else {

            btnSearch.setBackground(DarkModeManager.LightTheme.PRIMARY_ACCENT);
            btnSearch.setForeground(Color.WHITE);

            btnSearch.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btnSearch.setBackground(DarkModeManager.LightTheme.HOVER_BORDER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    btnSearch.setBackground(DarkModeManager.LightTheme.PRIMARY_ACCENT);
                }
            });
        }
    }

    private void clearAllFilters() {
        txtAuthor.setText("Nhập tên tác giả...");
        txtAuthor.setForeground(new Color(156, 163, 175));

        txtPublisher.setText("Nhập nhà xuất bản...");
        txtPublisher.setForeground(new Color(156, 163, 175));

        cbCategory.setSelectedIndex(0);

        refreshBookDisplay();
    }

    private JTextField createDarkModeTextField(String placeholder, int width) {
        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(width, 42));
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        Color bgColor, borderColor, textColor;
        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            bgColor = DarkModeManager.DarkTheme.SURFACE_BG;
            borderColor = DarkModeManager.DarkTheme.BORDER_COLOR;
            textColor = DarkModeManager.DarkTheme.PRIMARY_TEXT;
        } else {
            bgColor = DarkModeManager.LightTheme.SURFACE_BG;
            borderColor = DarkModeManager.LightTheme.BORDER_COLOR;
            textColor = DarkModeManager.LightTheme.PRIMARY_TEXT;
        }

        textField.setBackground(bgColor);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        Color placeholderColor = new Color(156, 163, 175);
        textField.setForeground(placeholderColor);
        textField.setText(placeholder);

        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(textColor);
                }

                textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(darkModeManager != null && darkModeManager.isDarkMode()
                        ? DarkModeManager.DarkTheme.FOCUS_BORDER
                        : DarkModeManager.LightTheme.FOCUS_BORDER, 2),
                    BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(placeholderColor);
                    textField.setText(placeholder);
                }

                textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return textField;
    }

    private JComboBox<String> createDarkModeComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setPreferredSize(new Dimension(170, 42));
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            comboBox.setBackground(DarkModeManager.DarkTheme.SURFACE_BG);
            comboBox.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkModeManager.DarkTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
        } else {
            comboBox.setBackground(DarkModeManager.LightTheme.SURFACE_BG);
            comboBox.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkModeManager.LightTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
        }

        return comboBox;
    }

    private JButton createDarkModeClearButton() {
        JButton btnClearFilter = new JButton("Xóa bộ lọc");
        btnClearFilter.setPreferredSize(new Dimension(130, 42));
        btnClearFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClearFilter.setFocusPainted(false);
        btnClearFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color bgColor, textColor, hoverColor, borderColor;
        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            bgColor = DarkModeManager.DarkTheme.SURFACE_BG;
            textColor = DarkModeManager.DarkTheme.SECONDARY_TEXT;
            hoverColor = DarkModeManager.DarkTheme.HOVER_BG;
            borderColor = DarkModeManager.DarkTheme.BORDER_COLOR;
        } else {
            bgColor = new Color(248, 249, 250);
            textColor = new Color(73, 80, 87);
            hoverColor = new Color(233, 236, 239);
            borderColor = DarkModeManager.LightTheme.BORDER_COLOR;
        }

        btnClearFilter.setBackground(bgColor);
        btnClearFilter.setForeground(textColor);
        btnClearFilter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        btnClearFilter.addActionListener(e -> clearAllFilters());
        btnClearFilter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnClearFilter.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btnClearFilter.setBackground(bgColor);
            }
        });

        return btnClearFilter;
    }

    private JLabel createDarkModeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));

        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            label.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
        } else {
            label.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
        }

        return label;
    }

    private void updateComponentColors(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    label.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
                } else {
                    label.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
                }
            } else if (comp instanceof JTextField) {
                JTextField textField = (JTextField) comp;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    textField.setBackground(DarkModeManager.DarkTheme.SURFACE_BG);
                    if (!textField.getText().contains("Nhập")) {
                        textField.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
                    }
                } else {
                    textField.setBackground(DarkModeManager.LightTheme.SURFACE_BG);
                    if (!textField.getText().contains("Nhập")) {
                        textField.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
                    }
                }
            } else if (comp instanceof JComboBox) {
                JComboBox<?> comboBox = (JComboBox<?>) comp;
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    comboBox.setBackground(DarkModeManager.DarkTheme.SURFACE_BG);
                    comboBox.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
                } else {
                    comboBox.setBackground(DarkModeManager.LightTheme.SURFACE_BG);
                    comboBox.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
                }
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;

                if (button.getText() != null && button.getText().contains("Xóa bộ lọc")) {
                    updateClearFilterButtonColors(button);
                }
            } else if (comp instanceof Container) {
                updateComponentColors((Container) comp);
            }
        }
    }

    private void updateClearFilterButtonColors(JButton button) {
        Color bgColor, textColor, borderColor;
        if (darkModeManager != null && darkModeManager.isDarkMode()) {
            bgColor = DarkModeManager.DarkTheme.SURFACE_BG;
            textColor = DarkModeManager.DarkTheme.SECONDARY_TEXT;
            borderColor = DarkModeManager.DarkTheme.BORDER_COLOR;
        } else {
            bgColor = new Color(248, 249, 250);
            textColor = new Color(73, 80, 87);
            borderColor = DarkModeManager.LightTheme.BORDER_COLOR;
        }

        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        button.repaint();
    }

    private void loadUserAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            System.out.println("⚡ Avatar URL is null/empty, using default");
            setDefaultAvatar();
            return;
        }

        System.out.println("🖼️ Loading avatar from: " + avatarUrl);

        new Thread(() -> {
            try {
                ImageIcon icon = null;

                if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
                    System.out.println("🌐 Loading from web URL: " + avatarUrl);
                    URL url = new URL(avatarUrl);
                    icon = new ImageIcon(url);
                } else if (avatarUrl.startsWith("file:")) {
                    System.out.println("📁 Loading from file URL: " + avatarUrl);
                    URL url = new URL(avatarUrl);
                    icon = new ImageIcon(url);
                } else if (avatarUrl.startsWith("/") || avatarUrl.contains(":\\")) {
                    System.out.println("💾 Loading from local path: " + avatarUrl);

                    java.io.File file = new java.io.File(avatarUrl);
                    if (file.exists()) {
                        icon = new ImageIcon(avatarUrl);
                    } else {
                        System.out.println("❌ Local file not found: " + avatarUrl);
                        SwingUtilities.invokeLater(() -> setDefaultAvatar());
                        return;
                    }
                } else {

                    System.out.println("🌐 Assuming web URL (trying https first): " + avatarUrl);
                    try {
                        URL url = new URL("https://" + avatarUrl);
                        icon = new ImageIcon(url);
                    } catch (Exception e1) {
                        System.out.println("🔄 HTTPS failed, trying with http: " + avatarUrl);
                        URL url = new URL("http://" + avatarUrl);
                        icon = new ImageIcon(url);
                    }
                }

                if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
                    System.out.println("❌ Failed to load image or invalid dimensions");
                    SwingUtilities.invokeLater(() -> setDefaultAvatar());
                    return;
                }

                System.out.println("✅ Image loaded successfully: " + icon.getIconWidth() + "x" + icon.getIconHeight());

                final int AVATAR_SIZE = 32;
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);

                java.awt.image.BufferedImage circularImage = new java.awt.image.BufferedImage(
                    AVATAR_SIZE, AVATAR_SIZE, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = circularImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, AVATAR_SIZE, AVATAR_SIZE));
                g2d.drawImage(scaledImg, 0, 0, null);

                g2d.setClip(null);
                if (darkModeManager != null && darkModeManager.isDarkMode()) {
                    g2d.setColor(new Color(88, 166, 255, 200));
                } else {
                    g2d.setColor(new Color(0, 123, 255, 200));
                }
                g2d.setStroke(new BasicStroke(2.5f));
                g2d.drawOval(1, 1, AVATAR_SIZE-3, AVATAR_SIZE-3);

                g2d.dispose();

                final ImageIcon finalIcon = new ImageIcon(circularImage);

                SwingUtilities.invokeLater(() -> {
                    lblAvatar.setIcon(finalIcon);
                    lblAvatar.setToolTipText("Click để xem thông tin cá nhân");
                    lblAvatar.revalidate();
                    lblAvatar.repaint();

                    if (lblAvatar.getParent() != null) {
                        lblAvatar.getParent().revalidate();
                        lblAvatar.getParent().repaint();
                    }

                    System.out.println("🎉 Avatar set successfully!");
                });

            } catch (Exception e) {
                System.err.println("💥 Error loading avatar from URL '" + avatarUrl + "': " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> setDefaultAvatar());
            }
        }).start();
    }

    private void loadUserAvatarFromDB(int userId) {
        System.out.println("🔍 Loading avatar for user ID: " + userId);

        new Thread(() -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String sql = "SELECT username, avatar FROM users WHERE id = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String username = rs.getString("username");
                    String avatarUrl = rs.getString("avatar");
                    System.out.println("📸 Found user: " + username + ", avatar: " +
                        (avatarUrl != null && !avatarUrl.isEmpty() ? avatarUrl : "null/empty"));

                    SwingUtilities.invokeLater(() -> {
                        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                            loadUserAvatar(avatarUrl.trim());
                        } else {
                            System.out.println("⚡ Using default avatar for user: " + username);
                            setDefaultAvatar();
                        }
                    });
                } else {
                    System.out.println("❌ No user found with ID: " + userId);
                    SwingUtilities.invokeLater(() -> setDefaultAvatar());
                }

                rs.close();
                pstmt.close();
            } catch (Exception e) {
                System.err.println("💥 Error loading user avatar from database: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> setDefaultAvatar());
            }
        }).start();
    }

    private void showImportantNotificationsOnLogin() {

        SwingUtilities.invokeLater(() -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

                String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'Nhắc nhở' AND is_read = 0";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    int count = rs.getInt(1);

                    int result = JOptionPane.showConfirmDialog(this,
                        String.format("Bạn có %d thông báo quan trọng về việc trả sách!\nBạn có muốn xem ngay không?", count),
                        "Thông báo quan trọng",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                    if (result == JOptionPane.YES_OPTION) {
                        new NotificationUI(userId).setVisible(true);
                    }
                }

                rs.close();
                pstmt.close();

            } catch (SQLException e) {
                System.err.println("Error checking important notifications: " + e.getMessage());
            }
        });
    }

}
