package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.*;
import java.time.format.DateTimeParseException;

public class BorrowListUI extends JFrame {
    private int userId;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public BorrowListUI(int userId) {
        this.userId = userId;
        setTitle("Sách đang mượn");
        setSize(1900, 1000);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        showBorrowedBooksDialog();
    }

    private void connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            in.readLine();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Không thể kết nối đến server: " + ex.getMessage(),
                "Lỗi kết nối",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showBorrowedBooksDialog() {
        if (userId == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng đăng nhập để xem sách đã mượn!");
            return;
        }

        try {
            if (socket == null || socket.isClosed() || out == null || in == null) {
                connectToServer();
            }

            out.println("LIST_BORROWED|" + userId);
            String resp = in.readLine();

            if (resp != null && resp.startsWith("BORROWED_LIST|")) {
                String data = resp.substring("BORROWED_LIST|".length());
                String[] books = data.split(";");

                if (books.length > 0 && !books[0].isEmpty()) {
                    createBorrowedBooksDialog(books);
                } else {
                    JOptionPane.showMessageDialog(this, "Bạn chưa mượn sách nào!", "Sách đang mượn", JOptionPane.INFORMATION_MESSAGE);
                    this.dispose();
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không thể tải danh sách sách đã mượn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                this.dispose();
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            this.dispose();
            return;
        }
    }

    private void createBorrowedBooksDialog(String[] books) {

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel headerLabel = new JLabel("Danh sách sách đang mượn", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerLabel.setForeground(new Color(220, 53, 69));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        String[] columnNames = {"STT", "Tên sách", "Tác giả", "Ngày mượn", "Hạn trả", "Trạng thái"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int i = 0; i < books.length; i++) {
            String book = books[i].trim();
            if (!book.isEmpty()) {
                String[] parts = book.split(",");
                if (parts.length >= 4) {
                    String status = "Đang mượn";
                    model.addRow(new Object[]{
                        i + 1,
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        status
                    });
                }
            }
        }

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(50);
        table.setSelectionBackground(new Color(220, 53, 69, 50));
        table.setSelectionForeground(new Color(220, 53, 69));
        table.setGridColor(new Color(222, 226, 230));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(220, 53, 69));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 53, 69), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel infoPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        infoPanel.setOpaque(false);

        int overdueCount = 0;
        int nearDueCount = 0;
        ZonedDateTime now = ZonedDateTime.now();
        for (int r = 0; r < model.getRowCount(); r++) {
            try {
                Object dueObj = model.getValueAt(r, 4);
                if (dueObj != null) {
                    String dueStr = dueObj.toString().trim();
                    if (!dueStr.isEmpty()) {
                        ZonedDateTime dueDateTime;
                        try {
                            LocalDateTime ldt = LocalDateTime.parse(dueStr);
                            dueDateTime = ldt.atZone(ZoneId.systemDefault());
                        } catch (DateTimeParseException ex1) {
                            try {
                                LocalDate ld = LocalDate.parse(dueStr);
                                dueDateTime = ld.atStartOfDay(ZoneId.systemDefault());
                            } catch (DateTimeParseException ex2) {
                                continue;
                            }
                        }

                        Duration diff = Duration.between(now, dueDateTime);
                        if (diff.isNegative()) {
                            overdueCount++;
                        } else if (diff.toHours() <= 24) {
                            nearDueCount++;
                        }
                    }
                }
            } catch (Exception ignore) {

            }
        }

        JPanel totalPanel = createBorrowInfoCard("Tổng số sách", String.valueOf(model.getRowCount()), new Color(220, 53, 69));
        JPanel overduePanel = createBorrowInfoCard("Quá hạn", String.valueOf(overdueCount), new Color(255, 193, 7));
        JPanel nearDuePanel = createBorrowInfoCard("Sắp hết hạn", String.valueOf(nearDueCount), new Color(255, 108, 0));

        infoPanel.add(totalPanel);
        infoPanel.add(overduePanel);
        infoPanel.add(nearDuePanel);

        class DueDateRowRenderer extends DefaultTableCellRenderer {
            private final Color nearDueBg = new Color(255, 235, 235);
            private final Color overdueBg = new Color(255, 220, 220);
            private final Color nearDueFg = new Color(153, 0, 0);
            private final Color overdueFg = new Color(102, 0, 0);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object dueObj = table.getModel().getValueAt(modelRow, 4);
                    if (dueObj != null) {
                        String dueStr = dueObj.toString().trim();
                        if (!dueStr.isEmpty()) {
                            ZonedDateTime nowZ = ZonedDateTime.now();
                            ZonedDateTime dueDateTime;
                            try {
                                LocalDateTime ldt = LocalDateTime.parse(dueStr);
                                dueDateTime = ldt.atZone(ZoneId.systemDefault());
                            } catch (DateTimeParseException ex1) {
                                try {
                                    LocalDate ld = LocalDate.parse(dueStr);
                                    dueDateTime = ld.atStartOfDay(ZoneId.systemDefault());
                                } catch (DateTimeParseException ex2) {
                                    return c;
                                }
                            }

                            Duration diff = Duration.between(nowZ, dueDateTime);
                            if (diff.isNegative()) {
                                if (!isSelected) {
                                    c.setBackground(overdueBg);
                                    c.setForeground(overdueFg);
                                }
                            } else if (diff.toHours() <= 24) {
                                if (!isSelected) {
                                    c.setBackground(nearDueBg);
                                    c.setForeground(nearDueFg);
                                }
                            } else {
                                if (!isSelected) {
                                    c.setBackground(Color.WHITE);
                                    c.setForeground(new Color(33, 37, 41));
                                }
                            }
                        }
                    }
                } catch (Exception ex) {

                }
                return c;
            }
        }

        TableCellRenderer dueRenderer = new DueDateRowRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(dueRenderer);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton refreshBtn = new JButton("Làm mới");
        refreshBtn.setPreferredSize(new Dimension(120, 40));
        refreshBtn.setBackground(new Color(40, 167, 69));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> {
            dispose();
            new BorrowListUI(userId).setVisible(true);
        });

        JButton closeBtn = new JButton("Đóng");
        closeBtn.setPreferredSize(new Dimension(100, 40));
        closeBtn.setBackground(new Color(220, 53, 69));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(closeBtn);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        setVisible(true);
    }

    private JPanel createBorrowInfoCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color startColor = Color.WHITE;
                Color endColor = new Color(248, 249, 250);
                GradientPaint gradient = new GradientPaint(0, 0, startColor, 0, getHeight(), endColor);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(color);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(new Color(33, 37, 41));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }
}
