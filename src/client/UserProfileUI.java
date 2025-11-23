package client;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class UserProfileUI extends JFrame {
    private int userId;
    private String username;
    private JTextField txtUsername, txtPhone, txtEmail;
    private JLabel lblAvatarDisplay;
    private JButton btnChangeAvatar, btnSave, btnLogout, btnCancel;
    private String currentAvatarPath = "";
    private String newAvatarPath = "";

    public UserProfileUI(int userId, String username) {
        this.userId = userId;
        this.username = username;

        setTitle("Thông tin cá nhân - " + username);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        createProfileInterface();
        loadUserData();
    }

    private void createProfileInterface() {

        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)) {
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel headerLabel = new JLabel("Thông tin cá nhân", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(new Color(0, 123, 255));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel contentPanel = new JPanel(new BorderLayout(30, 0));
        contentPanel.setOpaque(false);

        JPanel avatarPanel = createAvatarPanel();

        JPanel infoPanel = createInfoPanel();

        contentPanel.add(avatarPanel, BorderLayout.WEST);
        contentPanel.add(infoPanel, BorderLayout.CENTER);

        JPanel buttonPanel = createButtonPanel();

        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createAvatarPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(250, 300));

        lblAvatarDisplay = new JLabel();
        lblAvatarDisplay.setPreferredSize(new Dimension(200, 200));
        lblAvatarDisplay.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 3),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        lblAvatarDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        lblAvatarDisplay.setBackground(Color.WHITE);
        lblAvatarDisplay.setOpaque(true);

        setupDragAndDrop();

        setDefaultAvatar();

        btnChangeAvatar = new JButton("Đổi ảnh đại diện");
        btnChangeAvatar.setPreferredSize(new Dimension(200, 40));
        btnChangeAvatar.setBackground(new Color(255, 140, 0));
        btnChangeAvatar.setForeground(Color.WHITE);
        btnChangeAvatar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnChangeAvatar.setFocusPainted(false);
        btnChangeAvatar.addActionListener(e -> changeAvatar());

        panel.add(lblAvatarDisplay, BorderLayout.CENTER);
        panel.add(btnChangeAvatar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        panel.add(createFieldPanel("Tên đăng nhập:", txtUsername = new JTextField()));
        txtUsername.setEditable(false);
        txtUsername.setBackground(new Color(245, 245, 245));

        panel.add(Box.createVerticalStrut(20));

        panel.add(createFieldPanel("Số điện thoại:", txtPhone = new JTextField()));

        panel.add(Box.createVerticalStrut(20));

        panel.add(createFieldPanel("Email:", txtEmail = new JTextField()));

        panel.add(Box.createVerticalStrut(20));

        JPanel statsPanel = createStatsPanel();
        panel.add(statsPanel);

        return panel;
    }

    private JPanel createFieldPanel(String labelText, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(new Color(52, 58, 64));

        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setPreferredSize(new Dimension(300, 40));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        panel.add(label, BorderLayout.NORTH);
        panel.add(textField, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 15, 15));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 123, 255), 2),
            "Thống kê hoạt động",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 16),
            new Color(0, 123, 255)
        ));

        int borrowedBooks = getBorrowedBooksCount();
        int favoriteBooks = getFavoriteBooksCount();
        int totalActivities = getTotalActivitiesCount();
        int pendingRequests = getPendingRequestsCount();

        panel.add(createStatCard("Sách đang mượn", String.valueOf(borrowedBooks), new Color(220, 53, 69)));
        panel.add(createStatCard("Sách yêu thích", String.valueOf(favoriteBooks), new Color(255, 140, 0)));
        panel.add(createStatCard("Tổng hoạt động", String.valueOf(totalActivities), new Color(40, 167, 69)));
        panel.add(createStatCard("Yêu cầu chờ", String.valueOf(pendingRequests), new Color(23, 162, 184)));

        return panel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(new Color(33, 37, 41));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panel.setOpaque(false);

        btnSave = new JButton("Lưu thay đổi");
        btnSave.setPreferredSize(new Dimension(140, 45));
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> saveUserData());

        btnLogout = new JButton("Đăng xuất");
        btnLogout.setPreferredSize(new Dimension(120, 45));
        btnLogout.setBackground(new Color(220, 53, 69));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> logout());

        btnCancel = new JButton("Hủy");
        btnCancel.setPreferredSize(new Dimension(100, 45));
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(e -> dispose());

        panel.add(btnSave);
        panel.add(btnLogout);
        panel.add(btnCancel);

        return panel;
    }

    private void setDefaultAvatar() {

        String defaultAvatarText = "<html><div style='text-align:center'>" +
            "<div style='font-size:48px'>Ảnh</div>" +
            "<div style='font-size:14px; color:#666'>Chưa có ảnh</div>" +
            "<div style='font-size:10px; color:#999'>Kéo thả ảnh vào đây</div>" +
            "</html>";
        lblAvatarDisplay.setText(defaultAvatarText);
    }

    private void setupDragAndDrop() {

        new DropTarget(lblAvatarDisplay, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    lblAvatarDisplay.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(40, 167, 69), 3),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {

            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {

            }

            @Override
            public void dragExit(DropTargetEvent dte) {

                lblAvatarDisplay.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 123, 255), 3),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {

                    lblAvatarDisplay.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 123, 255), 3),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));

                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                        if (!files.isEmpty()) {
                            File file = files.get(0);
                            String fileName = file.getName().toLowerCase();

                            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                                fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                                fileName.endsWith(".bmp")) {

                                processAvatarFile(file);
                                dtde.dropComplete(true);
                            } else {
                                JOptionPane.showMessageDialog(UserProfileUI.this,
                                    "Chỉ chấp nhận file ảnh (jpg, png, gif, bmp)!",
                                    "Lỗi",
                                    JOptionPane.ERROR_MESSAGE);
                                dtde.dropComplete(false);
                            }
                        } else {
                            dtde.dropComplete(false);
                        }
                    } else {
                        dtde.dropComplete(false);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserProfileUI.this,
                        "Lỗi khi thả file: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                    dtde.dropComplete(false);
                }
            }
        });

        lblAvatarDisplay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (lblAvatarDisplay.getIcon() == null) {
                    lblAvatarDisplay.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 140, 0), 3),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (lblAvatarDisplay.getIcon() == null) {
                    lblAvatarDisplay.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 123, 255), 3),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }
            }

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                changeAvatar();
            }
        });
    }

    private void changeAvatar() {

        String[] options = {"Chọn file ảnh", "Nhập link ảnh", "Hủy"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Bạn muốn thay đổi ảnh đại diện bằng cách nào?",
            "Đổi ảnh đại diện",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );

        switch (choice) {
            case 0:
                chooseAvatarFile();
                break;
            case 1:
                enterAvatarURL();
                break;
            default:
                break;
        }
    }

    private void chooseAvatarFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn ảnh đại diện");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif", "bmp"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processAvatarFile(selectedFile);
        }
    }

    private void enterAvatarURL() {
        String url = JOptionPane.showInputDialog(
            this,
            "Nhập link ảnh đại diện:\n(Ví dụ: https://example.com/avatar.jpg)",
            "Nhập link ảnh",
            JOptionPane.PLAIN_MESSAGE
        );

        if (url != null && !url.trim().isEmpty()) {
            url = url.trim();

            if (!url.matches("^(http|https):ng|gif|bmp).*$")) {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Link có vẻ không đúng định dạng ảnh.\nBạn có muốn thử tải về không?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            processAvatarURL(url);
        }
    }

    private void processAvatarFile(File selectedFile) {
        try {

            long fileSizeInMB = selectedFile.length() / (1024 * 1024);
            if (fileSizeInMB > 5) {
                JOptionPane.showMessageDialog(this,
                    "File ảnh quá lớn! Vui lòng chọn file nhỏ hơn 5MB.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            ImageIcon originalIcon = new ImageIcon(selectedFile.getAbsolutePath());
            if (originalIcon.getIconWidth() <= 0) {
                JOptionPane.showMessageDialog(this,
                    "File không phải là ảnh hợp lệ!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            Image originalImage = originalIcon.getImage();
            Image resizedImage = originalImage.getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resizedImage);

            lblAvatarDisplay.setIcon(resizedIcon);
            lblAvatarDisplay.setText("");
            newAvatarPath = selectedFile.getAbsolutePath();

            JOptionPane.showMessageDialog(this,
                "Ảnh đã được tải thành công!\nNhấn 'Lưu thay đổi' để cập nhật.",
                "Thành công",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Không thể tải ảnh: " + ex.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processAvatarURL(String url) {

        lblAvatarDisplay.setText("<html><div style='text-align:center'>" +
            "<div style='font-size:24px'>Đang tải</div>" +
            "<div style='font-size:12px'>Đang tải...</div>" +
            "</html>");

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                try {
                    URL imageUrl = new URL(url);
                    URLConnection connection = imageUrl.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                    BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
                    if (bufferedImage != null) {
                        return new ImageIcon(bufferedImage);
                    }
                } catch (Exception e) {

                    return new ImageIcon(new URL(url));
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();

                    if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                        Image resizedImage = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                        lblAvatarDisplay.setIcon(new ImageIcon(resizedImage));
                        lblAvatarDisplay.setText("");

                        newAvatarPath = url;

                        JOptionPane.showMessageDialog(UserProfileUI.this,
                            "Ảnh từ link đã được tải thành công!\nNhấn 'Lưu thay đổi' để cập nhật.",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        setDefaultAvatar();
                        JOptionPane.showMessageDialog(UserProfileUI.this,
                            "Không thể tải ảnh từ link này!\nVui lòng kiểm tra lại đường dẫn.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    setDefaultAvatar();
                    JOptionPane.showMessageDialog(UserProfileUI.this,
                        "Lỗi tải ảnh: " + e.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void loadUserData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String query = "SELECT username, phone, email, avatar FROM users WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtUsername.setText(rs.getString("username"));
                txtPhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "");
                txtEmail.setText(rs.getString("email") != null ? rs.getString("email") : "");

                String avatarPath = rs.getString("avatar");
                System.out.println("DEBUG: Avatar URL from database: " + avatarPath);
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    currentAvatarPath = avatarPath;
                    loadAvatarImage(avatarPath);
                } else {
                    System.out.println("DEBUG: No avatar URL found, setting default");
                    setDefaultAvatar();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi tải thông tin người dùng: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAvatarImage(String avatarPath) {
        System.out.println("DEBUG: Loading avatar from: " + avatarPath);

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                ImageIcon icon = null;

                if (avatarPath.startsWith("http://") || avatarPath.startsWith("https://")) {

                    System.out.println("DEBUG: Loading as URL");
                    try {
                        URL url = new URL(avatarPath);
                        URLConnection connection = url.openConnection();
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(10000);
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

                        BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
                        if (bufferedImage != null) {
                            icon = new ImageIcon(bufferedImage);
                            System.out.println("DEBUG: URL image loaded, size: " + bufferedImage.getWidth() + "x" + bufferedImage.getHeight());
                        }
                    } catch (Exception urlEx) {
                        System.out.println("DEBUG: URL loading failed: " + urlEx.getMessage());

                        try {
                            icon = new ImageIcon(new URL(avatarPath));
                            if (icon.getIconWidth() <= 0) {
                                icon = null;
                            }
                        } catch (Exception fallbackEx) {
                            System.out.println("DEBUG: Fallback URL loading failed: " + fallbackEx.getMessage());
                        }
                    }
                } else if (avatarPath.startsWith("file:") || avatarPath.startsWith("/") || avatarPath.contains(":\\")) {

                    System.out.println("DEBUG: Loading as local file");
                    File avatarFile = new File(avatarPath);
                    if (avatarFile.exists()) {
                        icon = new ImageIcon(avatarPath);
                        System.out.println("DEBUG: Local file loaded");
                    } else {
                        System.out.println("DEBUG: Local file does not exist: " + avatarPath);
                    }
                } else {

                    System.out.println("DEBUG: Trying as URL without protocol");
                    try {
                        URL url = new URL("https://" + avatarPath);
                        URLConnection connection = url.openConnection();
                        connection.setConnectTimeout(5000);
                        connection.setReadTimeout(10000);
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                        BufferedImage bufferedImage = ImageIO.read(connection.getInputStream());
                        if (bufferedImage != null) {
                            icon = new ImageIcon(bufferedImage);
                            System.out.println("DEBUG: HTTPS URL loaded");
                        }
                    } catch (Exception httpsEx) {
                        System.out.println("DEBUG: HTTPS failed, trying HTTP: " + httpsEx.getMessage());
                        try {
                            URL url = new URL("http://" + avatarPath);
                            icon = new ImageIcon(url);
                            if (icon.getIconWidth() <= 0) {
                                icon = null;
                            } else {
                                System.out.println("DEBUG: HTTP URL loaded");
                            }
                        } catch (Exception httpEx) {
                            System.out.println("DEBUG: HTTP also failed: " + httpEx.getMessage());
                        }
                    }
                }

                return icon;
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();

                    if (icon != null && icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
                        System.out.println("DEBUG: Avatar loaded successfully, size: " + icon.getIconWidth() + "x" + icon.getIconHeight());
                        Image image = icon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                        lblAvatarDisplay.setIcon(new ImageIcon(image));
                        lblAvatarDisplay.setText("");
                    } else {

                        System.out.println("DEBUG: Failed to load avatar image, using default");
                        setDefaultAvatar();
                    }
                } catch (Exception e) {
                    System.err.println("DEBUG: Exception in done(): " + e.getMessage());
                    e.printStackTrace();
                    setDefaultAvatar();
                }
            }
        };

        worker.execute();
    }

    private void saveUserData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            String updateQuery = "UPDATE users SET phone = ?, email = ?, avatar = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(updateQuery);

            ps.setString(1, txtPhone.getText().trim());
            ps.setString(2, txtEmail.getText().trim());

            String avatarPathToSave = currentAvatarPath;
            if (!newAvatarPath.isEmpty()) {

                if (newAvatarPath.startsWith("http://") || newAvatarPath.startsWith("https://")) {

                    avatarPathToSave = newAvatarPath;
                } else {

                    File avatarsDir = new File("C:/data/avatars");
                    if (!avatarsDir.exists()) {
                        avatarsDir.mkdirs();
                    }

                    File sourceFile = new File(newAvatarPath);
                    String fileExtension = getFileExtension(sourceFile.getName());
                    String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + "." + fileExtension;
                    File destFile = new File(avatarsDir, fileName);

                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    avatarPathToSave = destFile.getAbsolutePath();
                }
            }

            ps.setString(3, avatarPathToSave);
            ps.setInt(4, userId);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                currentAvatarPath = avatarPathToSave;
                newAvatarPath = "";
                JOptionPane.showMessageDialog(this,
                    "Cập nhật thông tin thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Không thể cập nhật thông tin!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi cập nhật thông tin: " + e.getMessage(),
                "Lỗi",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "jpg";
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Bạn có chắc chắn muốn đăng xuất?",
            "Xác nhận đăng xuất",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            dispose();

            Window[] windows = Window.getWindows();
            for (Window window : windows) {
                if (window instanceof JFrame && window != this) {
                    window.dispose();
                }
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    app.MainApp.main(new String[]{});
                } catch (Exception e) {
                    System.err.println("Error showing login screen: " + e.getMessage());
                }
            });
        }
    }

    private int getBorrowedBooksCount() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String query = "SELECT COUNT(*) FROM borrow_requests br " +
                          "LEFT JOIN borrows b ON br.user_id = b.user_id AND br.book_id = b.book_id " +
                          "WHERE br.user_id = ? AND br.status = 'APPROVED' " +
                          "AND (b.return_date IS NULL OR b.return_date = '' OR b.return_date = 'null')";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String query = "SELECT COUNT(*) FROM borrows WHERE user_id = ? AND (return_date IS NULL OR return_date = '' OR return_date = 'null')";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    private int getFavoriteBooksCount() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String query = "SELECT COUNT(DISTINCT f.book_id) FROM favorites f " +
                          "INNER JOIN books b ON f.book_id = b.id " +
                          "WHERE f.user_id = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String query = "SELECT COUNT(*) FROM favorites WHERE user_id = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    private int getTotalActivitiesCount() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String query = "SELECT COUNT(*) FROM activities WHERE user_id = ? AND action_time IS NOT NULL";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String query = "SELECT COUNT(*) FROM activities WHERE user_id = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    private int getPendingRequestsCount() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {

            String query = "SELECT COUNT(*) FROM borrow_requests WHERE user_id = ? AND status = 'PENDING' AND request_date IS NOT NULL";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
                String query = "SELECT COUNT(*) FROM borrow_requests WHERE user_id = ? AND status = 'PENDING'";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            } catch (Exception ex) {
                return 0;
            }
        }
    }
}
