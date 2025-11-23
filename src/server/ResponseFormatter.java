package server;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Utility for creating standardized JSON responses
 */
public class ResponseFormatter {
    
    /**
     * Create success response with data
     */
    public static String success(String action, Object data) {
        return createJsonResponse("success", action, null, data);
    }
    
    /**
     * Create success response without data
     */
    public static String success(String action) {
        return createJsonResponse("success", action, null, null);
    }
    
    /**
     * Create error response
     */
    public static String error(String action, String message) {
        return createJsonResponse("error", action, message, null);
    }
    
    /**
     * Create error response with details
     */
    public static String error(String action, String message, Object details) {
        return createJsonResponse("error", action, message, details);
    }
    
    /**
     * Create fail response (for business logic failures)
     */
    public static String fail(String action, String reason) {
        return createJsonResponse("fail", action, reason, null);
    }
    
    /**
     * Create JSON response
     */
    private static String createJsonResponse(String status, String action, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("action", action);
        response.put("timestamp", System.currentTimeMillis());
        
        if (message != null && !message.trim().isEmpty()) {
            response.put("message", message);
        }
        
        if (data != null) {
            response.put("data", data);
        }
        
        return JsonParser.toJson(response);
    }
    
    /**
     * Format list data as JSON array
     */
    public static String formatList(List<?> items) {
        if (items == null || items.isEmpty()) return "[]";
        return JsonParser.toJson(items);
    }
    
    /**
     * Format array data as JSON array
     */
    public static String formatArray(Object[] items) {
        if (items == null || items.length == 0) return "[]";
        List<Object> list = new ArrayList<>();
        for (Object item : items) {
            list.add(item);
        }
        return JsonParser.toJson(list);
    }
    
    /**
     * Format object data as JSON
     */
    public static String formatObject(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return "{}";
        return JsonParser.toJson(data);
    }
    
    /**
     * Create standardized book data as JSON
     */
    public static String formatBook(int id, String title, String author, String publisher, int year, int quantity) {
        Map<String, Object> bookData = new HashMap<>();
        bookData.put("id", id);
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("publisher", publisher);
        bookData.put("year", year);
        bookData.put("quantity", quantity);
        return JsonParser.toJson(bookData);
    }
    
    /**
     * Create standardized user data as JSON  
     */
    public static String formatUser(int id, String username, String role, String email, String phone) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", id);
        userData.put("username", username);
        userData.put("role", role);
        userData.put("email", email);
        userData.put("phone", phone);
        return JsonParser.toJson(userData);
    }
    
    /**
     * Create standardized borrow data as JSON
     */
    public static String formatBorrow(int userId, int bookId, String borrowDate, String dueDate, String status) {
        Map<String, Object> borrowData = new HashMap<>();
        borrowData.put("userId", userId);
        borrowData.put("bookId", bookId);
        borrowData.put("borrowDate", borrowDate);
        borrowData.put("dueDate", dueDate);
        borrowData.put("status", status);
        return JsonParser.toJson(borrowData);
    }
    
    /**
     * Parse JSON response from server
     */
    public static Map<String, Object> parseResponse(String jsonResponse) {
        try {
            return JsonParser.parseJson(jsonResponse);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("status", "error");
            errorMap.put("message", "Failed to parse response");
            return errorMap;
        }
    }
}