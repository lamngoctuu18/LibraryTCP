package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.net.URL;

public class BookManagerUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtTitle, txtAuthor, txtPublisher, txtYear, txtQuantity, txtCoverImage;
    private JTextArea txtDescription;
    private JComboBox<String> cbCategory;
    private JLabel lblCount, lblImagePreview;

    private DatabaseManager dbManager;
    private BackgroundTaskManager taskManager;

    private static final String[] CATEGORIES = {
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

    public BookManagerUI() {
        initializeResourceManagers();

        setTitle("Quản lý sách");
        setMinimumSize(new Dimension(1000, 650));
        setPreferredSize(new Dimension(1900, 1000));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(248, 250, 252),
                    0, getHeight(), new Color(238, 242, 247)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        JPanel listPanel = new JPanel(new BorderLayout(0, 25));
        listPanel.setOpaque(false);
        listPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Quản lý sách");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(31, 81, 135));

        headerPanel.add(lblTitle);
        listPanel.add(headerPanel, BorderLayout.NORTH);

        String[] cols = {"ID sách", "Ảnh bìa", "Tên sách", "Tác giả", "Nhà XB", "Năm XB", "Thể loại", "SL", "Thao tác"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col) {
                return col == 8;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(233, 236, 239));
        table.setSelectionBackground(new Color(240, 248, 255));
        table.setSelectionForeground(new Color(31, 81, 135));
        table.setIntercellSpacing(new Dimension(0, 1));

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(31, 81, 135));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        table.getColumn("Thao tác").setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
                panel.setOpaque(false);

                JButton btnEdit = new JButton("Sửa");
                btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnEdit.setBackground(new Color(255, 193, 7));
                btnEdit.setForeground(Color.WHITE);
                btnEdit.setPreferredSize(new Dimension(65, 28));
                btnEdit.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                btnEdit.setFocusPainted(false);

                JButton btnDelete = new JButton("Xóa");
                btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnDelete.setBackground(new Color(220, 53, 69));
                btnDelete.setForeground(Color.WHITE);
                btnDelete.setPreferredSize(new Dimension(65, 28));
                btnDelete.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                btnDelete.setFocusPainted(false);

                panel.add(btnEdit);
                panel.add(btnDelete);
                return panel;
            }
        });

        table.getColumn("Thao tác").setCellEditor(new javax.swing.DefaultCellEditor(new JCheckBox()) {
            private JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            private JButton btnEdit = new JButton("Sửa");
            private JButton btnDelete = new JButton("Xóa");
            {
                panel.setOpaque(false);

                btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnEdit.setBackground(new Color(255, 193, 7));
                btnEdit.setForeground(Color.WHITE);
                btnEdit.setPreferredSize(new Dimension(65, 28));
                btnEdit.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                btnEdit.setFocusPainted(false);
                btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btnDelete.setBackground(new Color(220, 53, 69));
                btnDelete.setForeground(Color.WHITE);
                btnDelete.setPreferredSize(new Dimension(65, 28));
                btnDelete.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                btnDelete.setFocusPainted(false);
                btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                btnEdit.addActionListener(e -> {
                    int row = table.getEditingRow();
                    fireEditingStopped();
                    BookManagerUI.this.editBook(row);
                });

                btnDelete.addActionListener(e -> {
                    int row = table.getEditingRow();
                    fireEditingStopped();
                    BookManagerUI.this.deleteBook(row);
                });

                panel.add(btnEdit);
                panel.add(btnDelete);
            }
            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                return panel;
            }
        });

        table.getColumn("Thao tác").setPreferredWidth(150);
        table.getColumn("Thao tác").setMinWidth(150);
        table.getColumn("Thao tác").setMaxWidth(180);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Danh sách sách",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        scrollPane.getViewport().setBackground(Color.WHITE);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        listPanel.add(tablePanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(20, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        lblCount = new JLabel("Số lượng sách hiện tại:");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblCount.setForeground(new Color(52, 58, 64));
        bottomPanel.add(lblCount, BorderLayout.WEST);

        JButton btnAddNew = new JButton("Thêm sách mới");
        btnAddNew.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAddNew.setBackground(new Color(40, 167, 69));
        btnAddNew.setForeground(Color.WHITE);
        btnAddNew.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        btnAddNew.setFocusPainted(false);
        btnAddNew.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bottomPanel.add(btnAddNew, BorderLayout.EAST);

        listPanel.add(bottomPanel, BorderLayout.SOUTH);

        JPanel addPanel = createAddPanel();

        mainPanel.add(listPanel, "LIST");
        mainPanel.add(addPanel, "ADD");
        add(mainPanel);

        btnAddNew.addActionListener(e -> {
            clearAddForm();
            cardLayout.show(mainPanel, "ADD");
        });

        loadBooks();
        pack();
        setLocationRelativeTo(null);
    }

    private Connection getConn() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:C:/data/library.db");
    }

    private JPanel createAddPanel() {
        JPanel addPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(248, 250, 252),
                    0, getHeight(), new Color(238, 242, 247)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        addPanel.setLayout(new BorderLayout(0, 25));
        addPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setOpaque(false);
        JLabel lblFormTitle = new JLabel("Thêm sách mới vào thư viện");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblFormTitle.setForeground(new Color(31, 81, 135));
        headerPanel.add(lblFormTitle);
        addPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(30, 0));
        contentPanel.setOpaque(false);

        JPanel formPanel = createFormPanel();
        contentPanel.add(formPanel, BorderLayout.CENTER);

        JPanel imagePanel = createImagePreviewPanel();
        contentPanel.add(imagePanel, BorderLayout.EAST);

        addPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = createAddFormButtonPanel();
        addPanel.add(buttonPanel, BorderLayout.SOUTH);

        return addPanel;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Thông tin sách",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));

        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        txtTitle = createStyledTextField("", 25);
        txtAuthor = createStyledTextField("", 25);
        txtPublisher = createStyledTextField("", 25);
        txtYear = createStyledTextField("", 25);
        txtQuantity = createStyledTextField("", 25);

        txtDescription = new JTextArea(4, 25);
        txtDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        JScrollPane scrollDescription = new JScrollPane(txtDescription);
        scrollDescription.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollDescription.setPreferredSize(new Dimension(350, 100));

        cbCategory = new JComboBox<>(CATEGORIES);
        cbCategory.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbCategory.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        cbCategory.setPreferredSize(new Dimension(350, 40));

        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.setOpaque(false);
        txtCoverImage = createStyledTextField("", 20);
        JButton btnPreview = new JButton("Xem trước");
        btnPreview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPreview.setBackground(new Color(31, 81, 135));
        btnPreview.setForeground(Color.WHITE);
        btnPreview.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        btnPreview.setFocusPainted(false);
        btnPreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPreview.addActionListener(e -> previewImage());
        urlPanel.add(txtCoverImage, BorderLayout.CENTER);
        urlPanel.add(btnPreview, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createStyledLabel("Tên sách:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtTitle, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("Tác giả:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtAuthor, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("Nhà xuất bản:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPublisher, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("Năm xuất bản:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtYear, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("Số lượng:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtQuantity, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("Thể loại:"), gbc);
        gbc.gridx = 1;
        formPanel.add(cbCategory, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("Mô tả sách:"), gbc);
        gbc.gridx = 1;
        formPanel.add(scrollDescription, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createStyledLabel("URL ảnh bìa:"), gbc);
        gbc.gridx = 1;
        formPanel.add(urlPanel, gbc);

        return formPanel;
    }

    private JPanel createImagePreviewPanel() {
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setOpaque(false);
        imagePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Xem trước ảnh bìa",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));

        JPanel previewPanel = new JPanel();
        previewPanel.setBackground(Color.WHITE);
        previewPanel.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true));
        previewPanel.setPreferredSize(new Dimension(200, 280));
        previewPanel.setLayout(new BorderLayout());

        lblImagePreview = new JLabel("Chưa có ảnh", SwingConstants.CENTER);
        lblImagePreview.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblImagePreview.setForeground(new Color(108, 117, 125));
        previewPanel.add(lblImagePreview, BorderLayout.CENTER);

        imagePanel.add(previewPanel, BorderLayout.CENTER);
        return imagePanel;
    }

    private JPanel createAddFormButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);

        JButton btnAddBook = new JButton("Thêm sách");
        btnAddBook.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAddBook.setBackground(new Color(40, 167, 69));
        btnAddBook.setForeground(Color.WHITE);
        btnAddBook.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        btnAddBook.setFocusPainted(false);
        btnAddBook.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAddBook.addActionListener(e -> addBook());

        JButton btnBack = new JButton("Quay lại");
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnBack.setBackground(new Color(108, 117, 125));
        btnBack.setForeground(Color.WHITE);
        btnBack.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        btnBack.setFocusPainted(false);
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> cardLayout.show(mainPanel, "LIST"));

        buttonPanel.add(btnAddBook);
        buttonPanel.add(btnBack);

        return buttonPanel;
    }

    private void previewImage() {
        String imageUrl = txtCoverImage.getText().trim();
        if (!imageUrl.isEmpty()) {
            loadImagePreview(imageUrl, lblImagePreview);
        }
    }

    private void loadImagePreview(String imageUrl, JLabel previewLabel) {
        if (!imageUrl.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(new URL(imageUrl));
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(160, 200, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaledImg));
                previewLabel.setText("");
            } catch (Exception ex) {
                previewLabel.setIcon(null);
                previewLabel.setText("Không thể tải ảnh");
            }
        } else {
            previewLabel.setIcon(null);
            previewLabel.setText("Xem trước ảnh bìa");
        }
    }

    private JTextField createStyledTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        field.setPreferredSize(new Dimension(280, 40));
        field.setMinimumSize(new Dimension(280, 40));
        field.setMaximumSize(new Dimension(280, 40));
        return field;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(55, 71, 79));
        return label;
    }

    private JTextField createModernTextField(String text, int columns) {
        JTextField field = new JTextField(text, columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        field.setPreferredSize(new Dimension(320, 42));
        field.setMinimumSize(new Dimension(320, 42));
        field.setMaximumSize(new Dimension(320, 42));
        return field;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(52, 58, 64));
        label.setPreferredSize(new Dimension(130, 25));
        return label;
    }

    private void loadBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = getConn()) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT id, title, author, publisher, year, category, quantity, cover_image FROM books");
            while (rs.next()) {
                String bookId = String.valueOf(rs.getInt("id"));
                String coverPath = rs.getString("cover_image");
                ImageIcon coverIcon = null;

                if (coverPath != null && !coverPath.isEmpty()) {
                    try {
                        ImageCacheManager cacheManager = ImageCacheManager.getInstance();
                        coverIcon = cacheManager.getImage(coverPath, bookId, 50, 70);
                    } catch (Exception ex) {
                        coverIcon = null;
                    }
                }

                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    coverIcon,
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("publisher"),
                    rs.getString("year"),
                    rs.getString("category"),
                    rs.getInt("quantity"),
                    "Thao tác"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage());
        }
        updateBookCount();
    }

    private void updateBookCount() {
        int rowCount = tableModel.getRowCount();
        lblCount.setText("Số lượng sách hiện tại: " + rowCount);
    }

    private void addBook() {
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String publisher = txtPublisher.getText().trim();
        String year = txtYear.getText().trim();
        String quantity = txtQuantity.getText().trim();
        String description = txtDescription.getText().trim();
        String category = (String) cbCategory.getSelectedItem();
        String coverImage = txtCoverImage.getText().trim();

        if (title.isEmpty() || author.isEmpty() || publisher.isEmpty() || year.isEmpty() || quantity.isEmpty() || category == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO books(title, author, publisher, year, quantity, category, cover_image, description) " +
                "VALUES(?,?,?,?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, publisher);
            ps.setString(4, year);
            ps.setInt(5, Integer.parseInt(quantity));
            ps.setString(6, category);
            ps.setString(7, coverImage);
            ps.setString(8, description);

            ps.executeUpdate();

            if (!coverImage.isEmpty() && (coverImage.startsWith("http://") || coverImage.startsWith("https://"))) {
                try {
                    java.sql.ResultSet generatedKeys = ps.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        String bookId = String.valueOf(generatedKeys.getInt(1));
                        ImageCacheManager cacheManager = ImageCacheManager.getInstance();
                        String localPath = cacheManager.downloadAndCacheImage(coverImage, bookId);

                        if (localPath != null) {
                            PreparedStatement updatePs = conn.prepareStatement(
                                "UPDATE books SET cover_image = ? WHERE id = ?");
                            updatePs.setString(1, localPath);
                            updatePs.setInt(2, Integer.parseInt(bookId));
                            updatePs.executeUpdate();
                            System.out.println("✅ Đã lưu đường dẫn ảnh local vào database");
                        }
                    }
                } catch (Exception imgEx) {
                    System.err.println("⚠️ Không thể tải ảnh: " + imgEx.getMessage());
                }
            }

            JOptionPane.showMessageDialog(this, "Thêm sách thành công!");
            loadBooks();
            clearAddForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi thêm sách: " + ex.getMessage());
        }
    }

    private void clearAddForm() {
        txtTitle.setText("");
        txtAuthor.setText("");
        txtPublisher.setText("");
        txtYear.setText("");
        txtQuantity.setText("");
        txtDescription.setText("");
        txtCoverImage.setText("");
        cbCategory.setSelectedIndex(0);
        lblImagePreview.setIcon(null);
        lblImagePreview.setText("Xem trước ảnh bìa");
    }

    private void editBook(int row) {
        int bookId = (Integer) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 2);
        String author = (String) tableModel.getValueAt(row, 3);
        String publisher = (String) tableModel.getValueAt(row, 4);
        String year = (String) tableModel.getValueAt(row, 5);
        String category = (String) tableModel.getValueAt(row, 6);
        int quantity = (Integer) tableModel.getValueAt(row, 7);

        String currentImageUrl = "";
        String currentDescription = "";
        try (Connection conn = getConn()) {
            PreparedStatement ps = conn.prepareStatement("SELECT cover_image, description FROM books WHERE id = ?");
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentImageUrl = rs.getString("cover_image");
                if (currentImageUrl == null) currentImageUrl = "";
                currentDescription = rs.getString("description");
                if (currentDescription == null) currentDescription = "";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JDialog editDialog = new JDialog(this, "Chỉnh sửa thông tin sách", true);
    editDialog.setSize(1100, 920);
    editDialog.setMinimumSize(new Dimension(1100, 920));
    editDialog.setLocationRelativeTo(this);
        editDialog.setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(248, 250, 252),
                    0, getHeight(), new Color(238, 242, 247)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));

        JPanel titleSection = new JPanel();
        titleSection.setLayout(new BoxLayout(titleSection, BoxLayout.Y_AXIS));
        titleSection.setOpaque(false);

        JLabel titleLabel = new JLabel("Chỉnh sửa thông tin sách");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(31, 81, 135));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Cập nhật thông tin chi tiết cho sách trong thư viện");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titleSection.add(titleLabel);
        titleSection.add(Box.createVerticalStrut(5));
        titleSection.add(subtitleLabel);

        headerPanel.add(titleSection, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new BorderLayout(25, 0));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 25));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Thông tin chi tiết",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField editTitle = createModernTextField(title, 25);
        JTextField editAuthor = createModernTextField(author, 25);
        JTextField editPublisher = createModernTextField(publisher, 25);
        JTextField editYear = createModernTextField(year, 25);
        JTextField editQuantity = createModernTextField(String.valueOf(quantity), 25);

        JComboBox<String> editCategory = new JComboBox<>(CATEGORIES);
        editCategory.setSelectedItem(category);
        editCategory.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        editCategory.setBackground(Color.WHITE);
        editCategory.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        editCategory.setPreferredSize(new Dimension(320, 42));
        editCategory.setMinimumSize(new Dimension(320, 42));
        editCategory.setMaximumSize(new Dimension(320, 42));

        JTextArea editDescription = new JTextArea(currentDescription, 4, 25);
        editDescription.setBackground(Color.WHITE);
        editDescription.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        editDescription.setLineWrap(true);
        editDescription.setWrapStyleWord(true);
        editDescription.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        JScrollPane editScrollDescription = new JScrollPane(editDescription);
        editScrollDescription.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        editScrollDescription.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218), 1));
    editScrollDescription.setPreferredSize(new Dimension(320, 140));
    editScrollDescription.setMinimumSize(new Dimension(320, 140));
    editScrollDescription.setMaximumSize(new Dimension(320, 140));

        JTextField editImageUrl = createModernTextField(currentImageUrl, 25);

        JLabel editImagePreview = new JLabel("Xem trước ảnh bìa", SwingConstants.CENTER);
        editImagePreview.setPreferredSize(new Dimension(200, 280));
        editImagePreview.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 2, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        editImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        editImagePreview.setVerticalAlignment(SwingConstants.CENTER);
        editImagePreview.setBackground(Color.WHITE);
        editImagePreview.setOpaque(true);
        editImagePreview.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        editImagePreview.setForeground(new Color(108, 117, 125));

        if (!currentImageUrl.isEmpty()) {
            loadImagePreview(currentImageUrl, editImagePreview);
        }

        JButton editPreviewBtn = new JButton("Xem trước ảnh");
        editPreviewBtn.setBackground(new Color(31, 81, 135));
        editPreviewBtn.setForeground(Color.WHITE);
        editPreviewBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        editPreviewBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        editPreviewBtn.setFocusPainted(false);
        editPreviewBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editPreviewBtn.setPreferredSize(new Dimension(150, 40));
        editPreviewBtn.addActionListener(e -> {
            String url = editImageUrl.getText().trim();
            if (!url.isEmpty()) {
                loadImagePreview(url, editImagePreview);
            }
        });

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createFormLabel("Tên sách:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editTitle, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("Tác giả:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editAuthor, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("Nhà xuất bản:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editPublisher, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("Năm xuất bản:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editYear, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("Số lượng:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editQuantity, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("Thể loại:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editCategory, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("Mô tả sách:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editScrollDescription, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createFormLabel("URL ảnh bìa:"), gbc);
        gbc.gridx = 1;
        formPanel.add(editImageUrl, gbc);

        gbc.gridx = 1; gbc.gridy++;
        gbc.insets = new Insets(5, 12, 12, 12);
        formPanel.add(editPreviewBtn, gbc);

        JPanel imagePreviewPanel = new JPanel(new BorderLayout());
        imagePreviewPanel.setOpaque(false);
        imagePreviewPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(31, 81, 135), 1, true),
            "Xem trước ảnh bìa",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.PLAIN, 14),
            new Color(31, 81, 135)
        ));
        imagePreviewPanel.add(editImagePreview, BorderLayout.CENTER);

    contentPanel.add(formPanel, BorderLayout.CENTER);
    contentPanel.add(imagePreviewPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(25, 0, 15, 0));

        JButton btnSave = new JButton("Lưu thay đổi");
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        btnSave.setFocusPainted(false);
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    btnSave.setPreferredSize(new Dimension(180, 45));

        JButton btnCancel = new JButton("Hủy bỏ");
        btnCancel.setBackground(new Color(220, 53, 69));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    btnCancel.setPreferredSize(new Dimension(160, 45));

        btnSave.addActionListener(e -> {
            try (Connection conn = getConn()) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE books SET title=?, author=?, publisher=?, year=?, quantity=?, category=?, description=?, cover_image=? WHERE id=?");
                ps.setString(1, editTitle.getText().trim());
                ps.setString(2, editAuthor.getText().trim());
                ps.setString(3, editPublisher.getText().trim());
                ps.setString(4, editYear.getText().trim());
                ps.setInt(5, Integer.parseInt(editQuantity.getText().trim()));
                ps.setString(6, (String) editCategory.getSelectedItem());
                ps.setString(7, editDescription.getText().trim());
                ps.setString(8, editImageUrl.getText().trim());
                ps.setInt(9, bookId);

                ps.executeUpdate();
                JOptionPane.showMessageDialog(editDialog, "Cập nhật sách thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                loadBooks();
                editDialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editDialog, "Lỗi cập nhật: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> editDialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        editDialog.getRootPane().setDefaultButton(btnSave);
        javax.swing.InputMap im = mainPanel.getInputMap(javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.ActionMap am = mainPanel.getActionMap();
        if (im != null && am != null) {
            im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "closeDialog");
            am.put("closeDialog", new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    editDialog.dispose();
                }
            });
        }

    mainPanel.add(headerPanel, BorderLayout.NORTH);
    JScrollPane contentScroll = new JScrollPane(contentPanel);
    contentScroll.setBorder(BorderFactory.createEmptyBorder());
    contentScroll.getViewport().setOpaque(false);
    contentScroll.setOpaque(false);
    contentScroll.getVerticalScrollBar().setUnitIncrement(16);
    mainPanel.add(contentScroll, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        editDialog.add(mainPanel);
        editDialog.setVisible(true);
    }

    private void deleteBook(int row) {
        int bookId = (Integer) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn xóa sách '" + title + "'?",
            "Xác nhận xóa",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = getConn()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id=?");
                ps.setInt(1, bookId);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Xóa sách thành công!");
                loadBooks();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa sách: " + ex.getMessage());
            }
        }
    }

    private void initializeResourceManagers() {
        try {
            dbManager = DatabaseManager.getInstance();
            taskManager = BackgroundTaskManager.getInstance();
            System.out.println("BookManager resource managers initialized");
        } catch (Exception e) {
            System.err.println("Failed to initialize BookManager resources: " + e.getMessage());
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

    private void loadBooksWithLoading() {
        executeWithLoading("Đang tải danh sách sách...", this::loadBooks);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BookManagerUI().setVisible(true));
    }
}
