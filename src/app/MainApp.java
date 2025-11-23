package app;

import server.LibraryServer;
import client.ClientUI;
import client.RegisterUI;
import client.AdminLoginUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MainApp {
    public static void main(String[] args) {
        boolean serverRunning = false;
        try (Socket probe = new Socket()) {
            probe.connect(new java.net.InetSocketAddress("localhost", 12345), 500);
            serverRunning = true;
        } catch (java.io.IOException e) {
            serverRunning = false;
        }

        if (serverRunning) {
            System.out.println("[MainApp] Server detected on localhost:12345 — will not start a new server.");
        } else {
            System.out.println("[MainApp] No server detected on localhost:12345 — starting LibraryServer in background...");

            new Thread(() -> {
                try {
                    LibraryServer.main(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "LibraryServer-Starter").start();
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Hệ thống quản lý thư viện");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 850);
            frame.setMinimumSize(new Dimension(500, 750));
            frame.setResizable(false);

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    int w = getWidth(), h = getHeight();
                    GradientPaint gp = new GradientPaint(0, 0, new Color(240, 248, 255), 0, h, new Color(230, 240, 250));
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, w, h);
                }
            };
            frame.setContentPane(panel);

            GroupLayout layout = new GroupLayout(panel);
            panel.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);

            JPanel headerPanel = new JPanel();
            headerPanel.setOpaque(false);
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

            JPanel logoPanel = new JPanel();
            logoPanel.setOpaque(false);
            logoPanel.setPreferredSize(new Dimension(180, 180));

            try {
                ImageIcon originalIcon = new ImageIcon("c:\\Users\\84916\\Downloads\\logothuvien.jpg");
                Image originalImage = originalIcon.getImage();
                Image scaledImage = originalImage.getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                JLabel logoLabel = new JLabel(scaledIcon);
                logoLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180), 3),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
                logoPanel.add(logoLabel);
            } catch (Exception ex) {
                JLabel logoLabel = new JLabel("📚 LIBRARY", SwingConstants.CENTER);
                logoLabel.setForeground(new Color(70, 130, 180));
                logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                logoLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180), 3),
                    BorderFactory.createEmptyBorder(30, 30, 30, 30)
                ));
                logoPanel.add(logoLabel);
            }

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            JLabel titleLabel = new JLabel("HỆ THỐNG QUẢN LÝ THƯ VIỆN", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            titleLabel.setForeground(new Color(70, 130, 180));
            titlePanel.add(titleLabel);

            JLabel subtitleLabel = new JLabel("Đăng nhập để tiếp tục", SwingConstants.CENTER);
            subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            subtitleLabel.setForeground(new Color(108, 117, 125));

            headerPanel.add(logoPanel);
            headerPanel.add(Box.createVerticalStrut(10));
            headerPanel.add(titlePanel);
            headerPanel.add(Box.createVerticalStrut(5));
            headerPanel.add(subtitleLabel);

            JPanel formPanel = new JPanel();
            formPanel.setOpaque(false);
            formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)
            ));
            formPanel.setBackground(new Color(255, 255, 255, 220));

            JLabel lblRole = new JLabel("Vai trò:");
            lblRole.setForeground(new Color(52, 58, 64));
            lblRole.setFont(new Font("Segoe UI", Font.BOLD, 14));
            String[] roles = {"Người dùng", "Quản trị viên"};
            JComboBox<String> cbRole = new JComboBox<>(roles);
            cbRole.setBackground(Color.WHITE);
            cbRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cbRole.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            cbRole.setPreferredSize(new Dimension(280, 40));

            cbRole.addActionListener(e -> {
                String selectedRole = cbRole.getSelectedItem().toString();
                if ("Quản trị viên".equals(selectedRole)) {

                    cbRole.setSelectedIndex(0);
                    frame.dispose();
                    SwingUtilities.invokeLater(() -> {
                        new AdminLoginUI(() -> {

                            main(null);
                        }).setVisible(true);
                    });
                }
            });

            JLabel lblUser = new JLabel("Tên đăng nhập:");
            lblUser.setForeground(new Color(52, 58, 64));
            lblUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JTextField txtUser = new JTextField(20);
            txtUser.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            txtUser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            txtUser.setPreferredSize(new Dimension(280, 40));

            JLabel lblPass = new JLabel("Mật khẩu:");
            lblPass.setForeground(new Color(52, 58, 64));
            lblPass.setFont(new Font("Segoe UI", Font.BOLD, 14));

            JPanel passPanel = new JPanel(new BorderLayout());
            passPanel.setBackground(Color.WHITE);
            passPanel.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218), 1));
            passPanel.setPreferredSize(new Dimension(280, 40));

            final JPasswordField txtPass = new JPasswordField(20);
            txtPass.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 5));
            txtPass.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            final JToggleButton btnShowPass = new JToggleButton("Xem");
            btnShowPass.setFocusPainted(false);
            btnShowPass.setBackground(Color.WHITE);
            btnShowPass.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 12));
            btnShowPass.setPreferredSize(new Dimension(60, 50));
            btnShowPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            btnShowPass.addActionListener(e -> {
                if (btnShowPass.isSelected()) {
                    txtPass.setEchoChar((char) 0);
                    btnShowPass.setText("Ẩn");
                } else {
                    txtPass.setEchoChar('●');
                    btnShowPass.setText("Xem");
                }
            });

            passPanel.add(txtPass, BorderLayout.CENTER);
            passPanel.add(btnShowPass, BorderLayout.EAST);

            JLabel lblForgot = new JLabel("<html><span style='color: rgb(70,130,180);'>Quên mật khẩu?</span></html>");
            lblForgot.setForeground(new Color(70, 130, 180));
            lblForgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblForgot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblForgot.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    lblForgot.setText("<html><span style='color: rgb(70,130,180);'><u>Quên mật khẩu?</u></span></html>");
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    lblForgot.setText("<html><span style='color: rgb(70,130,180);'>Quên mật khẩu?</span></html>");
                }
            });

            JButton btnLogin = new JButton("ĐĂNG NHẬP");
            btnLogin.setBackground(new Color(70, 130, 180));
            btnLogin.setForeground(Color.WHITE);
            btnLogin.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnLogin.setPreferredSize(new Dimension(280, 45));
            btnLogin.setFocusPainted(false);
            btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnLogin.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btnLogin.setBackground(new Color(60, 120, 170));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btnLogin.setBackground(new Color(70, 130, 180));
                }
            });

            JButton btnRegister = new JButton("ĐĂNG KÝ TÀI KHOẢN");
            btnRegister.setBackground(Color.WHITE);
            btnRegister.setForeground(new Color(70, 130, 180));
            btnRegister.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            btnRegister.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnRegister.setPreferredSize(new Dimension(280, 45));
            btnRegister.setFocusPainted(false);
            btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnRegister.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btnRegister.setBackground(new Color(70, 130, 180));
                    btnRegister.setForeground(Color.WHITE);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btnRegister.setBackground(Color.WHITE);
                    btnRegister.setForeground(new Color(70, 130, 180));
                }
            });

            JLabel lblMsg = new JLabel("");
            lblMsg.setForeground(new Color(204, 0, 0));

            GroupLayout formLayout = new GroupLayout(formPanel);
            formPanel.setLayout(formLayout);
            formLayout.setAutoCreateGaps(true);
            formLayout.setAutoCreateContainerGaps(false);

            formLayout.setHorizontalGroup(
                formLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(lblRole, GroupLayout.Alignment.LEADING)
                    .addComponent(cbRole, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUser, GroupLayout.Alignment.LEADING)
                    .addComponent(txtUser, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPass, GroupLayout.Alignment.LEADING)
                    .addComponent(passPanel, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addGroup(formLayout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblForgot))
                    .addComponent(lblMsg)
                    .addComponent(btnLogin, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRegister, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
            );
            formLayout.setVerticalGroup(
                formLayout.createSequentialGroup()
                    .addComponent(lblRole)
                    .addGap(5)
                    .addComponent(cbRole, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                    .addGap(15)
                    .addComponent(lblUser)
                    .addGap(5)
                    .addComponent(txtUser, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                    .addGap(15)
                    .addComponent(lblPass)
                    .addGap(5)
                    .addComponent(passPanel, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
                    .addGap(5)
                    .addComponent(lblForgot)
                    .addGap(15)
                    .addComponent(lblMsg)
                    .addGap(15)
                    .addComponent(btnLogin, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                    .addGap(15)
                    .addComponent(btnRegister, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                    .addGap(10)
            );

            layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(headerPanel)
                    .addComponent(formPanel, GroupLayout.PREFERRED_SIZE, 400, GroupLayout.PREFERRED_SIZE)
            );
            layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGap(20)
                    .addComponent(headerPanel)
                    .addGap(20)
                    .addComponent(formPanel)
                    .addGap(30)
            );

            btnLogin.addActionListener(e -> {
                lblMsg.setText("");

                String username = txtUser.getText();
                String password = new String(txtPass.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    lblMsg.setText("Vui lòng nhập đầy đủ thông tin!");
                    return;
                }

                try (Socket socket = new Socket("localhost", 12345);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    in.readLine();
                    out.println("LOGIN|" + username + "|" + password);
                    String resp = in.readLine();
                    if (resp.startsWith("LOGIN_SUCCESS")) {
                        String[] p = resp.split("\\|");
                        int userId = Integer.parseInt(p[1]);
                        String role = p[2];

                        if ("admin".equals(role)) {
                            lblMsg.setText("Vui lòng chọn vai trò quản trị viên để đăng nhập!");
                            return;
                        }

                        frame.dispose();
                        SwingUtilities.invokeLater(() -> {
                            ClientUI ui = new ClientUI();
                            ui.setUserInfo(userId, username);
                            ui.setVisible(true);
                        });
                    } else if (resp.startsWith("LOGIN_FAIL|ACCOUNT_LOCKED")) {

                        String[] parts = resp.split("\\|", 3);
                        String message = parts.length > 2 ? parts[2] : "Tài khoản đã bị khóa";

                        JOptionPane.showMessageDialog(frame,
                            "🔒 " + message,
                            "Tài khoản bị khóa",
                            JOptionPane.WARNING_MESSAGE);

                        lblMsg.setText("Tài khoản đã bị khóa!");
                    } else {
                        lblMsg.setText("Đăng nhập thất bại!");
                    }
                } catch (Exception ex) {
                    lblMsg.setText("Không kết nối được server!");
                }
            });

            btnRegister.addActionListener(e -> {
                frame.dispose();
                SwingUtilities.invokeLater(() -> new RegisterUI(() -> {
                    main(null);
                }).setVisible(true));
            });

            lblForgot.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    showPasswordRecoveryDialog(frame);
                }
            });

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void showPasswordRecoveryDialog(JFrame parentFrame) {
        JDialog dialog = new JDialog(parentFrame, "Khôi phục mật khẩu - Bảo mật", true);
        dialog.setSize(550, 750);
        dialog.setResizable(false);

        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);

        JPanel emailPanel = createEmailInputPanel(dialog, cardLayout, cardPanel);
        cardPanel.add(emailPanel, "EMAIL_STEP");

        dialog.setContentPane(cardPanel);
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    private static JPanel createEmailInputPanel(JDialog dialog, CardLayout cardLayout, JPanel cardPanel) {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(240, 248, 255), 0, h, new Color(225, 240, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };

        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel("🔐", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("KHÔI PHỤC MẬT KHẨU", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(70, 130, 180));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stepLabel = new JLabel("Bước 1/3: Xác thực Email", SwingConstants.CENTER);
        stepLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stepLabel.setForeground(new Color(108, 117, 125));
        stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(stepLabel);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(30, 35, 30, 35)
        ));

        JLabel lblEmail = new JLabel("Địa chỉ Email:");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEmail.setForeground(new Color(52, 58, 64));

        JTextField txtEmail = new JTextField();
        txtEmail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtEmail.setPreferredSize(new Dimension(350, 45));
        txtEmail.setBackground(Color.WHITE);

        JLabel lblNote = new JLabel("<html><div style='text-align: center; color: #6c757d;'>Chúng tôi sẽ gửi mã xác thực 6 số đến email này.<br/>Mã có hiệu lực trong 5 phút.</div></html>", SwingConstants.CENTER);
        lblNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GroupLayout formLayout = new GroupLayout(formPanel);
        formPanel.setLayout(formLayout);
        formLayout.setAutoCreateGaps(true);
        formLayout.setAutoCreateContainerGaps(false);

        formLayout.setHorizontalGroup(
            formLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(lblEmail, GroupLayout.Alignment.LEADING)
                .addComponent(txtEmail, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblNote)
                .addComponent(lblStatus)
        );
        formLayout.setVerticalGroup(
            formLayout.createSequentialGroup()
                .addComponent(lblEmail)
                .addGap(8)
                .addComponent(txtEmail, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(15)
                .addComponent(lblNote)
                .addGap(15)
                .addComponent(lblStatus)
        );

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        JButton btnCancel = createStyledButton("Hủy bỏ", new Color(108, 117, 125), Color.WHITE);
        btnCancel.addActionListener(ev -> dialog.dispose());

        JButton btnSendCode = createStyledButton("Gửi mã xác thực", new Color(70, 130, 180), Color.WHITE);

        btnSendCode.addActionListener(ev -> {
            String email = txtEmail.getText().trim();
            if (email.isEmpty() || !isValidEmail(email)) {
                lblStatus.setText("⚠️ Vui lòng nhập địa chỉ email hợp lệ");
                lblStatus.setForeground(new Color(220, 53, 69));
                return;
            }

            btnSendCode.setText("Đang gửi...");
            btnSendCode.setEnabled(false);
            lblStatus.setText("📧 Đang kiểm tra email...");
            lblStatus.setForeground(new Color(108, 117, 125));

            SwingUtilities.invokeLater(() -> {
                try (Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {

                    PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE email = ?");
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String username = rs.getString("username");

                        String otpCode = generateOTP();
                        if (saveOTPToDatabase(conn, email, otpCode)) {

                            simulateEmailSending(email, otpCode);

                            lblStatus.setText("✅ Mã xác thực đã được gửi đến email của bạn");
                            lblStatus.setForeground(new Color(40, 167, 69));

                            JPanel otpPanel = createOTPVerificationPanel(dialog, cardLayout, cardPanel, email, username);
                            cardPanel.add(otpPanel, "OTP_STEP");
                            cardLayout.show(cardPanel, "OTP_STEP");
                        } else {
                            lblStatus.setText("⚠️ Lỗi hệ thống, vui lòng thử lại");
                            lblStatus.setForeground(new Color(220, 53, 69));
                        }
                    } else {
                        lblStatus.setText("❌ Email không tồn tại trong hệ thống");
                        lblStatus.setForeground(new Color(220, 53, 69));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("⚠️ Lỗi kết nối cơ sở dữ liệu");
                    lblStatus.setForeground(new Color(220, 53, 69));
                }

                btnSendCode.setText("Gửi mã xác thực");
                btnSendCode.setEnabled(true);
            });
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSendCode);

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(headerPanel)
                .addComponent(formPanel, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                .addComponent(buttonPanel)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(20)
                .addComponent(headerPanel)
                .addGap(25)
                .addComponent(formPanel)
                .addGap(20)
                .addComponent(buttonPanel)
                .addGap(20)
        );

        txtEmail.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnSendCode.doClick();
                }
            }
        });

        return mainPanel;
    }

    private static JPanel createOTPVerificationPanel(JDialog dialog, CardLayout cardLayout, JPanel cardPanel, String email, String username) {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(240, 248, 255), 0, h, new Color(225, 240, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };

        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel("📱", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("NHẬP MÃ XÁC THỰC", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(70, 130, 180));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stepLabel = new JLabel("Bước 2/3: Xác thực OTP", SwingConstants.CENTER);
        stepLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stepLabel.setForeground(new Color(108, 117, 125));
        stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(stepLabel);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(30, 35, 30, 35)
        ));

        JLabel lblInfo = new JLabel("<html><div style='text-align: center;'>Mã xác thực đã được gửi đến:<br/><b>" + maskEmail(email) + "</b></div></html>", SwingConstants.CENTER);
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblInfo.setForeground(new Color(52, 58, 64));

        JLabel lblOTP = new JLabel("Mã xác thực (6 số):");
        lblOTP.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblOTP.setForeground(new Color(52, 58, 64));

        JTextField txtOTP = new JTextField();
        txtOTP.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtOTP.setFont(new Font("Segoe UI", Font.BOLD, 18));
        txtOTP.setPreferredSize(new Dimension(200, 50));
        txtOTP.setBackground(Color.WHITE);
        txtOTP.setHorizontalAlignment(JTextField.CENTER);

        JLabel lblTimer = new JLabel("Mã có hiệu lực: 05:00", SwingConstants.CENTER);
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTimer.setForeground(new Color(220, 53, 69));

        Timer countdownTimer = new Timer(1000, null);
        final long[] startTime = {System.currentTimeMillis()};
        countdownTimer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime[0];
            long remaining = 300000 - elapsed;

            if (remaining <= 0) {
                lblTimer.setText("⏰ Mã đã hết hạn");
                lblTimer.setForeground(new Color(220, 53, 69));
                countdownTimer.stop();
            } else {
                int minutes = (int) (remaining / 60000);
                int seconds = (int) ((remaining % 60000) / 1000);
                lblTimer.setText(String.format("Mã có hiệu lực: %02d:%02d", minutes, seconds));
                lblTimer.setForeground(new Color(40, 167, 69));
            }
        });
        countdownTimer.start();

        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GroupLayout formLayout = new GroupLayout(formPanel);
        formPanel.setLayout(formLayout);
        formLayout.setAutoCreateGaps(true);
        formLayout.setAutoCreateContainerGaps(false);

        formLayout.setHorizontalGroup(
            formLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(lblInfo)
                .addComponent(lblOTP)
                .addComponent(txtOTP, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblTimer)
                .addComponent(lblStatus)
        );
        formLayout.setVerticalGroup(
            formLayout.createSequentialGroup()
                .addComponent(lblInfo)
                .addGap(20)
                .addComponent(lblOTP)
                .addGap(8)
                .addComponent(txtOTP, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                .addGap(15)
                .addComponent(lblTimer)
                .addGap(15)
                .addComponent(lblStatus)
        );

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        JButton btnBack = createStyledButton("Quay lại", new Color(108, 117, 125), Color.WHITE);
        btnBack.addActionListener(ev -> {
            countdownTimer.stop();
            cardLayout.show(cardPanel, "EMAIL_STEP");
        });

        JButton btnResend = createStyledButton("Gửi lại mã", new Color(255, 193, 7), Color.BLACK);
        btnResend.addActionListener(ev -> {

            String newOTP = generateOTP();
            try (Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
                if (saveOTPToDatabase(conn, email, newOTP)) {
                    simulateEmailSending(email, newOTP);
                    lblStatus.setText("📧 Mã mới đã được gửi");
                    lblStatus.setForeground(new Color(40, 167, 69));

                    startTime[0] = System.currentTimeMillis();
                    countdownTimer.restart();
                    txtOTP.setText("");
                }
            } catch (Exception ex) {
                lblStatus.setText("⚠️ Lỗi gửi mã");
                lblStatus.setForeground(new Color(220, 53, 69));
            }
        });

        JButton btnVerify = createStyledButton("Xác thực", new Color(70, 130, 180), Color.WHITE);
        btnVerify.addActionListener(ev -> {
            String otpInput = txtOTP.getText().trim();
            if (otpInput.isEmpty() || otpInput.length() != 6) {
                lblStatus.setText("⚠️ Vui lòng nhập mã 6 số");
                lblStatus.setForeground(new Color(220, 53, 69));
                return;
            }

            btnVerify.setText("Đang xác thực...");
            btnVerify.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try (Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {
                    if (verifyOTP(conn, email, otpInput)) {
                        countdownTimer.stop();
                        lblStatus.setText("✅ Xác thực thành công!");
                        lblStatus.setForeground(new Color(40, 167, 69));

                        JPanel passwordPanel = createPasswordResetPanel(dialog, cardLayout, cardPanel, email, username);
                        cardPanel.add(passwordPanel, "PASSWORD_STEP");
                        cardLayout.show(cardPanel, "PASSWORD_STEP");
                    } else {
                        lblStatus.setText("❌ Mã xác thực không đúng hoặc đã hết hạn");
                        lblStatus.setForeground(new Color(220, 53, 69));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("⚠️ Lỗi xác thực");
                    lblStatus.setForeground(new Color(220, 53, 69));
                }

                btnVerify.setText("Xác thực");
                btnVerify.setEnabled(true);
            });
        });

        buttonPanel.add(btnBack);
        buttonPanel.add(btnResend);
        buttonPanel.add(btnVerify);

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(headerPanel)
                .addComponent(formPanel, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                .addComponent(buttonPanel)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(20)
                .addComponent(headerPanel)
                .addGap(25)
                .addComponent(formPanel)
                .addGap(20)
                .addComponent(buttonPanel)
                .addGap(20)
        );

        txtOTP.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnVerify.doClick();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
                if (txtOTP.getText().length() >= 6 && c != KeyEvent.VK_BACK_SPACE) {
                    e.consume();
                }
            }
        });

        return mainPanel;
    }

    private static JPanel createPasswordResetPanel(JDialog dialog, CardLayout cardLayout, JPanel cardPanel, String email, String username) {
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, new Color(240, 248, 255), 0, h, new Color(225, 240, 255));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };

        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel("🔒", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("ĐẶT MẬT KHẨU MỚI", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(70, 130, 180));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stepLabel = new JLabel("Bước 3/3: Tạo mật khẩu mới", SwingConstants.CENTER);
        stepLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        stepLabel.setForeground(new Color(108, 117, 125));
        stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(stepLabel);

        JPanel formPanel = new JPanel();
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(30, 35, 30, 35)
        ));

        JLabel lblUsername = new JLabel("Tài khoản: " + username);
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblUsername.setForeground(new Color(70, 130, 180));

        JLabel lblNewPass = new JLabel("Mật khẩu mới:");
        lblNewPass.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblNewPass.setForeground(new Color(52, 58, 64));

        JPasswordField txtNewPass = new JPasswordField();
        txtNewPass.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtNewPass.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtNewPass.setPreferredSize(new Dimension(350, 45));
        txtNewPass.setBackground(Color.WHITE);

        JLabel lblConfirmPass = new JLabel("Xác nhận mật khẩu:");
        lblConfirmPass.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblConfirmPass.setForeground(new Color(52, 58, 64));

        JPasswordField txtConfirmPass = new JPasswordField();
        txtConfirmPass.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtConfirmPass.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtConfirmPass.setPreferredSize(new Dimension(350, 45));
        txtConfirmPass.setBackground(Color.WHITE);

        JLabel lblNote = new JLabel("<html><div style='text-align: center; color: #6c757d;'>Mật khẩu phải có ít nhất 6 ký tự</div></html>", SwingConstants.CENTER);
        lblNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblStatus = new JLabel("", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GroupLayout formLayout = new GroupLayout(formPanel);
        formPanel.setLayout(formLayout);
        formLayout.setAutoCreateGaps(true);
        formLayout.setAutoCreateContainerGaps(false);

        formLayout.setHorizontalGroup(
            formLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(lblUsername, GroupLayout.Alignment.LEADING)
                .addComponent(lblNewPass, GroupLayout.Alignment.LEADING)
                .addComponent(txtNewPass, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblConfirmPass, GroupLayout.Alignment.LEADING)
                .addComponent(txtConfirmPass, GroupLayout.PREFERRED_SIZE, 350, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblNote)
                .addComponent(lblStatus)
        );
        formLayout.setVerticalGroup(
            formLayout.createSequentialGroup()
                .addComponent(lblUsername)
                .addGap(15)
                .addComponent(lblNewPass)
                .addGap(8)
                .addComponent(txtNewPass, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(15)
                .addComponent(lblConfirmPass)
                .addGap(8)
                .addComponent(txtConfirmPass, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(15)
                .addComponent(lblNote)
                .addGap(15)
                .addComponent(lblStatus)
        );

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        JButton btnCancel = createStyledButton("Hủy bỏ", new Color(108, 117, 125), Color.WHITE);
        btnCancel.addActionListener(ev -> dialog.dispose());

        JButton btnReset = createStyledButton("Đặt mật khẩu mới", new Color(40, 167, 69), Color.WHITE);
        btnReset.addActionListener(ev -> {
            String newPassword = new String(txtNewPass.getPassword());
            String confirmPassword = new String(txtConfirmPass.getPassword());

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                lblStatus.setText("⚠️ Vui lòng nhập đầy đủ thông tin");
                lblStatus.setForeground(new Color(220, 53, 69));
                return;
            }

            if (newPassword.length() < 6) {
                lblStatus.setText("⚠️ Mật khẩu phải có ít nhất 6 ký tự");
                lblStatus.setForeground(new Color(220, 53, 69));
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                lblStatus.setText("⚠️ Mật khẩu xác nhận không khớp");
                lblStatus.setForeground(new Color(220, 53, 69));
                return;
            }

            btnReset.setText("Đang cập nhật...");
            btnReset.setEnabled(false);

            SwingUtilities.invokeLater(() -> {
                try (Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:C:/data/library.db")) {

                    PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
                    ps.setString(1, newPassword);
                    ps.setString(2, email);
                    int rowsUpdated = ps.executeUpdate();

                    if (rowsUpdated > 0) {

                        clearOTP(conn, email);

                        lblStatus.setText("✅ Mật khẩu đã được cập nhật thành công!");
                        lblStatus.setForeground(new Color(40, 167, 69));

                        Timer timer = new Timer(2000, e -> {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(null,
                                "🎉 Mật khẩu đã được đặt lại thành công!\n\nBạn có thể đăng nhập bằng mật khẩu mới.",
                                "Thành công",
                                JOptionPane.INFORMATION_MESSAGE);
                        });
                        timer.setRepeats(false);
                        timer.start();
                    } else {
                        lblStatus.setText("⚠️ Lỗi cập nhật mật khẩu");
                        lblStatus.setForeground(new Color(220, 53, 69));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("⚠️ Lỗi hệ thống");
                    lblStatus.setForeground(new Color(220, 53, 69));
                }

                btnReset.setText("Đặt mật khẩu mới");
                btnReset.setEnabled(true);
            });
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnReset);

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(headerPanel)
                .addComponent(formPanel, GroupLayout.PREFERRED_SIZE, 450, GroupLayout.PREFERRED_SIZE)
                .addComponent(buttonPanel)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(20)
                .addComponent(headerPanel)
                .addGap(25)
                .addComponent(formPanel)
                .addGap(20)
                .addComponent(buttonPanel)
                .addGap(20)
        );

        return mainPanel;
    }

    private static JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(140, 45));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            Color originalBg = bgColor;

            @Override
            public void mouseEntered(MouseEvent e) {
                Color hoverColor = new Color(
                    Math.max(0, originalBg.getRed() - 20),
                    Math.max(0, originalBg.getGreen() - 20),
                    Math.max(0, originalBg.getBlue() - 20)
                );
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        return button;
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private static String generateOTP() {
        java.util.Random random = new java.util.Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private static boolean saveOTPToDatabase(Connection conn, String email, String otpCode) {
        try {

            PreparedStatement createTable = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS password_reset_otp (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "email TEXT NOT NULL, " +
                "otp_code TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "expires_at INTEGER NOT NULL)"
            );
            createTable.execute();

            PreparedStatement clearOld = conn.prepareStatement("DELETE FROM password_reset_otp WHERE email = ?");
            clearOld.setString(1, email);
            clearOld.execute();

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO password_reset_otp (email, otp_code, created_at, expires_at) VALUES (?, ?, ?, ?)"
            );
            long now = System.currentTimeMillis();
            long expiry = now + 300000;

            ps.setString(1, email);
            ps.setString(2, otpCode);
            ps.setLong(3, now);
            ps.setLong(4, expiry);

            return ps.executeUpdate() > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean verifyOTP(Connection conn, String email, String otpCode) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT otp_code, expires_at FROM password_reset_otp WHERE email = ? ORDER BY created_at DESC LIMIT 1"
            );
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedOTP = rs.getString("otp_code");
                long expiryTime = rs.getLong("expires_at");
                long currentTime = System.currentTimeMillis();

                return storedOTP.equals(otpCode) && currentTime <= expiryTime;
            }
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void clearOTP(Connection conn, String email) {
        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM password_reset_otp WHERE email = ?");
            ps.setString(1, email);
            ps.execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 2) {
            String username = email.substring(0, atIndex);
            String domain = email.substring(atIndex);
            String maskedUsername = username.substring(0, 2) + "***" + username.substring(username.length() - 1);
            return maskedUsername + domain;
        }
        return email;
    }

    private static void simulateEmailSending(String email, String otpCode) {

        System.out.println("=== EMAIL SIMULATION ===");
        System.out.println("To: " + email);
        System.out.println("Subject: Mã xác thực khôi phục mật khẩu");
        System.out.println("Body: Mã xác thực của bạn là: " + otpCode);
        System.out.println("Mã có hiệu lực trong 5 phút.");
        System.out.println("========================");

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null,
                "📧 DEMO - Email đã được gửi!\n\n" +
                "Trong môi trường thực tế, email sẽ được gửi đến: " + email + "\n" +
                "Mã OTP demo: " + otpCode + "\n\n" +
                "Vui lòng sử dụng mã này để tiếp tục.",
                "Email Demo",
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
