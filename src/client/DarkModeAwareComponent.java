package client;

import javax.swing.*;
import java.awt.*;

public abstract class DarkModeAwareComponent extends JPanel implements DarkModeManager.DarkModeListener {
    protected DarkModeManager darkModeManager;
    private boolean isListenerRegistered = false;

    public DarkModeAwareComponent() {
        super();
        initializeDarkMode();
    }

    public DarkModeAwareComponent(LayoutManager layout) {
        super(layout);
        initializeDarkMode();
    }

    private void initializeDarkMode() {
        darkModeManager = DarkModeManager.getInstance();
        registerDarkModeListener();
        applyCurrentTheme();
    }

    private void registerDarkModeListener() {
        if (!isListenerRegistered) {
            darkModeManager.addDarkModeListener(this);
            isListenerRegistered = true;
        }
    }

    protected void applyCurrentTheme() {
        SwingUtilities.invokeLater(() -> {
            updateTheme(darkModeManager.isDarkMode());
            darkModeManager.applyDarkMode(this);
        });
    }

    @Override
    public void onDarkModeChanged(boolean isDarkMode) {
        SwingUtilities.invokeLater(() -> {
            updateTheme(isDarkMode);
            darkModeManager.applyDarkMode(this);
        });
    }

    protected abstract void updateTheme(boolean isDarkMode);

    public void cleanup() {
        if (isListenerRegistered) {
            darkModeManager.removeDarkModeListener(this);
            isListenerRegistered = false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    protected Color getCurrentBackgroundColor() {
        return darkModeManager.getBackgroundColor();
    }

    protected Color getCurrentTextColor() {
        return darkModeManager.getTextColor();
    }

    protected Color getCurrentSecondaryBackgroundColor() {
        return darkModeManager.getSecondaryBackgroundColor();
    }

    protected Color getCurrentBorderColor() {
        return darkModeManager.getBorderColor();
    }

    protected JButton createThemedButton(String text) {
        JButton button = new JButton(text);
        applyButtonTheme(button);
        return button;
    }

    protected void applyButtonTheme(JButton button) {
        if (darkModeManager.isDarkMode()) {
            button.setBackground(DarkModeManager.DarkTheme.TERTIARY_BG);
            button.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
            button.setBorder(BorderFactory.createLineBorder(DarkModeManager.DarkTheme.BORDER_COLOR));
        } else {
            button.setBackground(DarkModeManager.LightTheme.TERTIARY_BG);
            button.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
            button.setBorder(BorderFactory.createLineBorder(DarkModeManager.LightTheme.BORDER_COLOR));
        }
        button.setFocusPainted(false);
    }

    protected JLabel createThemedLabel(String text) {
        JLabel label = new JLabel(text);
        applyLabelTheme(label);
        return label;
    }

    protected void applyLabelTheme(JLabel label) {
        if (darkModeManager.isDarkMode()) {
            label.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
        } else {
            label.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
        }
    }

    protected JTextField createThemedTextField() {
        JTextField textField = new JTextField();
        applyTextFieldTheme(textField);
        return textField;
    }

    protected void applyTextFieldTheme(JTextField textField) {
        if (darkModeManager.isDarkMode()) {
            textField.setBackground(DarkModeManager.DarkTheme.SURFACE_BG);
            textField.setForeground(DarkModeManager.DarkTheme.PRIMARY_TEXT);
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkModeManager.DarkTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            textField.setCaretColor(DarkModeManager.DarkTheme.PRIMARY_TEXT);
        } else {
            textField.setBackground(DarkModeManager.LightTheme.SURFACE_BG);
            textField.setForeground(DarkModeManager.LightTheme.PRIMARY_TEXT);
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkModeManager.LightTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            textField.setCaretColor(DarkModeManager.LightTheme.PRIMARY_TEXT);
        }
    }

    protected JPanel createThemedPanel() {
        JPanel panel = new JPanel();
        applyPanelTheme(panel);
        return panel;
    }

    protected void applyPanelTheme(JPanel panel) {
        if (darkModeManager.isDarkMode()) {
            panel.setBackground(DarkModeManager.DarkTheme.SECONDARY_BG);
        } else {
            panel.setBackground(DarkModeManager.LightTheme.SECONDARY_BG);
        }
    }
}
