package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationUI extends JFrame {
    private int userId;
    private DefaultTableModel tableModel;
    private JTable notificationTable;
    private JLabel unreadCountLabel;

    public NotificationUI(int userId) {
        this.userId = userId;
        setTitle("Thông báo");
        setSize(1900, 1000);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        createNotificationInterface();
        loadNotifications();
    }

    private void createNotificationInterface() {

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color startColor = new Color(255, 248, 240);
                Color endColor = new Color(255, 240, 230);
                GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = createHeaderPanel();

        JPanel tablePanel = createTablePanel();

        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel("Thông báo của bạn", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(255, 140, 0));

        unreadCountLabel = new JLabel("", SwingConstants.RIGHT);
        unreadCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        unreadCountLabel.setForeground(new Color(220, 53, 69));

        panel.add(titleLabel, BorderLayout.CENTER);
        panel.add(unreadCountLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        String[] columnNames = {"ID", "Loại", "Tiêu đề", "Nội dung", "Thời gian", "Trạng thái"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        notificationTable = new JTable(tableModel);
        notificationTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notificationTable.setRowHeight(60);
        notificationTable.setSelectionBackground(new Color(255, 140, 0, 50));
        notificationTable.setSelectionForeground(new Color(255, 140, 0));
        notificationTable.setGridColor(new Color(222, 226, 230));
        notificationTable.setShowGrid(true);
        notificationTable.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = notificationTable.getTableHeader();
        header.setBackground(new Color(255, 140, 0));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));

        notificationTable.getColumnModel().getColumn(0).setMinWidth(0);
        notificationTable.getColumnModel().getColumn(0).setMaxWidth(0);
        notificationTable.getColumnModel().getColumn(0).setWidth(0);

        notificationTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        notificationTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        notificationTable.getColumnModel().getColumn(3).setPreferredWidth(300);
        notificationTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        notificationTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        notificationTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    markAsRead();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(notificationTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 140, 0), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panel.setOpaque(false);

        JButton markReadBtn = new JButton("Đánh dấu đã đọc");
        markReadBtn.setPreferredSize(new Dimension(160, 40));
        markReadBtn.setBackground(new Color(40, 167, 69));
        markReadBtn.setForeground(Color.WHITE);
        markReadBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        markReadBtn.setFocusPainted(false);
        markReadBtn.addActionListener(e -> markAsRead());

        JButton markAllReadBtn = new JButton("Đọc tất cả");
        markAllReadBtn.setPreferredSize(new Dimension(120, 40));
        markAllReadBtn.setBackground(new Color(23, 162, 184));
        markAllReadBtn.setForeground(Color.WHITE);
        markAllReadBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        markAllReadBtn.setFocusPainted(false);
        markAllReadBtn.addActionListener(e -> markAllAsRead());

        JButton deleteBtn = new JButton("Xóa");
        deleteBtn.setPreferredSize(new Dimension(100, 40));
        deleteBtn.setBackground(new Color(220, 53, 69));
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteBtn.setFocusPainted(false);
        deleteBtn.addActionListener(e -> deleteNotification());

        JButton refreshBtn = new JButton("Làm mới");
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.setBackground(new Color(108, 117, 125));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadNotifications());

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setPreferredSize(new Dimension(100, 40));
        closeBtn.setBackground(new Color(108, 117, 125));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());

        panel.add(markReadBtn);
        panel.add(markAllReadBtn);
        panel.add(deleteBtn);
        panel.add(refreshBtn);
        panel.add(closeBtn);

        return panel;
    }

    private void loadNotifications() {
        tableModel.setRowCount(0);
        int unreadCount = 0;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "SELECT id, type, title, content, created_at, is_read FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                boolean isRead = rs.getBoolean("is_read");
                if (!isRead) unreadCount++;

                String type = getNotificationTypeIcon(rs.getString("type"));
                String status = isRead ? "Đã đọc" : "Chưa đọc";

                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    type,
                    rs.getString("title"),
                    rs.getString("content"),
                    formatDateTime(rs.getString("created_at")),
                    status
                });
            }

            if (unreadCount > 0) {
                unreadCountLabel.setText(unreadCount + " thông báo chưa đọc");
            } else {
                unreadCountLabel.setText("Tất cả đã đọc");
                unreadCountLabel.setForeground(new Color(40, 167, 69));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi tải thông báo: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getNotificationTypeIcon(String type) {
        switch (type != null ? type.toLowerCase() : "") {
            case "borrow_approved": return "Duyệt";
            case "borrow_rejected": return "Từ chối";
            case "borrow_request": return "Mượn sách";
            case "new_book": return "Sách mới";
            case "reminder": return "Nhắc nhở";
            case "system": return "Hệ thống";
            default: return "Thông báo";
        }
    }

    private String formatDateTime(String dateTimeStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
            return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return dateTimeStr;
        }
    }

    private void markAsRead() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn một thông báo để đánh dấu đã đọc!",
                "Chưa chọn",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int notificationId = (Integer) tableModel.getValueAt(selectedRow, 0);
        updateReadStatus(notificationId, true);
        loadNotifications();
    }

    private void markAllAsRead() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Đánh dấu tất cả thông báo là đã đọc?",
            "Xác nhận",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String query = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, userId);
                ps.executeUpdate();

                loadNotifications();
                JOptionPane.showMessageDialog(this,
                    "Đã đánh dấu tất cả thông báo là đã đọc!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Lỗi: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteNotification() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng chọn một thông báo để xóa!",
                "Chưa chọn",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc muốn xóa thông báo này?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            int notificationId = (Integer) tableModel.getValueAt(selectedRow, 0);

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String query = "DELETE FROM notifications WHERE id = ? AND user_id = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, notificationId);
                ps.setInt(2, userId);
                ps.executeUpdate();

                loadNotifications();

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Lỗi xóa thông báo: " + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateReadStatus(int notificationId, boolean isRead) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "UPDATE notifications SET is_read = ? WHERE id = ? AND user_id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setBoolean(1, isRead);
            ps.setInt(2, notificationId);
            ps.setInt(3, userId);
            ps.executeUpdate();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi cập nhật trạng thái: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public static int getUnreadNotificationCount(int userId) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void addNotification(int userId, String type, String title, String content) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "INSERT INTO notifications (user_id, type, title, content, created_at, is_read) VALUES (?, ?, ?, ?, ?, 0)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, title);
            ps.setString(4, content);
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error adding notification: " + e.getMessage());
        }
    }
}
