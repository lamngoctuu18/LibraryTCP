package server;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for creating standardized JSON-like responses
 */
public class ResponseFormatter {
    
    /**
     * Create success response with data
     */
    public static String success(String action, Object data) {
        return formatResponse("SUCCESS", action, null, data);
    }
    
    /**
     * Create success response without data
     */
    public static String success(String action) {
        return formatResponse("SUCCESS", action, null, null);
    }
    
    /**
     * Create error response
     */
    public static String error(String action, String message) {
        return formatResponse("ERROR", action, message, null);
    }
    
    /**
     * Create error response with details
     */
    public static String error(String action, String message, Object details) {
        return formatResponse("ERROR", action, message, details);
    }
    
    /**
     * Create fail response (for business logic failures)
     */
    public static String fail(String action, String reason) {
        return formatResponse("FAIL", action, reason, null);
    }
    
    /**
     * Format response in structured format
     */
    private static String formatResponse(String status, String action, String message, Object data) {
        StringBuilder response = new StringBuilder();
        response.append(status).append("|").append(action);
        
        if (message != null && !message.trim().isEmpty()) {
            response.append("|").append(message);
        }
        
        if (data != null) {
            response.append("|").append(data.toString());
        }
        
        return response.toString();
    }
    
    /**
     * Format list data for transmission
     */
    public static String formatList(String[] items) {
        if (items == null || items.length == 0) return "";
        return String.join(";", items);
    }
    
    /**
     * Format object data as key-value pairs
     */
    public static String formatObject(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) sb.append(",");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }
    
    /**
     * Create standardized book data format
     */
    public static String formatBook(int id, String title, String author, String publisher, int year, int quantity) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("id", id);
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("publisher", publisher);
        bookData.put("year", year);
        bookData.put("quantity", quantity);
        return formatObject(bookData);
    }
    
    /**
     * Create standardized user data format  
     */
    public static String formatUser(int id, String username, String role, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", id);
        userData.put("username", username);
        userData.put("role", role);
        userData.put("email", email);
        userData.put("phone", phone);
        return formatObject(userData);
    }
    
    /**
     * Create standardized borrow data format
     */
    public static String formatBorrow(int userId, int bookId, String borrowDate, String dueDate, String status) {
        Map<String, Object> borrowData = new HashMap<>();
        borrowData.put("userId", userId);
        borrowData.put("bookId", bookId);
        borrowData.put("borrowDate", borrowDate);
        borrowData.put("dueDate", dueDate);
        borrowData.put("status", status);
        return formatObject(borrowData);
    }
}