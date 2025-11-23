package client;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.net.URL;

public class UserDetailUI extends JFrame {
    private String userId;
    private JLabel lblName, lblEmail, lblPhone, lblJoinDate, lblBorrowCount;
    private JTable borrowTable;
    private DefaultTableModel borrowModel;
    private JLabel lblAvatar;
    private JButton btnLock;

    public UserDetailUI(String userId) {
        this.userId = userId;
        setTitle("Thông tin chi tiết người dùng");
        setMinimumSize(new Dimension(900, 700));
        setPreferredSize(new Dimension(1900, 1000));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(248, 250, 252), 0, h, new Color(238, 242, 247));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new BorderLayout(0, 25));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        setContentPane(mainPanel);

        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel infoPanel = createInfoPanel();

        JPanel historyPanel = createHistoryPanel();

        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setOpaque(false);
        contentPanel.add(infoPanel, BorderLayout.NORTH);
        contentPanel.add(historyPanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        loadUserData();

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("Thông tin chi tiết người dùng", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(31, 81, 135));

        JLabel subtitleLabel = new JLabel("Quản lý thông tin và hoạt động mượn sách", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(108, 117, 125));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitleLabel);

        headerPanel.add(titlePanel, BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(25, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 1),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 235), 1),
                BorderFactory.createMatteBorder(0, 0, 2, 2, new Color(240, 242, 247))
            ),
            BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        JPanel avatarPanel = createAvatarPanel();
        panel.add(avatarPanel, BorderLayout.WEST);

        JPanel infoFields = createInfoFields();
        panel.add(infoFields, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAvatarPanel() {
        JPanel avatarPanel = new JPanel();
        avatarPanel.setLayout(new BoxLayout(avatarPanel, BoxLayout.Y_AXIS));
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(150, 180));

        lblAvatar = new JLabel();
        lblAvatar.setPreferredSize(new Dimension(120, 140));
        lblAvatar.setMaximumSize(new Dimension(120, 140));
        lblAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatar.setVerticalAlignment(SwingConstants.CENTER);
        lblAvatar.setText("Ảnh đại diện");
        lblAvatar.setOpaque(true);
        lblAvatar.setBackground(new Color(248, 250, 252));
        lblAvatar.setForeground(new Color(108, 117, 125));
        lblAvatar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAvatar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        lblAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarPanel.add(Box.createVerticalStrut(10));
        avatarPanel.add(lblAvatar);
        avatarPanel.add(Box.createVerticalGlue());

        return avatarPanel;
    }

    private JPanel createInfoFields() {
        JPanel infoFields = new JPanel();
        infoFields.setLayout(new GridBagLayout());
        infoFields.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 0, 8, 20);

        gbc.gridx = 0; gbc.gridy = 0;
        infoFields.add(createFieldLabel("Họ và tên:"), gbc);
        gbc.gridx = 1;
        lblName = createFieldValue("");
        infoFields.add(lblName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        infoFields.add(createFieldLabel("Số điện thoại:"), gbc);
        gbc.gridx = 1;
        lblPhone = createFieldValue("");
        infoFields.add(lblPhone, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        infoFields.add(createFieldLabel("Email:"), gbc);
        gbc.gridx = 1;
        lblEmail = createFieldValue("");
        infoFields.add(lblEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        infoFields.add(createFieldLabel("Số sách đang mượn:"), gbc);
        gbc.gridx = 1;
        lblBorrowCount = createFieldValue("0");
        infoFields.add(lblBorrowCount, gbc);

        return infoFields;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(52, 58, 64));
        label.setPreferredSize(new Dimension(140, 25));
        return label;
    }

    private JLabel createFieldValue(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(73, 80, 87));
        label.setPreferredSize(new Dimension(250, 25));
        return label;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 235), 1),
                BorderFactory.createMatteBorder(0, 0, 2, 2, new Color(240, 242, 247))
            ),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel headerLabel = new JLabel("Lịch sử mượn sách");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerLabel.setForeground(new Color(31, 81, 135));

        JLabel countLabel = new JLabel("Hiển thị tất cả sách đã và đang mượn");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(new Color(108, 117, 125));

        JPanel headerInfo = new JPanel();
        headerInfo.setLayout(new BoxLayout(headerInfo, BoxLayout.Y_AXIS));
        headerInfo.setOpaque(false);
        headerInfo.add(headerLabel);
        headerInfo.add(Box.createVerticalStrut(2));
        headerInfo.add(countLabel);

        headerPanel.add(headerInfo, BorderLayout.WEST);
        panel.add(headerPanel, BorderLayout.NORTH);

        String[] columns = {"Tên sách", "Tác giả", "Nhà XB", "Năm XB", "Thể loại", "Ngày mượn", "Ngày trả", "Trạng thái"};
        borrowModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        borrowTable = new JTable(borrowModel);
        borrowTable.setRowHeight(35);
        borrowTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        borrowTable.setGridColor(new Color(240, 242, 247));
        borrowTable.setShowGrid(true);
        borrowTable.setIntercellSpacing(new Dimension(1, 1));
        borrowTable.setSelectionBackground(new Color(220, 235, 255));
        borrowTable.setSelectionForeground(new Color(31, 81, 135));

        borrowTable.getTableHeader().setBackground(new Color(31, 81, 135));
        borrowTable.getTableHeader().setForeground(Color.WHITE);
        borrowTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        borrowTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        borrowTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        borrowTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        borrowTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        borrowTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        borrowTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        borrowTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        borrowTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        borrowTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        borrowTable.getColumnModel().getColumn(7).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(borrowTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 235), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        JButton btnBack = createModernButton("Quay lại", new Color(108, 117, 125), Color.WHITE);
        btnBack.addActionListener(e -> dispose());
        leftPanel.add(btnBack);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        btnLock = createModernButton("Khóa tài khoản", new Color(220, 53, 69), Color.WHITE);
        btnLock.addActionListener(e -> lockAccount());
        rightPanel.add(btnLock);

        bottomPanel.add(leftPanel, BorderLayout.WEST);
        bottomPanel.add(rightPanel, BorderLayout.EAST);

        return bottomPanel;
    }

    private JButton createModernButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(140, 40));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            private Color originalColor = button.getBackground();

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (originalColor.equals(new Color(108, 117, 125))) {
                    button.setBackground(new Color(90, 98, 104));
                } else if (originalColor.equals(new Color(220, 53, 69))) {
                    button.setBackground(new Color(200, 35, 51));
                } else if (originalColor.equals(new Color(40, 167, 69))) {
                    button.setBackground(new Color(33, 136, 56));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });

        return button;
    }

    private void loadUserData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {

            try {
                Statement stmt = conn.createStatement();
                stmt.execute("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'active'");
            } catch (SQLException e) {

            }

            PreparedStatement ps = conn.prepareStatement(
                "SELECT username, email, phone, avatar, status FROM users WHERE id = ?");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblName.setText(rs.getString("username"));
                lblEmail.setText(rs.getString("email"));
                lblPhone.setText(rs.getString("phone"));

                String status = rs.getString("status");
                if ("locked".equals(status)) {
                    btnLock.setText("Mở khóa tài khoản");
                    btnLock.setBackground(new Color(40, 167, 69));
                    setTitle("Thông tin chi tiết người dùng - [TÀI KHOẢN BỊ KHÓA]");
                } else {
                    btnLock.setText("Khóa tài khoản");
                    btnLock.setBackground(new Color(220, 53, 69));
                    setTitle("Thông tin chi tiết người dùng");
                }

                String avatarUrl = rs.getString("avatar");
                if (avatarUrl != null && avatarUrl.startsWith("http")) {
                    try {
                        ImageIcon icon = new ImageIcon(new URL(avatarUrl));
                        Image img = icon.getImage().getScaledInstance(100, 120, Image.SCALE_SMOOTH);
                        lblAvatar.setIcon(new ImageIcon(img));
                        lblAvatar.setText("");
                    } catch (Exception ex) {
                        lblAvatar.setIcon(null);
                        lblAvatar.setText("avata");
                    }
                }
            }

            PreparedStatement psBorrow = conn.prepareStatement(
                "SELECT COUNT(*) FROM borrows WHERE user_id=? AND return_date IS NULL");
            psBorrow.setString(1, userId);
            ResultSet rsBorrow = psBorrow.executeQuery();
            if (rsBorrow.next()) {
                lblBorrowCount.setText(rsBorrow.getString(1));
            }

            ps = conn.prepareStatement(
                "SELECT bk.title, bk.author, bk.publisher, bk.year, bk.category, b.borrow_date, b.return_date " +
                "FROM borrows b JOIN books bk ON b.book_id = bk.id WHERE b.user_id = ? ORDER BY b.borrow_date DESC");
            ps.setString(1, userId);
            rs = ps.executeQuery();
            borrowModel.setRowCount(0);
            while (rs.next()) {
                String returnDate = rs.getString("return_date");
                String status = (returnDate == null || returnDate.isEmpty()) ? "Đang mượn" : "Đã trả";

                borrowModel.addRow(new Object[]{
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("publisher"),
                    rs.getString("year"),
                    rs.getString("category"),
                    formatDate(rs.getString("borrow_date")),
                    returnDate == null ? "Chưa trả" : formatDate(returnDate),
                    status
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return "";
        }
        try {

            String[] parts = dateStr.split("-");
            if (parts.length == 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0];
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void lockAccount() {

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
            PreparedStatement psCheck = conn.prepareStatement("SELECT username, status FROM users WHERE id=?");
            psCheck.setString(1, userId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String currentStatus = rs.getString("status");

                if ("locked".equals(currentStatus)) {

                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Tài khoản '" + username + "' hiện đang bị khóa.\nBạn có muốn mở khóa tài khoản này?",
                        "Mở khóa tài khoản",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        unlockAccount(username);
                    }
                } else {

                    int confirm = JOptionPane.showConfirmDialog(this,
                        "Bạn có chắc chắn muốn khóa tài khoản '" + username + "'?\n" +
                        "Người dùng sẽ không thể đăng nhập sau khi bị khóa.",
                        "Khóa tài khoản",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        lockUserAccount(username);
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kiểm tra trạng thái tài khoản: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void lockUserAccount(String username) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {

            try {
                Statement stmt = conn.createStatement();
                stmt.execute("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'active'");
            } catch (SQLException e) {

            }

            PreparedStatement ps = conn.prepareStatement("UPDATE users SET status='locked' WHERE id=?");
            ps.setString(1, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                    "Tài khoản '" + username + "' đã bị khóa thành công!\n" +
                    "Người dùng sẽ không thể đăng nhập cho đến khi được mở khóa.",
                    "Khóa tài khoản thành công",
                    JOptionPane.INFORMATION_MESSAGE);

                btnLock.setText("Mở khóa tài khoản");
                btnLock.setBackground(new Color(40, 167, 69));
                setTitle("Thông tin chi tiết người dùng - [TÀI KHOẢN BỊ KHÓA]");

            } else {
                JOptionPane.showMessageDialog(this, "Không thể khóa tài khoản!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khóa tài khoản: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unlockAccount(String username) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET status='active' WHERE id=?");
            ps.setString(1, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this,
                    "Tài khoản '" + username + "' đã được mở khóa thành công!\n" +
                    "Người dùng có thể đăng nhập và sử dụng bình thường.",
                    "Mở khóa tài khoản thành công",
                    JOptionPane.INFORMATION_MESSAGE);

                btnLock.setText("Khóa tài khoản");
                btnLock.setBackground(new Color(220, 53, 69));
                setTitle("Thông tin chi tiết người dùng");

            } else {
                JOptionPane.showMessageDialog(this, "Không thể mở khóa tài khoản!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi mở khóa tài khoản: " + ex.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printUserReport() {
        try {

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

                Graphics2D g2d = (Graphics2D)graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                g2d.setFont(new Font("Arial", Font.BOLD, 16));

                int y = 20;
                g2d.drawString("THỐNG KÊ NGƯỜI DÙNG", 50, y);
                y += 30;
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Họ và tên: " + lblName.getText(), 50, y);
                y += 20;
                g2d.drawString("Email: " + lblEmail.getText(), 50, y);
                y += 20;
                g2d.drawString("Số điện thoại: " + lblPhone.getText(), 50, y);

                return Printable.PAGE_EXISTS;
            });

            if (job.printDialog()) {
                job.print();
            }
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi in thống kê: " + ex.getMessage());
        }
    }
}
