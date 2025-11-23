package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced search functionality with indexing and full-text capabilities
 */
public class AdvancedSearch {
    
    /**
     * Perform comprehensive book search with multiple criteria
     */
    public static String searchBooks(String query, String category, String author, String publisher, 
                                   Integer yearFrom, Integer yearTo, boolean availableOnly, int limit) {
        try (Connection conn = DatabasePool.getInstance().getConnection()) {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            sql.append("SELECT DISTINCT b.id, b.title, b.author, b.publisher, b.year, b.quantity, ");
            sql.append("(b.quantity - COALESCE(borrowed.count, 0)) as available ");
            sql.append("FROM books b ");
            sql.append("LEFT JOIN (SELECT book_id, COUNT(*) as count FROM borrows WHERE return_date IS NULL GROUP BY book_id) borrowed ");
            sql.append("ON b.id = borrowed.book_id ");
            sql.append("WHERE 1=1 ");
            
            // Full-text search across multiple fields
            if (query != null && !query.trim().isEmpty()) {
                sql.append("AND (b.title LIKE ? OR b.author LIKE ? OR b.publisher LIKE ? OR b.description LIKE ?) ");
                String searchTerm = "%" + query.trim() + "%";
                params.add(searchTerm);
                params.add(searchTerm);
                params.add(searchTerm);
                params.add(searchTerm);
            }
            
            // Category filter (assuming categories table exists)
            if (category != null && !category.trim().isEmpty()) {
                sql.append("AND EXISTS (SELECT 1 FROM book_categories bc JOIN categories c ON bc.category_id = c.id ");
                sql.append("WHERE bc.book_id = b.id AND c.name LIKE ?) ");
                params.add("%" + category.trim() + "%");
            }
            
            // Author filter
            if (author != null && !author.trim().isEmpty()) {
                sql.append("AND b.author LIKE ? ");
                params.add("%" + author.trim() + "%");
            }
            
            // Publisher filter
            if (publisher != null && !publisher.trim().isEmpty()) {
                sql.append("AND b.publisher LIKE ? ");
                params.add("%" + publisher.trim() + "%");
            }
            
            // Year range filter
            if (yearFrom != null) {
                sql.append("AND b.year >= ? ");
                params.add(yearFrom);
            }
            if (yearTo != null) {
                sql.append("AND b.year <= ? ");
                params.add(yearTo);
            }
            
            // Available books only
            if (availableOnly) {
                sql.append("AND (b.quantity - COALESCE(borrowed.count, 0)) > 0 ");
            }
            
            // Ordering and limit
            sql.append("ORDER BY ");
            if (query != null && !query.trim().isEmpty()) {
                // Relevance scoring for text search
                sql.append("CASE ");
                sql.append("WHEN b.title LIKE ? THEN 1 ");
                sql.append("WHEN b.author LIKE ? THEN 2 ");
                sql.append("WHEN b.publisher LIKE ? THEN 3 ");
                sql.append("ELSE 4 END, ");
                String exactTerm = "%" + query.trim() + "%";
                params.add(exactTerm);
                params.add(exactTerm);
                params.add(exactTerm);
            }
            sql.append("b.title ASC ");
            
            if (limit > 0) {
                sql.append("LIMIT ? ");
                params.add(limit);
            }
            
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = ps.executeQuery();
            List<String> books = new ArrayList<>();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String bookAuthor = rs.getString("author");
                String bookPublisher = rs.getString("publisher");
                int year = rs.getInt("year");
                int quantity = rs.getInt("quantity");
                int available = rs.getInt("available");
                
                books.add(ResponseFormatter.formatBook(id, title, bookAuthor, bookPublisher, year, available));
            }
            
            MetricsCollector.recordDatabaseOperation(true);
            return ResponseFormatter.success("SEARCH", ResponseFormatter.formatList(books.toArray(new String[0])));
            
        } catch (Exception e) {
            System.err.println("[ERROR] Advanced search failed: " + e.getMessage());
            MetricsCollector.recordDatabaseOperation(false);
            MetricsCollector.recordError("SEARCH_ERROR");
            return ResponseFormatter.error("SEARCH", e.getMessage());
        }
    }
    
    /**
     * Search users (admin only)
     */
    public static String searchUsers(String query, String role, boolean activeOnly, int limit) {
        try (Connection conn = DatabasePool.getInstance().getConnection()) {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            sql.append("SELECT u.id, u.username, u.role, u.email, u.phone, u.status ");
            sql.append("FROM users u WHERE 1=1 ");
            
            if (query != null && !query.trim().isEmpty()) {
                sql.append("AND (u.username LIKE ? OR u.email LIKE ?) ");
                String searchTerm = "%" + query.trim() + "%";
                params.add(searchTerm);
                params.add(searchTerm);
            }
            
            if (role != null && !role.trim().isEmpty()) {
                sql.append("AND u.role = ? ");
                params.add(role.trim());
            }
            
            if (activeOnly) {
                sql.append("AND u.status != 'locked' ");
            }
            
            sql.append("ORDER BY u.username ASC ");
            
            if (limit > 0) {
                sql.append("LIMIT ? ");
                params.add(limit);
            }
            
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = ps.executeQuery();
            List<String> users = new ArrayList<>();
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String userRole = rs.getString("role");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                
                users.add(ResponseFormatter.formatUser(id, username, userRole, email, phone));
            }
            
            MetricsCollector.recordDatabaseOperation(true);
            return ResponseFormatter.success("USER_SEARCH", ResponseFormatter.formatList(users.toArray(new String[0])));
            
        } catch (Exception e) {
            System.err.println("[ERROR] User search failed: " + e.getMessage());
            MetricsCollector.recordDatabaseOperation(false);
            MetricsCollector.recordError("USER_SEARCH_ERROR");
            return ResponseFormatter.error("USER_SEARCH", e.getMessage());
        }
    }
    
    /**
     * Get popular books based on borrow history
     */
    public static String getPopularBooks(int limit) {
        try (Connection conn = DatabasePool.getInstance().getConnection()) {
            String sql = "SELECT b.id, b.title, b.author, b.publisher, b.year, b.quantity, " +
                        "COUNT(br.book_id) as borrow_count " +
                        "FROM books b " +
                        "LEFT JOIN borrows br ON b.id = br.book_id " +
                        "GROUP BY b.id, b.title, b.author, b.publisher, b.year, b.quantity " +
                        "ORDER BY borrow_count DESC, b.title ASC " +
                        "LIMIT ?";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            List<String> books = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String publisher = rs.getString("publisher");
                int year = rs.getInt("year");
                int quantity = rs.getInt("quantity");
                
                books.add(ResponseFormatter.formatBook(id, title, author, publisher, year, quantity));
            }
            
            MetricsCollector.recordDatabaseOperation(true);
            return ResponseFormatter.success("POPULAR_BOOKS", ResponseFormatter.formatList(books.toArray(new String[0])));
            
        } catch (Exception e) {
            System.err.println("[ERROR] Popular books search failed: " + e.getMessage());
            MetricsCollector.recordDatabaseOperation(false);
            return ResponseFormatter.error("POPULAR_BOOKS", e.getMessage());
        }
    }
    
    /**
     * Get recently added books
     */
    public static String getRecentBooks(int limit) {
        try (Connection conn = DatabasePool.getInstance().getConnection()) {
            String sql = "SELECT id, title, author, publisher, year, quantity " +
                        "FROM books " +
                        "ORDER BY id DESC " + // Assuming higher ID means more recent
                        "LIMIT ?";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            List<String> books = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String publisher = rs.getString("publisher");
                int year = rs.getInt("year");
                int quantity = rs.getInt("quantity");
                
                books.add(ResponseFormatter.formatBook(id, title, author, publisher, year, quantity));
            }
            
            MetricsCollector.recordDatabaseOperation(true);
            return ResponseFormatter.success("RECENT_BOOKS", ResponseFormatter.formatList(books.toArray(new String[0])));
            
        } catch (Exception e) {
            System.err.println("[ERROR] Recent books search failed: " + e.getMessage());
            MetricsCollector.recordDatabaseOperation(false);
            return ResponseFormatter.error("RECENT_BOOKS", e.getMessage());
        }
    }
    
    /**
     * Get recommendations based on user's borrow history
     */
    public static String getRecommendations(int userId, int limit) {
        try (Connection conn = DatabasePool.getInstance().getConnection()) {
            // Simple recommendation: books by authors the user has borrowed before
            String sql = "SELECT DISTINCT b.id, b.title, b.author, b.publisher, b.year, b.quantity " +
                        "FROM books b " +
                        "WHERE b.author IN (" +
                        "  SELECT DISTINCT b2.author FROM borrows br " +
                        "  JOIN books b2 ON br.book_id = b2.id " +
                        "  WHERE br.user_id = ?" +
                        ") " +
                        "AND b.id NOT IN (" +
                        "  SELECT book_id FROM borrows WHERE user_id = ?" +
                        ") " +
                        "ORDER BY b.title ASC " +
                        "LIMIT ?";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            
            List<String> books = new ArrayList<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String publisher = rs.getString("publisher");
                int year = rs.getInt("year");
                int quantity = rs.getInt("quantity");
                
                books.add(ResponseFormatter.formatBook(id, title, author, publisher, year, quantity));
            }
            
            MetricsCollector.recordDatabaseOperation(true);
            return ResponseFormatter.success("RECOMMENDATIONS", ResponseFormatter.formatList(books.toArray(new String[0])));
            
        } catch (Exception e) {
            System.err.println("[ERROR] Recommendations search failed: " + e.getMessage());
            MetricsCollector.recordDatabaseOperation(false);
            return ResponseFormatter.error("RECOMMENDATIONS", e.getMessage());
        }
    }
}