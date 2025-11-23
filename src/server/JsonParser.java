package server;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple JSON parser for REST API
 */
public class JsonParser {
    
    /**
     * Parse simple JSON object into Map
     */
    public static Map<String, String> parseSimple(String json) {
        Map<String, String> result = new HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        
        String[] pairs = json.split(",");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim().replace("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * Create simple JSON object from Map
     */
    public static String createSimple(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                json.append(",");
            }
            
            json.append("\"").append(escape(entry.getKey())).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escape((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value.toString());
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(escape(value.toString())).append("\"");
            }
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escape JSON string
     */
    private static String escape(String str) {
        if (str == null) return "";
        
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Create success response JSON
     */
    public static String success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return createSimple(response);
    }
    
    /**
     * Create error response JSON
     */
    public static String error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return createSimple(response);
    }
    
    /**
     * Parse JSON object into Map (enhanced version)
     */
    public static Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }
        
        try {
            json = json.substring(1, json.length() - 1).trim();
            if (json.isEmpty()) {
                return result;
            }
            
            int depth = 0;
            int start = 0;
            boolean inString = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = !inString;
                }
                
                if (!inString) {
                    if (c == '{' || c == '[') {
                        depth++;
                    } else if (c == '}' || c == ']') {
                        depth--;
                    } else if (c == ',' && depth == 0) {
                        String pair = json.substring(start, i).trim();
                        parsePair(pair, result);
                        start = i + 1;
                    }
                }
            }
            
            String lastPair = json.substring(start).trim();
            if (!lastPair.isEmpty()) {
                parsePair(lastPair, result);
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse a key-value pair
     */
    private static void parsePair(String pair, Map<String, Object> result) {
        int colonIndex = pair.indexOf(':');
        if (colonIndex > 0) {
            String key = pair.substring(0, colonIndex).trim();
            String value = pair.substring(colonIndex + 1).trim();
            
            // Remove quotes from key
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length() - 1);
            }
            
            // Parse value
            Object parsedValue = parseValue(value);
            result.put(key, parsedValue);
        }
    }
    
    /**
     * Parse a JSON value
     */
    private static Object parseValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        value = value.trim();
        
        if (value.equals("null")) {
            return null;
        } else if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        } else if (value.contains(".")) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return value;
            }
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return value;
            }
        }
    }
    
    /**
     * Convert object to JSON string
     */
    @SuppressWarnings("unchecked")
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof Map) {
            return createSimple((Map<String, Object>) obj);
        } else if (obj instanceof String) {
            return "\"" + escape((String) obj) + "\"";
        } else if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        } else {
            return "\"" + escape(obj.toString()) + "\"";
        }
    }
    
    /**
     * Create error response JSON (alias for error method)
     */
    public static String createErrorResponse(String message) {
        return error(message);
    }
}