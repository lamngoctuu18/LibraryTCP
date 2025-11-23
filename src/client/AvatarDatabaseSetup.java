package client;

import java.sql.*;

public class AvatarDatabaseSetup {

    public static void main(String[] args) {
        setupTestUsers();
    }

    public static void setupTestUsers() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/data/library.db?busy_timeout=30000")) {
            System.out.println("🔌 Connected to database");

            System.out.println("\n📋 Current users in database:");
            String selectSQL = "SELECT id, username, avatar FROM users";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(selectSQL);

            boolean hasUsers = false;
            while (rs.next()) {
                hasUsers = true;
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String avatar = rs.getString("avatar");
                System.out.println("  👤 ID: " + id + " | Username: " + username + " | Avatar: " +
                    (avatar != null && !avatar.isEmpty() ? avatar : "null/empty"));
            }

            if (!hasUsers) {
                System.out.println("  ❌ No users found in database");
            }

            String checkAdminSQL = "SELECT id, avatar FROM users WHERE username = 'admin'";
            PreparedStatement checkStmt = conn.prepareStatement(checkAdminSQL);
            ResultSet adminRs = checkStmt.executeQuery();

            if (adminRs.next()) {
                int adminId = adminRs.getInt("id");
                String adminAvatar = adminRs.getString("avatar");

                if (adminAvatar == null || adminAvatar.trim().isEmpty()) {
                    System.out.println("\n🔄 Updating admin avatar...");

                    String[] testAvatars = {
                        "https://api.dicebear.com/7.x/avataaars/png?seed=admin",
                        "https://api.dicebear.com/7.x/bottts/png?seed=admin",
                        "https://picsum.photos/100/100?random=1",
                        "https://via.placeholder.com/100x100/3498db/ffffff?text=A"
                    };

                    String selectedAvatar = testAvatars[0];

                    String updateSQL = "UPDATE users SET avatar = ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                    updateStmt.setString(1, selectedAvatar);
                    updateStmt.setInt(2, adminId);

                    int rowsUpdated = updateStmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        System.out.println("✅ Admin avatar updated to: " + selectedAvatar);
                    } else {
                        System.out.println("❌ Failed to update admin avatar");
                    }
                    updateStmt.close();
                } else {
                    System.out.println("✅ Admin already has avatar: " + adminAvatar);
                }
            } else {
                System.out.println("❌ Admin user not found");
            }

            System.out.println("\n🆕 Creating additional test users...");
            String[] testUsers = {
                "user1|password1|user|0123456789|user1@example.com|https://api.dicebear.com/7.x/avataaars/png?seed=user1",
                "user2|password2|user|0987654321|user2@example.com|https://api.dicebear.com/7.x/bottts/png?seed=user2",
                "testuser|test123|user|0111222333|test@example.com|https://picsum.photos/100/100?random=2"
            };

            for (String userData : testUsers) {
                String[] parts = userData.split("\\|");
                if (parts.length == 6) {
                    String username = parts[0];
                    String password = parts[1];
                    String role = parts[2];
                    String phone = parts[3];
                    String email = parts[4];
                    String avatar = parts[5];

                    String checkUserSQL = "SELECT id FROM users WHERE username = ?";
                    PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSQL);
                    checkUserStmt.setString(1, username);
                    ResultSet userRs = checkUserStmt.executeQuery();

                    if (!userRs.next()) {

                        String insertSQL = "INSERT INTO users (username, password, role, phone, email, avatar) VALUES (?, ?, ?, ?, ?, ?)";
                        PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
                        insertStmt.setString(1, username);
                        insertStmt.setString(2, password);
                        insertStmt.setString(3, role);
                        insertStmt.setString(4, phone);
                        insertStmt.setString(5, email);
                        insertStmt.setString(6, avatar);

                        int rowsInserted = insertStmt.executeUpdate();
                        if (rowsInserted > 0) {
                            System.out.println("✅ Created user: " + username + " with avatar: " + avatar);
                        } else {
                            System.out.println("❌ Failed to create user: " + username);
                        }
                        insertStmt.close();
                    } else {
                        System.out.println("⚠️ User already exists: " + username);
                    }

                    userRs.close();
                    checkUserStmt.close();
                }
            }

            System.out.println("\n📋 Updated users list:");
            rs = stmt.executeQuery(selectSQL);
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String avatar = rs.getString("avatar");
                System.out.println("  👤 ID: " + id + " | Username: " + username + " | Avatar: " +
                    (avatar != null && !avatar.isEmpty() ? avatar : "null/empty"));
            }

            rs.close();
            stmt.close();
            checkStmt.close();
            adminRs.close();

            System.out.println("\n🎉 Database setup completed!");

        } catch (Exception e) {
            System.err.println("💥 Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
