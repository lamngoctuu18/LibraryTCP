package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoadingDialog extends JDialog {
    private JProgressBar progressBar;
    private JLabel messageLabel;
    private Timer animationTimer;
    private int dotCount = 0;
    private String baseMessage;
    private boolean isIndeterminate = true;

    private static final Color PRIMARY_BLUE = new Color(52, 152, 219);
    private static final Color BACKGROUND_COLOR = new Color(248, 249, 250);
    private static final Color TEXT_COLOR = new Color(52, 73, 94);

    public LoadingDialog(Frame parent, String title, String message) {
        super(parent, title, true);
        this.baseMessage = message;
        initializeComponents();
        setupAnimation();
    }

    public LoadingDialog(Frame parent, String message) {
        this(parent, "Đang xử lý...", message);
    }

    private void initializeComponents() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(350, 150);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setOpaque(false);

        JLabel iconLabel = new JLabel("⏳", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        iconLabel.setPreferredSize(new Dimension(40, 40));

        messageLabel = new JLabel(baseMessage, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setForeground(TEXT_COLOR);

        contentPanel.add(iconLabel, BorderLayout.WEST);
        contentPanel.add(messageLabel, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(isIndeterminate);
        progressBar.setStringPainted(false);
        progressBar.setBackground(Color.WHITE);
        progressBar.setForeground(PRIMARY_BLUE);
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        progressBar.setPreferredSize(new Dimension(300, 8));

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void setupAnimation() {

        animationTimer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dotCount = (dotCount + 1) % 4;
                StringBuilder dotsBuilder = new StringBuilder();
                for (int i = 0; i < dotCount; i++) {
                    dotsBuilder.append(".");
                }
                String dots = dotsBuilder.toString();
                messageLabel.setText(baseMessage + dots);
            }
        });
    }

    public void showLoading() {
        SwingUtilities.invokeLater(() -> {
            animationTimer.start();
            setVisible(true);
        });
    }

    public void hideLoading() {
        SwingUtilities.invokeLater(() -> {
            animationTimer.stop();
            setVisible(false);
            dispose();
        });
    }

    public void updateMessage(String newMessage) {
        SwingUtilities.invokeLater(() -> {
            this.baseMessage = newMessage;
            messageLabel.setText(newMessage);
        });
    }

    public void setProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            if (isIndeterminate) {
                progressBar.setIndeterminate(false);
                progressBar.setStringPainted(true);
                isIndeterminate = false;
            }
            progressBar.setValue(value);
            progressBar.setString(value + "%");
        });
    }

    public void setIndeterminate(boolean indeterminate) {
        SwingUtilities.invokeLater(() -> {
            this.isIndeterminate = indeterminate;
            progressBar.setIndeterminate(indeterminate);
            progressBar.setStringPainted(!indeterminate);
        });
    }

    public static LoadingDialog show(Frame parent, String message) {
        LoadingDialog dialog = new LoadingDialog(parent, message);

        SwingUtilities.invokeLater(() -> dialog.showLoading());
        return dialog;
    }

    public static LoadingDialog show(Frame parent, String title, String message) {
        LoadingDialog dialog = new LoadingDialog(parent, title, message);
        SwingUtilities.invokeLater(() -> dialog.showLoading());
        return dialog;
    }

    @Override
    public void dispose() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        super.dispose();
    }
}
