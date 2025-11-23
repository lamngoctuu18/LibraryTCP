package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import dao.UserDAO;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SessionManager.ClientSession session;
    private String clientIdentifier;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientIdentifier = socket.getRemoteSocketAddress().toString();
        this.session = SessionManager.createSession(clientIdentifier);
        System.out.println("[INFO] New client handler created for: " + clientIdentifier);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("WELCOME|Library TCP Server");

            String request = null;
            while (true) {
                try {
                    request = in.readLine();
                    if (request == null) {
                        System.out.println("[ClientHandler] Client disconnected: " + socket.getRemoteSocketAddress());
                        break;
                    }
                } catch (java.net.SocketTimeoutException ste) {
                    continue;
                } catch (java.net.SocketException se) {
                    System.out.println("[ClientHandler] SocketException from " + socket.getRemoteSocketAddress() + ": " + se.getMessage());
                    break;
                } catch (java.io.IOException ioe) {
                    System.out.println("[ClientHandler] IOException while reading from " + socket.getRemoteSocketAddress() + ": " + ioe.getMessage());
                    break;
                }

                System.out.println("REQ: " + request);
                String[] parts = request.split("\\|", -1);
                String cmd = parts[0];

                switch (cmd) {
                    case "LOGIN":
                        handleLogin(parts);
                        break;
                    case "REGISTER":
                        handleRegister(parts);
                        break;
                    case "SEARCH":
                        handleSearch(parts);
                        break;
                    case "BORROW":
                        handleBorrow(parts);
                        break;
                    case "RETURN":
                        handleReturn(parts);
                        break;
                    case "ADD_BOOK":
                        handleAddBook(parts);
                        break;
                    case "DELETE_BOOK":
                        handleDeleteBook(parts);
                        break;
                    case "LIST_BORROWS":
                        handleListBorrows();
                        break;
                    case "FAVORITE":
                        handleFavorite(parts);
                        break;
                    case "LIST_ACTIVITIES":
                        handleListActivities(parts);
                        break;
                    case "LIST_FAVORITES":
                        handleListFavorites(parts);
                        break;
                    case "LIST_BORROWED":
                        handleListBorrowed(parts);
                        break;
                    case "EXIT":
                        out.println("BYE");
                        socket.close();
                        return;
                    default:
                        out.println("ERROR|Unknown command");
                }
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Client handler error for " + clientIdentifier + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up session
            if (session != null) {
                SessionManager.removeSession(session.getSessionId());
            }
            try { 
                if (socket != null && !socket.isClosed()) {
                    socket.close(); 
                    System.out.println("[INFO] Connection closed for: " + clientIdentifier);
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Error closing socket: " + e.getMessage());
            }
            MetricsCollector.connectionClosed();
        }
    }

    private Connection getConnection() throws Exception {
        return DatabasePool.getInstance().getConnection();
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) { 
            out.println(ResponseFormatter.error("LOGIN", "Missing username or password"));
            return; 
        }
        String username = InputValidator.sanitizeInput(parts[1]);
        String password = parts[2]; // Don't sanitize password
        
        if (!InputValidator.isValidUsername(username)) {
            out.println(ResponseFormatter.error("LOGIN", "Invalid username format"));
            return;
        }

        // Hard-coded admin for backward compatibility
        if ("admin".equals(username) && "admin".equals(password)) {
            session.authenticate(username, "admin");
            
            java.util.Map<String, Object> userData = new java.util.HashMap<>();
            userData.put("id", 1);
            userData.put("username", "admin");
            userData.put("role", "admin");
            userData.put("sessionId", clientIdentifier);
            
            out.println(ResponseFormatter.success("LOGIN", userData));
            System.out.println("[INFO] Admin login successful from: " + clientIdentifier);
            return;
        }

        try (Connection conn = getConnection()) {
            // Add status column if not exists
            try {
                Statement stmt = conn.createStatement();
                stmt.execute("ALTER TABLE users ADD COLUMN status TEXT DEFAULT 'active'");
            } catch (SQLException e) {
                // Column already exists
            }

            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, role, status, password FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String dbPassword = rs.getString("password");
                if (!PasswordUtil.verifyPassword(password, dbPassword)) {
                    out.println(ResponseFormatter.error("LOGIN", "Invalid credentials"));
                    System.err.println("[SECURITY] Failed login attempt for user: " + username + " from: " + clientIdentifier);
                    return;
                }
                
                int id = rs.getInt("id");
                String role = rs.getString("role");
                String status = rs.getString("status");

                if ("locked".equals(status)) {
                    out.println(ResponseFormatter.error("LOGIN", "Tài khoản của bạn đã bị khóa, vui lòng đến thư viện hoặc liên hệ số 1900 2004 để biết chi tiết"));
                    System.err.println("[SECURITY] Locked account login attempt: " + username + " from: " + clientIdentifier);
                    return;
                }

                session.authenticate(username, role);
                
                java.util.Map<String, Object> userData = new java.util.HashMap<>();
                userData.put("id", id);
                userData.put("username", username);
                userData.put("role", role);
                userData.put("sessionId", clientIdentifier);
                
                out.println(ResponseFormatter.success("LOGIN", userData));
                System.out.println("[INFO] User login successful: " + username + " (" + role + ") from: " + clientIdentifier);
            } else {
                out.println(ResponseFormatter.error("LOGIN", "Invalid credentials"));
                System.err.println("[SECURITY] Failed login attempt for unknown user: " + username + " from: " + clientIdentifier);
            }
        } catch (Exception e) {
            out.println(ResponseFormatter.error("LOGIN", e.getMessage()));
            System.err.println("[ERROR] Login error for user " + username + ": " + e.getMessage());
        }
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 6) { out.println("REGISTER_FAIL|Missing params"); return; }
        
        String username = InputValidator.sanitizeInput(parts[1]);
        String password = parts[2]; // Don't sanitize password
        String phone = InputValidator.sanitizeInput(parts[3]);
        String email = InputValidator.sanitizeInput(parts[4]);
        String avatar = parts.length > 5 ? InputValidator.sanitizeInput(parts[5]) : "";
        String role = "user";
        
        // Validate all inputs
        if (!InputValidator.isValidUsername(username)) {
            out.println("REGISTER_FAIL|" + InputValidator.getValidationError("username", username));
            return;
        }
        if (!InputValidator.isValidPassword(password)) {
            out.println("REGISTER_FAIL|" + InputValidator.getValidationError("password", password));
            return;
        }
        if (!InputValidator.isValidEmail(email)) {
            out.println("REGISTER_FAIL|" + InputValidator.getValidationError("email", email));
            return;
        }
        if (!InputValidator.isValidPhone(phone)) {
            out.println("REGISTER_FAIL|" + InputValidator.getValidationError("phone", phone));
            return;
        }
        
        // Hash password before storing
        String hashedPassword = PasswordUtil.hashPassword(password);
        
        try {
            UserDAO dao = new UserDAO();
            int result = dao.createUser(username, hashedPassword, role, phone, email, avatar);
            if (result > 0) {
                out.println("REGISTER_SUCCESS");
                System.out.println("[INFO] User registered successfully: " + username);
            } else {
                out.println("REGISTER_FAIL|Could not create user");
                System.err.println("[ERROR] Failed to create user: " + username);
            }
        } catch (Exception e) {
            out.println("REGISTER_FAIL|" + e.getMessage());
            System.err.println("[ERROR] Registration error for user " + username + ": " + e.getMessage());
        }
    }

    private void handleSearch(String[] parts) {
        String keyword = parts.length > 1 ? InputValidator.validateSearchKeyword(parts[1]) : "";
        
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, title, author, publisher, year, quantity FROM books WHERE title LIKE ? OR author LIKE ? LIMIT 100");
            String k = "%" + keyword + "%";
            ps.setString(1, k);
            ps.setString(2, k);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("SEARCH_RESULT|");
            int count = 0;
            while (rs.next() && count < 100) {
                sb.append(rs.getInt("id")).append(",")
                  .append(rs.getString("title")).append(",")
                  .append(rs.getString("author")).append(",")
                  .append(rs.getString("publisher")).append(",")
                  .append(rs.getString("year")).append(",")
                  .append(rs.getInt("quantity")).append(";");
                count++;
            }
            out.println(sb.toString());
            System.out.println("[INFO] Search performed: \"" + keyword + "\" returned " + count + " results");
        } catch (Exception e) {
            out.println("SEARCH_FAIL|" + e.getMessage());
            System.err.println("[ERROR] Search error for keyword \"" + keyword + "\": " + e.getMessage());
        }
    }

    private void handleBorrow(String[] parts) {
        if (parts.length < 3) { out.println("BORROW_FAIL|Missing params"); return; }
        
        int userId, bookId;
        try {
            userId = Integer.parseInt(parts[1]);
            bookId = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println("BORROW_FAIL|Invalid parameters");
            return;
        }
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            PreparedStatement checkActive = conn.prepareStatement(
                "SELECT COUNT(*) FROM borrows WHERE user_id = ? AND book_id = ? AND return_date IS NULL");
            checkActive.setInt(1, userId);
            checkActive.setInt(2, bookId);
            ResultSet rsActive = checkActive.executeQuery();
            if (rsActive.next() && rsActive.getInt(1) > 0) {
                conn.rollback();
                out.println("BORROW_FAIL|Already borrowed this book");
                return;
            }
            
            PreparedStatement updateStock = conn.prepareStatement(
                "UPDATE books SET quantity = quantity - 1 WHERE id = ? AND quantity > 0");
            updateStock.setInt(1, bookId);
            int affected = updateStock.executeUpdate();
            if (affected == 0) {
                conn.rollback();
                out.println("BORROW_FAIL|Book not available or out of stock");
                return;
            }

            PreparedStatement borrow = conn.prepareStatement(
                "INSERT INTO borrows(user_id, book_id, borrow_date) VALUES(?,?,date('now'))");
            borrow.setInt(1, userId);
            borrow.setInt(2, bookId);
            borrow.executeUpdate();

            PreparedStatement act = conn.prepareStatement(
                "INSERT INTO activities(user_id, book_id, action, action_time) VALUES(?,?,?,datetime('now'))");
            act.setInt(1, userId);
            act.setInt(2, bookId);
            act.setString(3, "borrow");
            act.executeUpdate();

            conn.commit();
            out.println("BORROW_SUCCESS");
        } catch (Exception e) {
            out.println("BORROW_FAIL|" + e.getMessage());
        }
    }

    private void handleReturn(String[] parts) {
        if (parts.length < 3) { out.println("RETURN_FAIL|Missing params"); return; }
        int userId = Integer.parseInt(parts[1]);
        int bookId = Integer.parseInt(parts[2]);
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement ret = conn.prepareStatement(
                "UPDATE borrows SET return_date=date('now') WHERE user_id=? AND book_id=? AND return_date IS NULL");
            ret.setInt(1, userId);
            ret.setInt(2, bookId);
            int rows = ret.executeUpdate();
            if (rows > 0) {
                PreparedStatement update = conn.prepareStatement("UPDATE books SET quantity=quantity+1 WHERE id=?");
                update.setInt(1, bookId);
                update.executeUpdate();

                PreparedStatement act = conn.prepareStatement("INSERT INTO activities(user_id, book_id, action, action_time) VALUES(?,?,?,datetime('now'))");
                act.setInt(1, userId);
                act.setInt(2, bookId);
                act.setString(3, "return");
                act.executeUpdate();
                conn.commit();
                out.println("RETURN_SUCCESS");
            } else {
                conn.rollback();
                out.println("RETURN_FAIL|No active borrow");
            }
        } catch (Exception e) {
            out.println("RETURN_FAIL|" + e.getMessage());
        }
    }

    private void handleAddBook(String[] parts) {
        // Session-based admin authorization
        if (!isAdmin()) {
            out.println("ADD_BOOK_FAIL|Access denied - Admin privileges required");
            System.err.println("[SECURITY] Unauthorized ADD_BOOK attempt from: " + clientIdentifier);
            return;
        }
        
        if (parts.length < 5) {
            out.println("ADD_BOOK_FAIL|Missing required parameters: title, author, publisher, year, quantity");
            return;
        }
        
        // Sanitize and validate all inputs
        String title = InputValidator.sanitizeInput(parts[1]);
        String author = InputValidator.sanitizeInput(parts[2]);
        String publisher = InputValidator.sanitizeInput(parts[3]);
        String year = InputValidator.sanitizeInput(parts[4]);
        String quantity = parts.length > 5 ? InputValidator.sanitizeInput(parts[5]) : "1";
        
        // Comprehensive validation
        if (!InputValidator.isValidBookTitle(title)) {
            out.println("ADD_BOOK_FAIL|" + InputValidator.getValidationError("title", title));
            return;
        }
        if (!InputValidator.isValidAuthor(author)) {
            out.println("ADD_BOOK_FAIL|" + InputValidator.getValidationError("author", author));
            return;
        }
        if (publisher.trim().isEmpty() || publisher.length() > 100) {
            out.println("ADD_BOOK_FAIL|Publisher must be 1-100 characters");
            return;
        }
        if (!InputValidator.isValidYear(year)) {
            out.println("ADD_BOOK_FAIL|" + InputValidator.getValidationError("year", year));
            return;
        }
        if (!InputValidator.isValidPositiveInt(quantity)) {
            out.println("ADD_BOOK_FAIL|Quantity must be a positive integer");
            return;
        }
        
        try {
            int yearInt = Integer.parseInt(year);
            int quantityInt = Integer.parseInt(quantity);
            
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO books(title, author, publisher, year, quantity) VALUES(?,?,?,?,?)");
                ps.setString(1, title);
                ps.setString(2, author);
                ps.setString(3, publisher);
                ps.setInt(4, yearInt);
                ps.setInt(5, quantityInt);
                ps.executeUpdate();
                
                out.println("ADD_BOOK_SUCCESS");
                System.out.println("[INFO] Book added successfully by " + session.getUsername() + ": \"" + title + "\" by " + author);
            }
        } catch (Exception e) {
            out.println("ADD_BOOK_FAIL|" + e.getMessage());
            System.err.println("[ERROR] Add book error by " + session.getUsername() + ": " + e.getMessage());
        }
    }

    private void handleDeleteBook(String[] parts) {
        // Session-based admin authorization  
        if (!isAdmin()) {
            out.println("DELETE_BOOK_FAIL|Access denied - Admin privileges required");
            System.err.println("[SECURITY] Unauthorized DELETE_BOOK attempt from: " + clientIdentifier);
            return;
        }
        
        if (parts.length < 2) {
            out.println("DELETE_BOOK_FAIL|Missing book ID parameter");
            return;
        }
        
        String bookIdStr = InputValidator.sanitizeInput(parts[1]);
        if (!InputValidator.isValidId(bookIdStr)) {
            out.println("DELETE_BOOK_FAIL|Invalid book ID format");
            return;
        }
        
        try {
            int bookId = Integer.parseInt(bookIdStr);
            
            try (Connection conn = getConnection()) {
                // Check if book has active borrows before deletion
                PreparedStatement checkBorrows = conn.prepareStatement(
                    "SELECT COUNT(*) FROM borrows WHERE book_id = ? AND return_date IS NULL");
                checkBorrows.setInt(1, bookId);
                ResultSet rs = checkBorrows.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    out.println("DELETE_BOOK_FAIL|Cannot delete book with active borrows");
                    return;
                }
                
                PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE id=?");
                ps.setInt(1, bookId);
                int affected = ps.executeUpdate();
                
                if (affected > 0) {
                    out.println("DELETE_BOOK_SUCCESS");
                    System.out.println("[INFO] Book deleted successfully by " + session.getUsername() + ": ID " + bookId);
                } else {
                    out.println("DELETE_BOOK_FAIL|Book not found");
                    System.err.println("[ERROR] Book not found for deletion by " + session.getUsername() + ": ID " + bookId);
                }
            }
        } catch (Exception e) {
            out.println("DELETE_BOOK_FAIL|" + e.getMessage());
            System.err.println("[ERROR] Delete book error by " + session.getUsername() + ": " + e.getMessage());
        }
    }

    private void handleListBorrows() {
        try (Connection conn = getConnection()) {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM borrows");
            StringBuilder sb = new StringBuilder("BORROW_LIST|");
            while (rs.next()) {
                sb.append(rs.getInt("id")).append(",")
                  .append(rs.getInt("user_id")).append(",")
                  .append(rs.getInt("book_id")).append(",")
                  .append(rs.getString("borrow_date")).append(",")
                  .append(rs.getString("return_date")).append(";");
            }
            out.println(sb.toString());
        } catch (Exception e) {
            out.println("LIST_BORROWS_FAIL|" + e.getMessage());
        }
    }

    private void handleFavorite(String[] parts) {

        if (parts.length < 3) { out.println("FAVORITE_FAIL|Missing params"); return; }
        int userId = Integer.parseInt(parts[1]);
        int bookId = Integer.parseInt(parts[2]);
        try (Connection conn = getConnection()) {

            try {
                conn.createStatement().executeQuery("SELECT id FROM activities LIMIT 1");
            } catch (Exception e) {
                conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS activities (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER," +
                    "book_id INTEGER," +
                    "action TEXT," +
                    "action_time TEXT," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)," +
                    "FOREIGN KEY(book_id) REFERENCES books(id))"
                );
            }

            try {
                conn.createStatement().executeQuery("SELECT favorite FROM books LIMIT 1");
            } catch (Exception e) {
                out.println("FAVORITE_FAIL|Cột favorite chưa tồn tại trong bảng books");
                return;
            }

            PreparedStatement check = conn.prepareStatement("SELECT id FROM books WHERE id=?");
            check.setInt(1, bookId);
            ResultSet rs = check.executeQuery();
            if (!rs.next()) {
                out.println("FAVORITE_FAIL|Book not found");
                return;
            }

            PreparedStatement ps = conn.prepareStatement("UPDATE books SET favorite=1 WHERE id=?");
            ps.setInt(1, bookId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                out.println("FAVORITE_FAIL|Update failed");
                return;
            }

            PreparedStatement act = conn.prepareStatement("INSERT INTO activities(user_id, book_id, action, action_time) VALUES(?,?,?,datetime('now'))");
            act.setInt(1, userId);
            act.setInt(2, bookId);
            act.setString(3, "favorite");
            act.executeUpdate();
            out.println("FAVORITE_SUCCESS");
        } catch (Exception e) {
            out.println("FAVORITE_FAIL|" + e.getMessage());
        }
    }

    private void handleListActivities(String[] parts) {

        if (parts.length < 2) { out.println("ACTIVITIES_FAIL|Missing params"); return; }
        int userId = Integer.parseInt(parts[1]);
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT a.id, a.action, a.action_time, b.title FROM activities a LEFT JOIN books b ON a.book_id=b.id WHERE a.user_id=? ORDER BY a.action_time DESC LIMIT 50");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                String id = rs.getString("id");
                String action = rs.getString("action");
                String time = rs.getString("action_time");
                String title = rs.getString("title");
                sb.append(id).append(" - ").append(title).append(" - ").append(action).append(" - ").append(time).append(";");
            }
            out.println("ACTIVITIES_LIST|" + sb.toString());
        } catch (Exception e) {
            out.println("ACTIVITIES_FAIL|" + e.getMessage());
        }
    }

    private void handleListFavorites(String[] parts) {

        if (parts.length < 2) { out.println("FAVORITES_FAIL|Missing params"); return; }
        int userId = Integer.parseInt(parts[1]);
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT b.id, b.title, b.author FROM books b INNER JOIN favorites f ON b.id = f.book_id WHERE f.user_id = ? ORDER BY f.added_date DESC");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                String id = rs.getString("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                sb.append(id).append(" - ").append(title).append(" - ").append(author).append(";");
            }
            out.println("FAVORITES_LIST|" + sb.toString());
        } catch (Exception e) {
            out.println("FAVORITES_FAIL|" + e.getMessage());
        }
    }

    private void handleListBorrowed(String[] parts) {

        if (parts.length < 2) { out.println("BORROWED_FAIL|Missing params"); return; }
        int userId = Integer.parseInt(parts[1]);
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT b.title, b.author, br.borrow_date, " +
                "COALESCE((SELECT rq.expected_return_date FROM borrow_requests rq " +
                "WHERE rq.user_id = br.user_id AND rq.book_id = br.book_id ORDER BY rq.request_date DESC LIMIT 1), datetime(br.borrow_date, '+30 days')) as due_date " +
                "FROM borrows br INNER JOIN books b ON br.book_id = b.id WHERE br.user_id = ? AND br.return_date IS NULL");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                String title = rs.getString("title");
                String author = rs.getString("author");
                String borrowDate = rs.getString("borrow_date");
                String dueDate = rs.getString("due_date");
                sb.append(title).append(",").append(author).append(",").append(borrowDate).append(",").append(dueDate).append(";");
            }
            out.println("BORROWED_LIST|" + sb.toString());
        } catch (Exception e) {
            out.println("BORROWED_FAIL|" + e.getMessage());
        }
    }
    
    private void handleLogout() {
        if (session != null && session.isAuthenticated()) {
            System.out.println("[INFO] User logout: " + session.getUsername() + " from: " + clientIdentifier);
            SessionManager.removeSession(session.getSessionId());
        }
        out.println("LOGOUT_SUCCESS");
    }
    
    private boolean isAdmin() {
        return session != null && session.isAuthenticated() && "admin".equals(session.getRole());
    }
    
    private void handleAdvancedSearch(String[] parts) {
        String query = parts.length > 1 ? parts[1] : "";
        String category = parts.length > 2 ? parts[2] : null;
        String author = parts.length > 3 ? parts[3] : null;
        String publisher = parts.length > 4 ? parts[4] : null;
        Integer yearFrom = parts.length > 5 && !parts[5].isEmpty() ? 
                          Integer.parseInt(parts[5]) : null;
        Integer yearTo = parts.length > 6 && !parts[6].isEmpty() ? 
                        Integer.parseInt(parts[6]) : null;
        boolean availableOnly = parts.length > 7 ? Boolean.parseBoolean(parts[7]) : false;
        int limit = parts.length > 8 ? Integer.parseInt(parts[8]) : 50;
        
        String result = AdvancedSearch.searchBooks(query, category, author, publisher, 
                                                  yearFrom, yearTo, availableOnly, limit);
        out.println(result);
    }
    
    private void handlePopularBooks(String[] parts) {
        int limit = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
        String result = AdvancedSearch.getPopularBooks(limit);
        out.println(result);
    }
    
    private void handleRecentBooks(String[] parts) {
        int limit = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
        String result = AdvancedSearch.getRecentBooks(limit);
        out.println(result);
    }
    
    private void handleRecommendations(String[] parts) {
        if (!session.isAuthenticated()) {
            out.println(ResponseFormatter.error("RECOMMENDATIONS", "Please login first"));
            return;
        }
        
        int limit = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
        // Get user ID from session - this would require storing it during login
        // For now, using a placeholder
        int userId = 1; // This should come from session
        String result = AdvancedSearch.getRecommendations(userId, limit);
        out.println(result);
    }
    
    private void handleHealthCheck() {
        String health = MetricsCollector.getHealthStatus();
        out.println(health);
    }
    
    private void handleMetrics() {
        String metrics = MetricsCollector.getMetricsReport();
        out.println(ResponseFormatter.success("METRICS", metrics));
    }
    
    private void handleBackup(String[] parts) {
        if (parts.length > 1 && "RESTORE".equals(parts[1].toUpperCase())) {
            // Restore from backup
            if (parts.length < 3) {
                out.println(ResponseFormatter.error("BACKUP", "Missing backup filename"));
                return;
            }
            boolean success = BackupManager.restoreFromBackup(parts[2]);
            if (success) {
                out.println(ResponseFormatter.success("BACKUP_RESTORE"));
            } else {
                out.println(ResponseFormatter.error("BACKUP_RESTORE", "Failed to restore from backup"));
            }
        } else if (parts.length > 1 && "LIST".equals(parts[1].toUpperCase())) {
            // List available backups
            String[] backups = BackupManager.listBackups();
            out.println(ResponseFormatter.success("BACKUP_LIST", ResponseFormatter.formatArray(backups)));
        } else {
            // Perform backup
            boolean success = BackupManager.performBackup();
            if (success) {
                out.println(ResponseFormatter.success("BACKUP"));
            } else {
                out.println(ResponseFormatter.error("BACKUP", "Failed to create backup"));
            }
        }
    }
}
