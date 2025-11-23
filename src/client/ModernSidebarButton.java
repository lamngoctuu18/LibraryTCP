package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernSidebarButton extends JButton {
    private boolean isSelected = false;
    private boolean isHovered = false;
    private Timer animationTimer;
    private float animationProgress = 0.0f;
    private float targetProgress = 0.0f;

    private SidebarTheme.Theme theme;

    private String iconText;
    private String buttonText;

    public ModernSidebarButton(String icon, String text) {
        this.iconText = icon;
        this.buttonText = text;
        this.theme = SidebarTheme.getCurrentTheme();

        setupButton();
        setupAnimation();
        setupMouseListeners();
    }

    private void setupButton() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(230, 50));
        setMaximumSize(new Dimension(230, 50));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setContentAreaFilled(false);

        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        iconLabel.setForeground(theme.iconColor);
        iconLabel.setPreferredSize(new Dimension(25, 20));

        JLabel textLabel = new JLabel(buttonText);
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        textLabel.setForeground(theme.textColor);
        textLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));

        add(iconLabel, BorderLayout.WEST);
        add(textLabel, BorderLayout.CENTER);
    }

    private void setupAnimation() {
        animationTimer = new Timer(16, e -> {
            float speed = 0.15f;

            if (animationProgress < targetProgress) {
                animationProgress = Math.min(targetProgress, animationProgress + speed);
            } else if (animationProgress > targetProgress) {
                animationProgress = Math.max(targetProgress, animationProgress - speed);
            }

            repaint();

            if (Math.abs(animationProgress - targetProgress) < 0.01f) {
                animationProgress = targetProgress;
                animationTimer.stop();
            }
        });
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isSelected) {
                    isHovered = true;
                    animateToHover();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isSelected) {
                    isHovered = false;
                    animateToNormal();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

                animationProgress = 0.8f;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isSelected) {
                    animateToSelected();
                } else if (isHovered) {
                    animateToHover();
                } else {
                    animateToNormal();
                }
            }
        });
    }

    private void animateToNormal() {
        targetProgress = 0.0f;
        startAnimation();
    }

    private void animateToHover() {
        targetProgress = 0.3f;
        startAnimation();
    }

    private void animateToSelected() {
        targetProgress = 1.0f;
        startAnimation();
    }

    private void startAnimation() {
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        if (selected) {
            animateToSelected();
        } else if (isHovered) {
            animateToHover();
        } else {
            animateToNormal();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        Color bgColor = interpolateColor(theme.normalBg,
                                       isSelected ? theme.selectedBg : theme.hoverBg,
                                       animationProgress);

        int cornerRadius = 8;
        if (animationProgress > 0.01f) {
            g2d.setColor(bgColor);
            g2d.fillRoundRect(5, 2, width - 10, height - 4, cornerRadius, cornerRadius);
        }

        if (isSelected && animationProgress > 0.5f) {
            int barHeight = (int) ((height - 10) * (animationProgress - 0.5f) * 2);
            int barY = (height - barHeight) / 2;

            g2d.setColor(theme.accentColor);
            g2d.fillRoundRect(2, barY, 4, barHeight, 2, 2);
        }

        if (animationProgress > 0.2f) {
            float glowOpacity = (animationProgress - 0.2f) * 0.3f;
            Color glowColor = new Color(theme.accentColor.getRed(), theme.accentColor.getGreen(),
                                      theme.accentColor.getBlue(), (int)(glowOpacity * 255));

            g2d.setColor(glowColor);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(4, 1, width - 8, height - 2, cornerRadius + 1, cornerRadius + 1);
        }

        g2d.dispose();
        super.paintComponent(g);
    }

    private Color interpolateColor(Color start, Color end, float progress) {
        progress = Math.max(0, Math.min(1, progress));

        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * progress);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * progress);

        return new Color(r, g, b, a);
    }

    public void cleanup() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }
}
