package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BorrowRequestManagerUI extends JPanel {
    private JTable requestsTable;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbStatus;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public BorrowRequestManagerUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 249, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        connectToServer();
        createComponents();
        loadBorrowRequests();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            in.readLine();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối server: " + e.getMessage());
        }
    }

    private void createComponents() {

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);

        JPanel statsPanel = createStatsPanel();

        JPanel buttonPanel = createButtonPanel();

        bottomPanel.add(statsPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statsPanel.setOpaque(false);

        JLabel totalLabel = new JLabel("Tổng: 0");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalLabel.setForeground(new Color(0, 123, 255));

        JLabel pendingLabel = new JLabel("Chờ duyệt: 0");
        pendingLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pendingLabel.setForeground(new Color(255, 193, 7));

        JLabel approvedLabel = new JLabel("Đã duyệt: 0");
        approvedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        approvedLabel.setForeground(new Color(40, 167, 69));

        JLabel rejectedLabel = new JLabel("Đã từ chối: 0");
        rejectedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rejectedLabel.setForeground(new Color(220, 53, 69));

        statsPanel.add(totalLabel);
        statsPanel.add(new JLabel("|"));
        statsPanel.add(pendingLabel);
        statsPanel.add(new JLabel("|"));
        statsPanel.add(approvedLabel);
        statsPanel.add(new JLabel("|"));
        statsPanel.add(rejectedLabel);

        return statsPanel;
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(15, 15));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Quản lý đăng ký mượn sách");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 123, 255));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        txtSearch = new JTextField(20);
        txtSearch.setPreferredSize(new Dimension(250, 35));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                loadBorrowRequests();
            }
        });

        JLabel statusLabel = new JLabel("Trạng thái:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        String[] statusDisplayOptions = {"Tất cả", "Chờ duyệt", "Đã duyệt", "Đã từ chối"};
        cbStatus = new JComboBox<>(statusDisplayOptions);
        cbStatus.setSelectedItem("Tất cả");
        cbStatus.setPreferredSize(new Dimension(140, 35));
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStatus.setBackground(Color.WHITE);

        cbStatus.addActionListener(e -> loadBorrowRequests());

        JButton btnSearch = new JButton("Tìm kiếm");
        btnSearch.setPreferredSize(new Dimension(100, 35));
        btnSearch.setBackground(new Color(0, 123, 255));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> loadBorrowRequests());

        JButton btnRefresh = new JButton("Làm mới");
        btnRefresh.setPreferredSize(new Dimension(120, 35));
        btnRefresh.setBackground(new Color(40, 167, 69));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setFocusPainted(false);
        btnRefresh.addActionListener(e -> {

            txtSearch.setText("");
            cbStatus.setSelectedItem("Tất cả");
            loadBorrowRequests();
        });

        searchPanel.add(searchLabel);
        searchPanel.add(txtSearch);
        searchPanel.add(statusLabel);
        searchPanel.add(cbStatus);
        searchPanel.add(btnSearch);
        searchPanel.add(btnRefresh);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(searchPanel, BorderLayout.CENTER);

        return headerPanel;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);

        String[] columnNames = {
            "ID", "Người dùng", "Tên sách", "Tác giả",
            "Ngày đăng ký", "Trạng thái", "Ghi chú"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestsTable = new JTable(tableModel);
        requestsTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        requestsTable.setRowHeight(45);
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsTable.setSelectionBackground(new Color(0, 123, 255, 50));
        requestsTable.setSelectionForeground(new Color(0, 123, 255));
        requestsTable.setGridColor(new Color(222, 226, 230));
        requestsTable.setShowGrid(true);

        JTableHeader header = requestsTable.getTableHeader();
        header.setBackground(new Color(0, 123, 255));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 50));

        requestsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        requestsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        requestsTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        requestsTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        requestsTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        requestsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        requestsTable.getColumnModel().getColumn(6).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(requestsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0, 123, 255), 2));
        scrollPane.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        buttonPanel.setOpaque(false);

        JButton btnApprove = new JButton("Duyệt đăng ký");
        btnApprove.setPreferredSize(new Dimension(150, 40));
        btnApprove.setBackground(new Color(40, 167, 69));
        btnApprove.setForeground(Color.WHITE);
        btnApprove.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnApprove.setFocusPainted(false);
        btnApprove.addActionListener(e -> approveRequest());

        JButton btnReject = new JButton("Từ chối");
        btnReject.setPreferredSize(new Dimension(120, 40));
        btnReject.setBackground(new Color(220, 53, 69));
        btnReject.setForeground(Color.WHITE);
        btnReject.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnReject.setFocusPainted(false);
        btnReject.addActionListener(e -> rejectRequest());

        JButton btnDetails = new JButton("Chi tiết");
        btnDetails.setPreferredSize(new Dimension(120, 40));
        btnDetails.setBackground(new Color(255, 193, 7));
        btnDetails.setForeground(Color.WHITE);
        btnDetails.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDetails.setFocusPainted(false);
        btnDetails.addActionListener(e -> viewRequestDetails());

        buttonPanel.add(btnDetails);
        buttonPanel.add(btnApprove);
        buttonPanel.add(btnReject);

        return buttonPanel;
    }

    private void loadBorrowRequests() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            StringBuilder query = new StringBuilder(
                "SELECT br.id, u.username, b.title, b.author, br.request_date, br.status, br.admin_notes " +
                "FROM borrow_requests br " +
                "JOIN users u ON br.user_id = u.id " +
                "JOIN books b ON br.book_id = b.id " +
                "WHERE 1=1"
            );

            String searchText = txtSearch.getText().trim();
            if (!searchText.isEmpty()) {
                query.append(" AND (u.username LIKE ? OR b.title LIKE ? OR b.author LIKE ?)");
            }

            String statusFilter = cbStatus.getSelectedItem().toString();
            String actualStatus = null;

            switch (statusFilter) {
                case "Chờ duyệt":
                    actualStatus = "PENDING";
                    break;
                case "Đã duyệt":
                    actualStatus = "APPROVED";
                    break;
                case "Đã từ chối":
                    actualStatus = "REJECTED";
                    break;
                case "Tất cả":
                default:
                    actualStatus = null;
                    break;
            }

            if (actualStatus != null) {
                query.append(" AND br.status = ?");
            }

            query.append(" ORDER BY br.request_date DESC");

            PreparedStatement ps = conn.prepareStatement(query.toString());
            int paramIndex = 1;

            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText + "%";
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, searchPattern);
                ps.setString(paramIndex++, searchPattern);
            }

            if (actualStatus != null) {
                ps.setString(paramIndex++, actualStatus);
            }

            ResultSet rs = ps.executeQuery();

            tableModel.setRowCount(0);

            int totalCount = 0;
            int pendingCount = 0;
            int approvedCount = 0;
            int rejectedCount = 0;

            while (rs.next()) {
                String status = rs.getString("status");
                String statusDisplay;
                switch (status) {
                    case "PENDING":
                        statusDisplay = "Chờ duyệt";
                        pendingCount++;
                        break;
                    case "APPROVED":
                        statusDisplay = "Đã duyệt";
                        approvedCount++;
                        break;
                    case "REJECTED":
                        statusDisplay = "Đã từ chối";
                        rejectedCount++;
                        break;
                    default:
                        statusDisplay = status;
                        break;
                }

                totalCount++;

                Object[] row = {
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("request_date"),
                    statusDisplay,
                    rs.getString("admin_notes") != null ? rs.getString("admin_notes") : ""
                };
                tableModel.addRow(row);
            }

            updateStatistics(totalCount, pendingCount, approvedCount, rejectedCount);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    private void updateStatistics(int total, int pending, int approved, int rejected) {

        updateStatisticsInPanel(this, total, pending, approved, rejected);
    }

    private void updateStatisticsInPanel(java.awt.Container container, int total, int pending, int approved, int rejected) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                String text = label.getText();
                if (text.startsWith("Tổng:")) {
                    label.setText("Tổng: " + total);
                } else if (text.startsWith("Chờ duyệt:")) {
                    label.setText("Chờ duyệt: " + pending);
                } else if (text.startsWith("Đã duyệt:")) {
                    label.setText("Đã duyệt: " + approved);
                } else if (text.startsWith("Đã từ chối:")) {
                    label.setText("Đã từ chối: " + rejected);
                }
            } else if (comp instanceof java.awt.Container) {
                updateStatisticsInPanel((java.awt.Container) comp, total, pending, approved, rejected);
            }
        }
    }

    private void approveRequest() {
        int selectedRows = requestsTable.getSelectedRow();
        if (selectedRows == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đăng ký để duyệt!");
            return;
        }

        int requestId = (Integer) tableModel.getValueAt(selectedRows, 0);
        String username = (String) tableModel.getValueAt(selectedRows, 1);
        String bookTitle = (String) tableModel.getValueAt(selectedRows, 2);

        int choice = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn duyệt đăng ký mượn sách:\n" +
            "Người dùng: " + username + "\n" +
            "Sách: " + bookTitle,
            "Xác nhận duyệt",
            JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            String notes = JOptionPane.showInputDialog(this,
                "Ghi chú (tùy chọn):",
                "Ghi chú duyệt đăng ký",
                JOptionPane.PLAIN_MESSAGE);

            updateRequestStatus(requestId, "APPROVED", notes != null ? notes : "Đã được duyệt");
        }
    }

    private void rejectRequest() {
        int selectedRows = requestsTable.getSelectedRow();
        if (selectedRows == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đăng ký để từ chối!");
            return;
        }

        int requestId = (Integer) tableModel.getValueAt(selectedRows, 0);
        String username = (String) tableModel.getValueAt(selectedRows, 1);
        String bookTitle = (String) tableModel.getValueAt(selectedRows, 2);

        String reason = JOptionPane.showInputDialog(this,
            "Lý do từ chối đăng ký của " + username + " cho sách '" + bookTitle + "':",
            "Lý do từ chối",
            JOptionPane.PLAIN_MESSAGE);

        if (reason != null && !reason.trim().isEmpty()) {
            updateRequestStatus(requestId, "REJECTED", reason);
        }
    }

    private void updateRequestStatus(int requestId, String status, String notes) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String selectQuery = "SELECT br.user_id, b.title " +
                "FROM borrow_requests br " +
                "JOIN books b ON br.book_id = b.id " +
                "WHERE br.id = ?";
            PreparedStatement selectPs = conn.prepareStatement(selectQuery);
            selectPs.setInt(1, requestId);
            ResultSet rs = selectPs.executeQuery();

            int userId = -1;
            String bookTitle = "";
            if (rs.next()) {
                userId = rs.getInt("user_id");
                bookTitle = rs.getString("title");
            }

            String updateQuery = "UPDATE borrow_requests " +
                "SET status = ?, admin_notes = ?, approved_date = ? " +
                "WHERE id = ?";

            PreparedStatement ps = conn.prepareStatement(updateQuery);
            ps.setString(1, status);
            ps.setString(2, notes);
            ps.setString(3, LocalDateTime.now().toString());
            ps.setInt(4, requestId);

            int rowsUpdated = ps.executeUpdate();

            if (rowsUpdated > 0) {

                if (userId != -1) {
                    if ("APPROVED".equals(status)) {
                        NotificationUI.addNotification(
                            userId,
                            "borrow_approved",
                            "Yêu cầu mượn sách được duyệt",
                            "Yêu cầu mượn sách '" + bookTitle + "' của bạn đã được chấp nhận. " + notes
                        );

                        createBorrowRecord(requestId);
                    } else if ("REJECTED".equals(status)) {
                        NotificationUI.addNotification(
                            userId,
                            "borrow_rejected",
                            "Yêu cầu mượn sách bị từ chối",
                            "Yêu cầu mượn sách '" + bookTitle + "' của bạn đã bị từ chối. Lý do: " + notes
                        );
                    }
                }

                JOptionPane.showMessageDialog(this,
                    "Đã cập nhật trạng thái đăng ký thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
                loadBorrowRequests();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể cập nhật trạng thái!");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật: " + e.getMessage());
        }
    }

    private void createBorrowRecord(int requestId) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String getRequestQuery = "SELECT user_id, book_id, expected_return_date FROM borrow_requests WHERE id = ?";
            PreparedStatement ps1 = conn.prepareStatement(getRequestQuery);
            ps1.setInt(1, requestId);
            ResultSet rs = ps1.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                int bookId = rs.getInt("book_id");
                String expectedReturnDate = rs.getString("expected_return_date");

                try {
                    conn.createStatement().execute("ALTER TABLE borrows ADD COLUMN expected_return_date TEXT");
                } catch (Exception e) {

                }

                String insertBorrowQuery = "INSERT INTO borrows (user_id, book_id, borrow_date, expected_return_date) " +
                    "VALUES (?, ?, ?, ?)";

                PreparedStatement ps2 = conn.prepareStatement(insertBorrowQuery);
                ps2.setInt(1, userId);
                ps2.setInt(2, bookId);
                ps2.setString(3, LocalDateTime.now().toString());
                ps2.setString(4, expectedReturnDate);
                ps2.executeUpdate();

                String updateBookQuery = "UPDATE books SET quantity = quantity - 1 WHERE id = ? AND quantity > 0";
                PreparedStatement ps3 = conn.prepareStatement(updateBookQuery);
                ps3.setInt(1, bookId);
                ps3.executeUpdate();

                System.out.println("✅ Created borrow record with expected return date: " + expectedReturnDate);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tạo bản ghi mượn sách: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewRequestDetails() {
        int selectedRow = requestsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đăng ký để xem chi tiết!");
            return;
        }

        int requestId = (Integer) tableModel.getValueAt(selectedRow, 0);
        showRequestDetailsDialog(requestId);
    }

    private void showRequestDetailsDialog(int requestId) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "SELECT br.*, u.username, u.email, b.title, b.author, b.category, b.quantity " +
                "FROM borrow_requests br " +
                "JOIN users u ON br.user_id = u.id " +
                "JOIN books b ON br.book_id = b.id " +
                "WHERE br.id = ?";

            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "Chi tiết đăng ký mượn sách", true);
                dialog.setSize(600, 500);
                dialog.setLocationRelativeTo(this);

                JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                mainPanel.setBackground(new Color(248, 249, 250));

                JLabel headerLabel = new JLabel("Chi tiết đăng ký mượn sách", SwingConstants.CENTER);
                headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                headerLabel.setForeground(new Color(0, 123, 255));

                JPanel contentPanel = new JPanel();
                contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
                contentPanel.setOpaque(false);

                addDetailRow(contentPanel, "ID đăng ký:", String.valueOf(rs.getInt("id")));
                addDetailRow(contentPanel, "Người dùng:", rs.getString("username"));
                addDetailRow(contentPanel, "Email:", rs.getString("email"));
                addDetailRow(contentPanel, "Tên sách:", rs.getString("title"));
                addDetailRow(contentPanel, "Tác giả:", rs.getString("author"));
                addDetailRow(contentPanel, "Thể loại:", rs.getString("category"));
                addDetailRow(contentPanel, "Số lượng còn lại:", String.valueOf(rs.getInt("quantity")));
                addDetailRow(contentPanel, "Ngày đăng ký:", rs.getString("request_date"));
                addDetailRow(contentPanel, "Trạng thái:", rs.getString("status"));
                addDetailRow(contentPanel, "Ghi chú admin:",
                    rs.getString("admin_notes") != null ? rs.getString("admin_notes") : "Chưa có");

                JButton closeBtn = new JButton("Đóng");
                closeBtn.setPreferredSize(new Dimension(100, 40));
                closeBtn.setBackground(new Color(108, 117, 125));
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
                closeBtn.setFocusPainted(false);
                closeBtn.addActionListener(e -> dialog.dispose());

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.setOpaque(false);
                buttonPanel.add(closeBtn);

                mainPanel.add(headerLabel, BorderLayout.NORTH);
                mainPanel.add(contentPanel, BorderLayout.CENTER);
                mainPanel.add(buttonPanel, BorderLayout.SOUTH);

                dialog.add(mainPanel);
                dialog.setVisible(true);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải chi tiết: " + e.getMessage());
        }
    }

    private void addDetailRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        row.setOpaque(false);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblLabel.setPreferredSize(new Dimension(150, 25));
        lblLabel.setForeground(new Color(52, 58, 64));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblValue.setForeground(new Color(73, 80, 87));

        row.add(lblLabel);
        row.add(lblValue);

        parent.add(row);
    }
}
