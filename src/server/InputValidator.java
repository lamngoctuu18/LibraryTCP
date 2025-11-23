package server;

import java.util.regex.Pattern;

/**
 * Input validation and sanitization utility
 */
public class InputValidator {
    
    // Regex patterns for validation
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{10,15}$");
    private static final Pattern BOOK_TITLE_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}]{1,200}$");
    private static final Pattern AUTHOR_PATTERN = Pattern.compile("^[\\p{L}\\p{Z}\\p{P}]{1,100}$");
    
    // SQL injection prevention
    private static final String[] SQL_KEYWORDS = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", 
        "UNION", "WHERE", "OR", "AND", "EXEC", "EXECUTE"
    };
    
    /**
     * Validate username format
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Validate book title
     */
    public static boolean isValidBookTitle(String title) {
        return title != null && BOOK_TITLE_PATTERN.matcher(title).matches();
    }
    
    /**
     * Validate author name
     */
    public static boolean isValidAuthor(String author) {
        return author != null && AUTHOR_PATTERN.matcher(author).matches();
    }
    
    /**
     * Validate password strength
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;
        
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        
        return hasUpper && hasLower && hasDigit;
    }
    
    /**
     * Validate positive integer
     */
    public static boolean isValidPositiveInt(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            int num = Integer.parseInt(value.trim());
            return num > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate year (1000-current year + 5)
     */
    public static boolean isValidYear(String year) {
        if (!isValidPositiveInt(year)) return false;
        int y = Integer.parseInt(year);
        int currentYear = java.time.Year.now().getValue();
        return y >= 1000 && y <= currentYear + 5;
    }
    
    /**
     * Sanitize string input to prevent SQL injection
     */
    public static String sanitizeInput(String input) {
        if (input == null) return null;
        
        // Remove potential SQL injection patterns
        String cleaned = input.trim();
        String upperCleaned = cleaned.toUpperCase();
        
        for (String keyword : SQL_KEYWORDS) {
            if (upperCleaned.contains(keyword)) {
                // If it contains SQL keywords, escape them or reject
                cleaned = cleaned.replace(keyword.toLowerCase(), "");
                cleaned = cleaned.replace(keyword.toUpperCase(), "");
            }
        }
        
        // Remove potentially dangerous characters
        cleaned = cleaned.replaceAll("[';\\-\\-]", "");
        
        return cleaned.trim();
    }
    
    /**
     * Validate and sanitize search keyword
     */
    public static String validateSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return "";
        
        String cleaned = sanitizeInput(keyword);
        
        // Limit search keyword length
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }
        
        return cleaned;
    }
    
    /**
     * Validate numeric ID
     */
    public static boolean isValidId(String id) {
        return isValidPositiveInt(id);
    }
    
    /**
     * Generate validation error message
     */
    public static String getValidationError(String field, String value) {
        switch (field.toLowerCase()) {
            case "username":
                if (!isValidUsername(value)) 
                    return "Username must be 3-30 characters, alphanumeric and underscore only";
                break;
            case "email":
                if (!isValidEmail(value))
                    return "Invalid email format";
                break;
            case "phone":
                if (!isValidPhone(value))
                    return "Invalid phone number format";
                break;
            case "password":
                if (!isValidPassword(value))
                    return "Password must be at least 6 characters with uppercase, lowercase and digit";
                break;
            case "title":
                if (!isValidBookTitle(value))
                    return "Book title must be 1-200 characters";
                break;
            case "author":
                if (!isValidAuthor(value))
                    return "Author name must be 1-100 characters";
                break;
            case "year":
                if (!isValidYear(value))
                    return "Invalid year format";
                break;
        }
        return "Invalid " + field;
    }
}