package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ThemeSelector extends JDialog {
    private AdminUI parentFrame;

    public ThemeSelector(AdminUI parent) {
        super(parent, "Chọn Theme Sidebar", true);
        this.parentFrame = parent;
        initializeDialog();
    }

    private void initializeDialog() {
        setSize(400, 300);
        setLocationRelativeTo(parentFrame);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Chọn Theme cho Sidebar", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(52, 73, 94));

        JPanel themesPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        themesPanel.setOpaque(false);

        JButton blueBtn = createThemeButton("Blue Theme", SidebarTheme.BLUE_THEME);
        JButton purpleBtn = createThemeButton("Purple Theme", SidebarTheme.PURPLE_THEME);
        JButton greenBtn = createThemeButton("Green Theme", SidebarTheme.GREEN_THEME);
        JButton darkBtn = createThemeButton("Dark Theme", SidebarTheme.DARK_THEME);
        JButton orangeBtn = createThemeButton("Orange Theme", SidebarTheme.ORANGE_THEME);

        JButton currentBtn = new JButton("Current Theme");
        currentBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        currentBtn.setBackground(new Color(220, 220, 220));
        currentBtn.setEnabled(false);

        themesPanel.add(blueBtn);
        themesPanel.add(purpleBtn);
        themesPanel.add(greenBtn);
        themesPanel.add(darkBtn);
        themesPanel.add(orangeBtn);
        themesPanel.add(currentBtn);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setOpaque(false);
        JButton closeBtn = new JButton("Đóng");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setBackground(new Color(52, 73, 94));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        closeBtn.addActionListener(e -> dispose());
        buttonPanel.add(closeBtn);

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(themesPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JButton createThemeButton(String name, SidebarTheme.Theme theme) {
        JButton btn = new JButton(name);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(theme.accentColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SidebarTheme.setTheme(theme);

                JOptionPane.showMessageDialog(ThemeSelector.this,
                    "Theme đã được thay đổi!\nKhởi động lại ứng dụng để áp dụng theme mới.",
                    "Theme Updated",
                    JOptionPane.INFORMATION_MESSAGE);

                dispose();
            }
        });

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = btn.getBackground();

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(originalColor.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(originalColor);
            }
        });

        return btn;
    }

    public static void showThemeSelector(AdminUI parent) {
        SwingUtilities.invokeLater(() -> {
            new ThemeSelector(parent).setVisible(true);
        });
    }
}
