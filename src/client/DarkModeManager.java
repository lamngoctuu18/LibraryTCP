package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class DarkModeManager {
    private static volatile DarkModeManager instance;
    private boolean isDarkMode = false;
    private List<DarkModeListener> listeners = new ArrayList<>();
    private Preferences prefs;

    public static class DarkTheme {

        public static final Color PRIMARY_BG = new Color(15, 17, 21);
        public static final Color SECONDARY_BG = new Color(24, 28, 34);
        public static final Color TERTIARY_BG = new Color(34, 40, 49);
        public static final Color SURFACE_BG = new Color(44, 51, 61);
        public static final Color CARD_BG = new Color(28, 33, 40);

        public static final Color PRIMARY_TEXT = new Color(251, 252, 253);
        public static final Color SECONDARY_TEXT = new Color(195, 202, 210);
        public static final Color MUTED_TEXT = new Color(158, 168, 179);

        public static final Color PRIMARY_ACCENT = new Color(88, 166, 255);
        public static final Color SUCCESS_ACCENT = new Color(64, 186, 106);
        public static final Color WARNING_ACCENT = new Color(255, 183, 77);
        public static final Color DANGER_ACCENT = new Color(255, 107, 107);
        public static final Color INFO_ACCENT = new Color(84, 199, 236);

        public static final Color BORDER_COLOR = new Color(54, 63, 77);
        public static final Color FOCUS_BORDER = new Color(88, 166, 255);
        public static final Color HOVER_BORDER = new Color(74, 144, 226);

        public static final Color HOVER_BG = new Color(54, 63, 77);
        public static final Color SELECTED_BG = new Color(88, 166, 255, 25);
        public static final Color ACTIVE_BG = new Color(88, 166, 255, 40);
    }

    public static class LightTheme {

        public static final Color PRIMARY_BG = new Color(255, 255, 255);
        public static final Color SECONDARY_BG = new Color(248, 250, 252);
        public static final Color TERTIARY_BG = new Color(241, 245, 249);
        public static final Color SURFACE_BG = new Color(255, 255, 255);
        public static final Color CARD_BG = new Color(251, 252, 253);

        public static final Color PRIMARY_TEXT = new Color(15, 23, 42);
        public static final Color SECONDARY_TEXT = new Color(71, 85, 105);
        public static final Color MUTED_TEXT = new Color(100, 116, 139);

        public static final Color PRIMARY_ACCENT = new Color(59, 130, 246);
        public static final Color SUCCESS_ACCENT = new Color(34, 197, 94);
        public static final Color WARNING_ACCENT = new Color(251, 191, 36);
        public static final Color DANGER_ACCENT = new Color(239, 68, 68);
        public static final Color INFO_ACCENT = new Color(14, 165, 233);

        public static final Color BORDER_COLOR = new Color(226, 232, 240);
        public static final Color FOCUS_BORDER = new Color(59, 130, 246);
        public static final Color HOVER_BORDER = new Color(37, 99, 235);

        public static final Color HOVER_BG = new Color(248, 250, 252);
        public static final Color SELECTED_BG = new Color(59, 130, 246, 20);
        public static final Color ACTIVE_BG = new Color(59, 130, 246, 35);
    }

    private DarkModeManager() {
        prefs = Preferences.userNodeForPackage(DarkModeManager.class);
        loadSettings();
    }

    public static DarkModeManager getInstance() {
        if (instance == null) {
            synchronized (DarkModeManager.class) {
                if (instance == null) {
                    instance = new DarkModeManager();
                }
            }
        }
        return instance;
    }

    public void toggleDarkMode() {
        setDarkMode(!isDarkMode);
    }

    public void setDarkMode(boolean enabled) {
        if (this.isDarkMode != enabled) {
            this.isDarkMode = enabled;
            saveSettings();
            notifyListeners();
            updateLookAndFeel();
        }
    }

    public boolean isDarkMode() {
        return isDarkMode;
    }

    public Color getBackgroundColor() {
        return isDarkMode ? DarkTheme.PRIMARY_BG : LightTheme.PRIMARY_BG;
    }

    public Color getSecondaryBackgroundColor() {
        return isDarkMode ? DarkTheme.SECONDARY_BG : LightTheme.SECONDARY_BG;
    }

    public Color getTertiaryBackgroundColor() {
        return isDarkMode ? DarkTheme.TERTIARY_BG : LightTheme.TERTIARY_BG;
    }

    public Color getTextColor() {
        return isDarkMode ? DarkTheme.PRIMARY_TEXT : LightTheme.PRIMARY_TEXT;
    }

    public Color getSecondaryTextColor() {
        return isDarkMode ? DarkTheme.SECONDARY_TEXT : LightTheme.SECONDARY_TEXT;
    }

    public Color getBorderColor() {
        return isDarkMode ? DarkTheme.BORDER_COLOR : LightTheme.BORDER_COLOR;
    }

    public Color getHoverBackgroundColor() {
        return isDarkMode ? DarkTheme.HOVER_BG : LightTheme.HOVER_BG;
    }

    public Color getSelectedBackgroundColor() {
        return isDarkMode ? DarkTheme.SELECTED_BG : LightTheme.SELECTED_BG;
    }

    private void updateLookAndFeel() {
        try {
            if (isDarkMode) {

                UIManager.put("Panel.background", DarkTheme.PRIMARY_BG);
                UIManager.put("OptionPane.background", DarkTheme.SECONDARY_BG);
                UIManager.put("Button.background", DarkTheme.TERTIARY_BG);
                UIManager.put("Button.foreground", DarkTheme.PRIMARY_TEXT);
                UIManager.put("Label.foreground", DarkTheme.PRIMARY_TEXT);
                UIManager.put("TextField.background", DarkTheme.SURFACE_BG);
                UIManager.put("TextField.foreground", DarkTheme.PRIMARY_TEXT);
                UIManager.put("TextField.border", BorderFactory.createLineBorder(DarkTheme.BORDER_COLOR));
                UIManager.put("ComboBox.background", DarkTheme.SURFACE_BG);
                UIManager.put("ComboBox.foreground", DarkTheme.PRIMARY_TEXT);
                UIManager.put("Table.background", DarkTheme.SECONDARY_BG);
                UIManager.put("Table.foreground", DarkTheme.PRIMARY_TEXT);
                UIManager.put("Table.gridColor", DarkTheme.BORDER_COLOR);
                UIManager.put("TableHeader.background", DarkTheme.TERTIARY_BG);
                UIManager.put("TableHeader.foreground", DarkTheme.PRIMARY_TEXT);
                UIManager.put("ScrollPane.background", DarkTheme.PRIMARY_BG);
                UIManager.put("Viewport.background", DarkTheme.PRIMARY_BG);
            } else {

                UIManager.put("Panel.background", LightTheme.PRIMARY_BG);
                UIManager.put("OptionPane.background", LightTheme.SECONDARY_BG);
                UIManager.put("Button.background", LightTheme.TERTIARY_BG);
                UIManager.put("Button.foreground", LightTheme.PRIMARY_TEXT);
                UIManager.put("Label.foreground", LightTheme.PRIMARY_TEXT);
                UIManager.put("TextField.background", LightTheme.SURFACE_BG);
                UIManager.put("TextField.foreground", LightTheme.PRIMARY_TEXT);
                UIManager.put("TextField.border", BorderFactory.createLineBorder(LightTheme.BORDER_COLOR));
                UIManager.put("ComboBox.background", LightTheme.SURFACE_BG);
                UIManager.put("ComboBox.foreground", LightTheme.PRIMARY_TEXT);
                UIManager.put("Table.background", LightTheme.SECONDARY_BG);
                UIManager.put("Table.foreground", LightTheme.PRIMARY_TEXT);
                UIManager.put("Table.gridColor", LightTheme.BORDER_COLOR);
                UIManager.put("TableHeader.background", LightTheme.TERTIARY_BG);
                UIManager.put("TableHeader.foreground", LightTheme.PRIMARY_TEXT);
                UIManager.put("ScrollPane.background", LightTheme.PRIMARY_BG);
                UIManager.put("Viewport.background", LightTheme.PRIMARY_BG);
            }

        } catch (Exception e) {
            System.err.println("Error updating Look and Feel: " + e.getMessage());
        }
    }

    public void applyDarkMode(JComponent component) {
        if (component == null) return;

        if (isDarkMode) {
            applyDarkTheme(component);
        } else {
            applyLightTheme(component);
        }

        for (Component child : component.getComponents()) {
            if (child instanceof JComponent) {
                applyDarkMode((JComponent) child);
            }
        }

        component.repaint();
    }

    private void applyDarkTheme(JComponent component) {
        if (component instanceof JPanel) {

            component.setBackground(DarkTheme.SECONDARY_BG);
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;

            Color currentBg = button.getBackground();

            if (isSpecialColoredButton(currentBg)) {

                adjustButtonForDarkMode(button, currentBg);
            } else {

                button.setBackground(DarkTheme.TERTIARY_BG);
                button.setForeground(DarkTheme.PRIMARY_TEXT);
                button.setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER_COLOR, 1));
            }
        } else if (component instanceof JTextField || component instanceof JTextArea) {
            component.setBackground(DarkTheme.SURFACE_BG);
            component.setForeground(DarkTheme.PRIMARY_TEXT);
            component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else if (component instanceof JLabel) {
            component.setForeground(DarkTheme.PRIMARY_TEXT);
        } else if (component instanceof JTable) {
            component.setBackground(DarkTheme.CARD_BG);
            component.setForeground(DarkTheme.PRIMARY_TEXT);
            ((JTable) component).setGridColor(DarkTheme.BORDER_COLOR);

            if (((JTable) component).getTableHeader() != null) {
                ((JTable) component).getTableHeader().setBackground(DarkTheme.TERTIARY_BG);
                ((JTable) component).getTableHeader().setForeground(DarkTheme.PRIMARY_TEXT);
            }
        } else if (component instanceof JComboBox) {
            component.setBackground(DarkTheme.SURFACE_BG);
            component.setForeground(DarkTheme.PRIMARY_TEXT);
        } else {

            component.setBackground(DarkTheme.PRIMARY_BG);
            component.setForeground(DarkTheme.PRIMARY_TEXT);
        }
    }

    private void applyLightTheme(JComponent component) {
        if (component instanceof JPanel) {

            component.setBackground(LightTheme.SECONDARY_BG);
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;

            Color currentBg = button.getBackground();

            if (isSpecialColoredButton(currentBg)) {

                restoreButtonOriginalColors(button, currentBg);
            } else {

                button.setBackground(LightTheme.TERTIARY_BG);
                button.setForeground(LightTheme.PRIMARY_TEXT);
                button.setBorder(BorderFactory.createLineBorder(LightTheme.BORDER_COLOR, 1));
            }
        } else if (component instanceof JTextField || component instanceof JTextArea) {
            component.setBackground(LightTheme.SURFACE_BG);
            component.setForeground(LightTheme.PRIMARY_TEXT);
            component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LightTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
        } else if (component instanceof JLabel) {
            component.setForeground(LightTheme.PRIMARY_TEXT);
        } else if (component instanceof JTable) {
            component.setBackground(LightTheme.CARD_BG);
            component.setForeground(LightTheme.PRIMARY_TEXT);
            ((JTable) component).setGridColor(LightTheme.BORDER_COLOR);

            if (((JTable) component).getTableHeader() != null) {
                ((JTable) component).getTableHeader().setBackground(LightTheme.TERTIARY_BG);
                ((JTable) component).getTableHeader().setForeground(LightTheme.PRIMARY_TEXT);
            }
        } else if (component instanceof JComboBox) {
            component.setBackground(LightTheme.SURFACE_BG);
            component.setForeground(LightTheme.PRIMARY_TEXT);
        } else {

            component.setBackground(LightTheme.PRIMARY_BG);
            component.setForeground(LightTheme.PRIMARY_TEXT);
        }
    }

    private boolean isSpecialColoredButton(Color currentColor) {
        if (currentColor == null) return false;

        if (currentColor.equals(UIManager.getColor("Button.background")) ||
            currentColor.equals(new Color(238, 238, 238)) ||
            currentColor.equals(new Color(240, 240, 240)) ||
            currentColor.equals(Color.LIGHT_GRAY) ||
            currentColor.equals(Color.WHITE)) {
            return false;
        }

        return true;
    }

    private void adjustButtonForDarkMode(JButton button, Color originalColor) {

        Color adjustedColor = darkenColor(originalColor, 0.3f);
        button.setBackground(adjustedColor);

        button.setForeground(DarkTheme.PRIMARY_TEXT);

        Color borderColor = darkenColor(originalColor, 0.5f);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private void restoreButtonOriginalColors(JButton button, Color currentColor) {

        Color restoredColor = lightenColor(currentColor, 0.2f);
        button.setBackground(restoredColor);

        button.setForeground(Color.WHITE);

        Color borderColor = lightenColor(restoredColor, 0.1f);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(
            Math.max(0, (int) (color.getRed() * (1 - factor))),
            Math.max(0, (int) (color.getGreen() * (1 - factor))),
            Math.max(0, (int) (color.getBlue() * (1 - factor))),
            color.getAlpha()
        );
    }

    private Color lightenColor(Color color, float factor) {
        return new Color(
            Math.min(255, (int) (color.getRed() + (255 - color.getRed()) * factor)),
            Math.min(255, (int) (color.getGreen() + (255 - color.getGreen()) * factor)),
            Math.min(255, (int) (color.getBlue() + (255 - color.getBlue()) * factor)),
            color.getAlpha()
        );
    }

    public void addDarkModeListener(DarkModeListener listener) {
        listeners.add(listener);
    }

    public void removeDarkModeListener(DarkModeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (DarkModeListener listener : listeners) {
            try {
                listener.onDarkModeChanged(isDarkMode);
            } catch (Exception e) {
                System.err.println("Error notifying dark mode listener: " + e.getMessage());
            }
        }
    }

    private void saveSettings() {
        prefs.putBoolean("darkMode", isDarkMode);
    }

    private void loadSettings() {
        isDarkMode = prefs.getBoolean("darkMode", false);
    }

    public interface DarkModeListener {
        void onDarkModeChanged(boolean isDarkMode);
    }

    public JButton createDarkModeToggleButton() {
        JButton toggleButton = new JButton();
        updateToggleButtonAppearance(toggleButton);

        toggleButton.addActionListener(e -> {
            toggleDarkMode();
            updateToggleButtonAppearance(toggleButton);
        });

        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        toggleButton.setToolTipText(isDarkMode ? "Chuyển sang Light Mode" : "Chuyển sang Dark Mode");

        return toggleButton;
    }

    private void updateToggleButtonAppearance(JButton button) {
        if (isDarkMode) {
            button.setText("☀️ Light");
            button.setBackground(new Color(255, 193, 7));
            button.setForeground(Color.BLACK);
            button.setToolTipText("Chuyển sang Light Mode");
        } else {
            button.setText("🌙 Dark");
            button.setBackground(new Color(52, 58, 64));
            button.setForeground(Color.WHITE);
            button.setToolTipText("Chuyển sang Dark Mode");
        }
    }
}
