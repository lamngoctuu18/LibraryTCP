package client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class UserManagerUI extends JFrame {
    private JTextField txtPhone;
    private JButton btnSearch, btnBack;
    private JTable userTable;
    private DefaultTableModel userModel;

    private DatabaseManager dbManager;
    private BackgroundTaskManager taskManager;

    public UserManagerUI() {

        initializeResourceManagers();

        setTitle("Quản lý người dùng");
        setMinimumSize(new Dimension(1000, 650));
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
        setContentPane(mainPanel);

        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        mainPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        mainPanel.add(createSearchPanel(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        loadUsers();

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Quản lý người dùng");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(31, 81, 135));

        headerPanel.add(titleLabel);
        return headerPanel;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(0, 20));
        searchPanel.setOpaque(false);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Tìm kiếm người dùng",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));

        JLabel lblPhone = new JLabel("Số điện thoại:");
        lblPhone.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblPhone.setForeground(new Color(52, 58, 64));

        txtPhone = new JTextField(20);
        txtPhone.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPhone.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setBackground(new Color(31, 81, 135));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        controlsPanel.add(lblPhone);
        controlsPanel.add(txtPhone);
        controlsPanel.add(btnSearch);

        JPanel tablePanel = createTablePanel();

        searchPanel.add(controlsPanel, BorderLayout.NORTH);
        searchPanel.add(tablePanel, BorderLayout.CENTER);

        return searchPanel;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Danh sách người dùng",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));

        String[] cols = {"Họ và tên", "Số điện thoại", "Email", "Số sách đang mượn", "Ngày tạo tài khoản", "Thao tác"};
        userModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 5;
            }
        };

        userTable = new JTable(userModel);
        userTable.setRowHeight(35);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userTable.setGridColor(new Color(233, 236, 239));
        userTable.setSelectionBackground(new Color(240, 248, 255));
        userTable.setSelectionForeground(new Color(31, 81, 135));

        JTableHeader header = userTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(31, 81, 135));
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        userTable.getColumn("Thao tác").setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JButton btn = new JButton("Xem chi tiết");
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setBackground(new Color(40, 167, 69));
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                btn.setFocusPainted(false);
                return btn;
            }
        });

        userTable.getColumn("Thao tác").setCellEditor(new DefaultCellEditor(new JCheckBox()) {
            private JButton btn = new JButton("Xem chi tiết");
            {
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setBackground(new Color(40, 167, 69));
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                btn.setFocusPainted(false);
                btn.addActionListener(e -> {
                    int row = userTable.getEditingRow();
                    String userPhone = userTable.getValueAt(row, 1).toString();
                    fireEditingStopped();
                    showUserDetail(userPhone);
                });
            }
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                return btn;
            }
        });

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.getViewport().setBackground(Color.WHITE);

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        return tablePanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 15));
        buttonPanel.setOpaque(false);

        btnBack = new JButton("Làm mới");
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnBack.setBackground(new Color(23, 162, 184));
        btnBack.setForeground(Color.WHITE);
        btnBack.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        btnBack.setFocusPainted(false);
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        buttonPanel.add(btnBack);

        btnSearch.addActionListener(e -> executeWithLoading("Đang tìm kiếm người dùng...", this::loadUsers));
        btnBack.addActionListener(e -> refreshData());

        return buttonPanel;
    }

    private void loadUsers() {
        userModel.setRowCount(0);
        String phone = txtPhone.getText().trim();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
            String sql = "SELECT id, username, phone, email, created_at FROM users WHERE role != 'admin'";
            if (!phone.isEmpty()) sql += " AND phone LIKE ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            int idx = 1;
            if (!phone.isEmpty()) ps.setString(idx++, "%" + phone + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("id");
                String username = rs.getString("username");
                String userPhone = rs.getString("phone");
                String email = rs.getString("email");
                String createdAt = rs.getString("created_at");

                PreparedStatement psBorrow = conn.prepareStatement(
                    "SELECT COUNT(*) FROM borrows WHERE user_id=? AND return_date IS NULL");
                psBorrow.setInt(1, userId);
                ResultSet rsBorrow = psBorrow.executeQuery();
                String borrowCount = rsBorrow.next() ? rsBorrow.getString(1) : "0";

                userModel.addRow(new Object[]{
                    username, userPhone, email, borrowCount, createdAt, "Xem chi tiết"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage());
        }
    }

    private void showUserDetail(String phone) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE phone=?");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String userId = rs.getString("id");
                UserDetailUI detailUI = new UserDetailUI(userId);
                detailUI.setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy người dùng!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi truy vấn: " + ex.getMessage());
        }
    }

    private void refreshData() {

        txtPhone.setText("");

        loadUsers();

        JOptionPane.showMessageDialog(this,
            "Dữ liệu đã được làm mới thành công!",
            "Làm mới dữ liệu",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void initializeResourceManagers() {
        try {
            dbManager = DatabaseManager.getInstance();
            taskManager = BackgroundTaskManager.getInstance();
            System.out.println("UserManager resource managers initialized");
        } catch (Exception e) {
            System.err.println("Failed to initialize UserManager resources: " + e.getMessage());
        }
    }

    public void executeWithLoading(String message, Runnable task) {
        if (taskManager != null) {
            taskManager.executeWithLoading(
                this,
                message,
                () -> {
                    task.run();
                    return null;
                },
                result -> {

                },
                error -> {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "Lỗi: " + error.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    });
                }
            );
        } else {

            task.run();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserManagerUI().setVisible(true));
    }
}
