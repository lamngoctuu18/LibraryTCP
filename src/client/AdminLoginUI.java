package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class AdminLoginUI extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblMessage;
    private Runnable onBackToMain;

    public AdminLoginUI(Runnable onBackToMain) {
        this.onBackToMain = onBackToMain;
        initializeComponents();
        setupEventListeners();
    }

    private void initializeComponents() {
        setTitle("Admin Portal - Library Management System");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(520, 680);
        setMinimumSize(new Dimension(550, 750));
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(13, 39, 80),
                    0, h, new Color(41, 84, 144)
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);

                g2d.setColor(new Color(255, 255, 255, 5));
                for (int i = 0; i < w; i += 40) {
                    for (int j = 0; j < h; j += 40) {
                        g2d.fillOval(i, j, 2, 2);
                    }
                }
            }
        };
        setContentPane(mainPanel);

        GroupLayout layout = new GroupLayout(mainPanel);
        mainPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        JPanel headerPanel = createHeaderPanel();

        JPanel formPanel = createFormPanel();

        JPanel buttonPanel = createButtonPanel();

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(headerPanel)
                .addComponent(formPanel, GroupLayout.PREFERRED_SIZE, 400, GroupLayout.PREFERRED_SIZE)
                .addComponent(buttonPanel)
        );

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(25)
                .addComponent(headerPanel)
                .addGap(35)
                .addComponent(formPanel)
                .addGap(25)
                .addComponent(buttonPanel)
                .addGap(25)
        );
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 100;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                for (int i = 0; i < 5; i++) {
                    g2d.setColor(new Color(52, 152, 219, 30 - i * 5));
                    g2d.fillOval(x - i * 2, y - i * 2, size + i * 4, size + i * 4);
                }

                GradientPaint gp = new GradientPaint(
                    x, y, new Color(52, 152, 219),
                    x + size, y + size, new Color(41, 128, 185)
                );
                g2d.setPaint(gp);
                g2d.fillOval(x, y, size, size);

                g2d.setColor(new Color(255, 255, 255, 40));
                g2d.fillOval(x + 10, y + 10, size - 40, size - 40);
            }
        };
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(120, 120));
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = new JLabel("ADMIN", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconPanel.add(iconLabel);

        JLabel titleLabel = new JLabel("ADMINISTRATOR", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(new Color(173, 216, 230));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Secure Access Portal", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel infoLabel = new JLabel("Authorized Personnel Only", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        infoLabel.setForeground(new Color(149, 165, 166));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(iconPanel);
        headerPanel.add(Box.createVerticalStrut(15));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(infoLabel);
        headerPanel.add(Box.createVerticalStrut(5));

        return headerPanel;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 14;

                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.fillRoundRect(3, 3, getWidth() - 3, getHeight() - 3, arc, arc);

                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, arc, arc);

                g2d.setColor(new Color(173, 216, 230, 80));
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, arc, arc);
            }
        };
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(35, 35, 35, 35));

        GroupLayout formLayout = new GroupLayout(formPanel);
        formPanel.setLayout(formLayout);

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblUsername.setForeground(Color.WHITE);

        txtUsername = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(255, 255, 255, 15));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                if (hasFocus()) {
                    g2d.setColor(new Color(41, 128, 185));
                    g2d.setStroke(new BasicStroke(2));
                } else {
                    g2d.setColor(new Color(173, 216, 230, 100));
                    g2d.setStroke(new BasicStroke(1));
                }
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);

                super.paintComponent(g);
            }
        };
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtUsername.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        txtUsername.setPreferredSize(new Dimension(300, 45));
        txtUsername.setBackground(new Color(0, 0, 0, 0));
        txtUsername.setForeground(Color.WHITE);
        txtUsername.setCaretColor(Color.WHITE);
        txtUsername.setOpaque(false);

        txtUsername.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                txtUsername.repaint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                txtUsername.repaint();
            }
        });

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPassword.setForeground(Color.WHITE);

        JPanel passPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(new Color(255, 255, 255, 15));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                if (txtPassword.hasFocus()) {
                    g2d.setColor(new Color(41, 128, 185));
                    g2d.setStroke(new BasicStroke(2));
                } else {
                    g2d.setColor(new Color(173, 216, 230, 100));
                    g2d.setStroke(new BasicStroke(1));
                }
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
            }
        };
        passPanel.setOpaque(false);
        passPanel.setPreferredSize(new Dimension(300, 45));

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtPassword.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 5));
        txtPassword.setBackground(new Color(0, 0, 0, 0));
        txtPassword.setForeground(Color.WHITE);
        txtPassword.setCaretColor(Color.WHITE);
        txtPassword.setOpaque(false);

        txtPassword.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                passPanel.repaint();
            }
            @Override
            public void focusLost(FocusEvent e) {
                passPanel.repaint();
            }
        });

        JToggleButton btnShowPass = new JToggleButton("Hiện") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isSelected()) {
                    g2d.setColor(new Color(41, 128, 185, 120));
                } else {
                    g2d.setColor(new Color(255, 255, 255, 25));
                }
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);

                super.paintComponent(g);
            }
        };
        btnShowPass.setFocusPainted(false);
        btnShowPass.setOpaque(false);
        btnShowPass.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 12));
        btnShowPass.setPreferredSize(new Dimension(50, 45));
        btnShowPass.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnShowPass.setForeground(Color.BLACK);
        btnShowPass.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnShowPass.addActionListener(e -> {
            if (btnShowPass.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                btnShowPass.setText("Ẩn");
            } else {
                txtPassword.setEchoChar('●');
                btnShowPass.setText("Hiện");
            }
        });

        passPanel.add(txtPassword, BorderLayout.CENTER);
        passPanel.add(btnShowPass, BorderLayout.EAST);

        lblMessage = new JLabel("", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getText() != null && !getText().isEmpty()) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2d.setColor(new Color(231, 76, 60, 40));
                    g2d.fillRoundRect(5, 2, getWidth() - 10, getHeight() - 4, 8, 8);

                    g2d.setColor(new Color(231, 76, 60, 100));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRoundRect(5, 2, getWidth() - 10, getHeight() - 4, 8, 8);
                }
                super.paintComponent(g);
            }
        };
        lblMessage.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMessage.setForeground(new Color(220, 50, 50));
        lblMessage.setOpaque(false);

        JLabel securityNote = new JLabel("Restricted Access Area", SwingConstants.CENTER);
        securityNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        securityNote.setForeground(new Color(230, 126, 34));

        formLayout.setHorizontalGroup(
            formLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(lblUsername, GroupLayout.Alignment.LEADING)
                .addComponent(txtUsername, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblPassword, GroupLayout.Alignment.LEADING)
                .addComponent(passPanel, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblMessage)
                .addComponent(securityNote)
        );

        formLayout.setVerticalGroup(
            formLayout.createSequentialGroup()
                .addComponent(lblUsername)
                .addGap(8)
                .addComponent(txtUsername, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(20)
                .addComponent(lblPassword)
                .addGap(8)
                .addComponent(passPanel, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(15)
                .addComponent(lblMessage)
                .addGap(12)
                .addComponent(securityNote)
        );

        return formPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);

        JButton btnLogin = new JButton("Đăng nhập") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gp;
                if (getModel().isPressed()) {
                    gp = new GradientPaint(0, 0, new Color(23, 54, 93), 0, getHeight(), new Color(31, 72, 124));
                } else if (getModel().isRollover()) {
                    gp = new GradientPaint(0, 0, new Color(52, 103, 171), 0, getHeight(), new Color(41, 84, 144));
                } else {
                    gp = new GradientPaint(0, 0, new Color(31, 72, 124), 0, getHeight(), new Color(23, 54, 93));
                }
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);

                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.setForeground(Color.BLACK);
        btnLogin.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        btnLogin.setPreferredSize(new Dimension(180, 50));
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnBack = new JButton("Quay lại") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(180, 180, 180, 60));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(200, 200, 200, 50));
                } else {
                    g2d.setColor(new Color(220, 220, 220, 40));
                }
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);

                g2d.setColor(new Color(255, 255, 255, 60));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);

                super.paintComponent(g);
            }
        };
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnBack.setForeground(new Color(80, 80, 80));
        btnBack.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        btnBack.setPreferredSize(new Dimension(180, 50));
        btnBack.setFocusPainted(false);
        btnBack.setOpaque(false);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnLogin.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnLogin.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnLogin.repaint();
            }
        });

        btnBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnBack.setForeground(new Color(255, 255, 255));
                btnBack.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnBack.setForeground(new Color(220, 225, 230));
                btnBack.repaint();
            }
        });

        btnLogin.addActionListener(e -> handleLogin());

        btnBack.addActionListener(e -> {
            dispose();
            if (onBackToMain != null) {
                onBackToMain.run();
            }
        });

        buttonPanel.add(btnBack);
        buttonPanel.add(btnLogin);

        return buttonPanel;
    }

    private void setupEventListeners() {

        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        });

        txtUsername.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    txtPassword.requestFocus();
                }
            }
        });

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                if (onBackToMain != null) {
                    onBackToMain.run();
                }
            }
        });
    }

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        lblMessage.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if ("admin".equals(username) && "admin".equals(password)) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                try {
                    AdminUI adminUI = new AdminUI();
                    adminUI.setVisible(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    lblMessage.setText("Lỗi mở giao diện admin: " + ex.getMessage());
                }
            });
            return;
        }

        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine();
            out.println("LOGIN|" + username + "|" + password);
            String resp = in.readLine();

            if (resp.startsWith("LOGIN_SUCCESS")) {
                String[] parts = resp.split("\\|");
                String role = parts[2];

                if ("admin".equals(role)) {
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        try {
                            AdminUI adminUI = new AdminUI();
                            adminUI.setVisible(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Lỗi mở giao diện admin: " + ex.getMessage());
                        }
                    });
                } else {
                    lblMessage.setText("Tài khoản này không có quyền quản trị!");
                }
            } else if (resp.startsWith("LOGIN_FAIL|ACCOUNT_LOCKED")) {
                String[] parts = resp.split("\\|", 3);
                String message = parts.length > 2 ? parts[2] : "Tài khoản đã bị khóa";

                JOptionPane.showMessageDialog(this,
                    message,
                    "Tài khoản bị khóa",
                    JOptionPane.WARNING_MESSAGE);

                lblMessage.setText("Tài khoản đã bị khóa!");
            } else {
                lblMessage.setText("Sai tên đăng nhập hoặc mật khẩu!");
            }
        } catch (Exception ex) {
            lblMessage.setText("Không thể kết nối đến server!");
        }
    }
}
