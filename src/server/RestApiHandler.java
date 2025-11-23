package server;

import model.Book;
import model.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST API handler for modern web/mobile client integration
 * Integrated with I18n support and AI recommendations
 */
public class RestApiHandler {
    private final EnhancedBookDAO bookDAO;
    private final EnhancedBorrowDAO borrowDAO;
    private final EnhancedUserDAO userDAO;
    private final SessionManager sessionManager;
    private final RateLimiter rateLimiter;
    private final RecommendationEngine recommendationEngine;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public RestApiHandler(int port, EnhancedBookDAO bookDAO, EnhancedBorrowDAO borrowDAO, EnhancedUserDAO userDAO,
                         SessionManager sessionManager, RateLimiter rateLimiter) {
        this.bookDAO = bookDAO;
        this.borrowDAO = borrowDAO;
        this.userDAO = userDAO;
        this.sessionManager = sessionManager;
        this.rateLimiter = rateLimiter;
        this.recommendationEngine = new RecommendationEngine(bookDAO, borrowDAO, userDAO);
        
        try {
            this.serverSocket = new ServerSocket(port);
            this.threadPool = Executors.newCachedThreadPool();
            System.out.println("[REST API] Server started on port " + port);
        } catch (IOException e) {
            System.err.println("[REST API] Error starting server: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Start the REST API server
     */
    public void start() {
        running = true;
        threadPool.submit(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(new HttpRequestHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[REST API] Error accepting connection: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * Stop the REST API server
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            System.out.println("[REST API] Server stopped");
        } catch (IOException e) {
            System.err.println("[REST API] Error stopping server: " + e.getMessage());
        }
    }
    
    /**
     * Inner class to handle individual HTTP requests
     */
    private class HttpRequestHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientIp;
        
        public HttpRequestHandler(Socket socket) {
            this.socket = socket;
            this.clientIp = socket.getRemoteSocketAddress().toString();
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                
                // Check rate limiting
                if (!rateLimiter.isAllowed(clientIp)) {
                    sendResponse(429, "application/json", 
                        JsonParser.createErrorResponse(I18nManager.getMessage("rate.limit.exceeded")));
                    return;
                }
                
                // Parse HTTP request
                HttpRequest request = parseHttpRequest();
                if (request == null) {
                    sendResponse(400, "application/json", 
                        JsonParser.createErrorResponse("Bad request"));
                    return;
                }
                
                // Detect language from Accept-Language header
                String language = I18nManager.detectLanguage(request.headers.get("Accept-Language"));
                I18nManager.setLanguage(language);
                
                // Handle CORS preflight
                if ("OPTIONS".equals(request.method)) {
                    sendCorsResponse();
                    return;
                }
                
                // Route request
                handleRequest(request);
                
            } catch (Exception e) {
                System.err.println("[REST API] Error handling request: " + e.getMessage());
                try {
                    sendResponse(500, "application/json", 
                        JsonParser.createErrorResponse(I18nManager.getMessage("error.general")));
                } catch (Exception ex) {
                    // Ignore
                }
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        /**
         * Parse HTTP request
         */
        private HttpRequest parseHttpRequest() throws IOException {
            String requestLine = in.readLine();
            if (requestLine == null) return null;
            
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) return null;
            
            HttpRequest request = new HttpRequest();
            request.method = parts[0];
            request.path = parts[1];
            request.protocol = parts[2];
            
            // Parse headers
            String line;
            while ((line = in.readLine()) != null && !line.trim().isEmpty()) {
                int colonIndex = line.indexOf(":");
                if (colonIndex > 0) {
                    String headerName = line.substring(0, colonIndex).trim();
                    String headerValue = line.substring(colonIndex + 1).trim();
                    request.headers.put(headerName, headerValue);
                }
            }
            
            // Parse body for POST/PUT requests
            if ("POST".equals(request.method) || "PUT".equals(request.method)) {
                String contentLengthStr = request.headers.get("Content-Length");
                if (contentLengthStr != null) {
                    try {
                        int contentLength = Integer.parseInt(contentLengthStr);
                        char[] buffer = new char[contentLength];
                        in.read(buffer);
                        request.body = new String(buffer);
                    } catch (NumberFormatException e) {
                        // Invalid content length
                    }
                }
            }
            
            return request;
        }
        
        /**
         * Handle HTTP request routing
         */
        private void handleRequest(HttpRequest request) throws IOException {
            String path = request.path;
            String method = request.method;
            
            // API routes
            if (path.startsWith("/api/")) {
                // Authentication endpoints
                if (path.equals("/api/login") && "POST".equals(method)) {
                    handleLogin(request);
                } else if (path.equals("/api/register") && "POST".equals(method)) {
                    handleRegister(request);
                } else if (path.equals("/api/logout") && "POST".equals(method)) {
                    handleLogout(request);
                }
                // Book endpoints
                else if (path.equals("/api/books") && "GET".equals(method)) {
                    handleGetBooks(request);
                } else if (path.equals("/api/books") && "POST".equals(method)) {
                    handleAddBook(request);
                } else if (path.matches("/api/books/\\d+") && "GET".equals(method)) {
                    int bookId = Integer.parseInt(path.substring("/api/books/".length()));
                    handleGetBook(request, bookId);
                } else if (path.matches("/api/books/\\d+") && "PUT".equals(method)) {
                    int bookId = Integer.parseInt(path.substring("/api/books/".length()));
                    handleUpdateBook(request, bookId);
                } else if (path.matches("/api/books/\\d+") && "DELETE".equals(method)) {
                    int bookId = Integer.parseInt(path.substring("/api/books/".length()));
                    handleDeleteBook(request, bookId);
                }
                // Search endpoint
                else if (path.startsWith("/api/search") && "GET".equals(method)) {
                    handleSearch(request);
                }
                // Recommendation endpoints
                else if (path.equals("/api/recommendations") && "GET".equals(method)) {
                    handleGetRecommendations(request);
                } else if (path.matches("/api/books/\\d+/similar") && "GET".equals(method)) {
                    int bookId = Integer.parseInt(path.substring("/api/books/".length(), path.indexOf("/similar")));
                    handleGetSimilarBooks(request, bookId);
                }
                // Borrow endpoints
                else if (path.equals("/api/borrows") && "GET".equals(method)) {
                    handleGetBorrows(request);
                } else if (path.equals("/api/borrows") && "POST".equals(method)) {
                    handleBorrowBook(request);
                } else if (path.matches("/api/borrows/\\d+/return") && "POST".equals(method)) {
                    int borrowId = Integer.parseInt(path.substring("/api/borrows/".length(), path.indexOf("/return")));
                    handleReturnBook(request, borrowId);
                }
                // User endpoints
                else if (path.equals("/api/users") && "GET".equals(method)) {
                    handleGetUsers(request);
                } else if (path.matches("/api/users/\\d+") && "GET".equals(method)) {
                    int userId = Integer.parseInt(path.substring("/api/users/".length()));
                    handleGetUser(request, userId);
                }
                // Language endpoint
                else if (path.equals("/api/language") && "POST".equals(method)) {
                    handleSetLanguage(request);
                } else {
                    sendResponse(404, "application/json", 
                        JsonParser.createErrorResponse("Endpoint not found"));
                }
            } else {
                // Serve static files or documentation
                sendResponse(200, "text/html", getApiDocumentation());
            }
        }
        
        /**
         * Handle user login
         */
        private void handleLogin(HttpRequest request) throws IOException {
            Map<String, Object> data = JsonParser.parseJson(request.body);
            String username = (String) data.get("username");
            String password = (String) data.get("password");
            
            if (username == null || password == null) {
                sendResponse(400, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("login.failed")));
                return;
            }
            
            try {
                User user = userDAO.getUserByUsername(username);
                if (user != null && PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
                    String token = SessionManager.createSession(clientIp, user.getId()).getToken();
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", I18nManager.getMessage("login.success"));
                    response.put("token", token);
                    response.put("user", userToJson(user));
                    
                    sendResponse(200, "application/json", JsonParser.toJson(response));
                } else {
                    sendResponse(401, "application/json", 
                        JsonParser.createErrorResponse(I18nManager.getMessage("login.failed")));
                }
            } catch (Exception e) {
                sendResponse(500, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("error.general")));
            }
        }
        
        /**
         * Handle user registration
         */
        private void handleRegister(HttpRequest request) throws IOException {
            Map<String, Object> data = JsonParser.parseJson(request.body);
            String username = (String) data.get("username");
            String password = (String) data.get("password");
            String email = (String) data.get("email");
            String fullName = (String) data.get("fullName");
            
            if (username == null || password == null) {
                sendResponse(400, "application/json", 
                    JsonParser.createErrorResponse("Username and password are required"));
                return;
            }
            
            try {
                User existingUser = userDAO.getUserByUsername(username);
                if (existingUser != null) {
                    sendResponse(400, "application/json", 
                        JsonParser.createErrorResponse("Username already exists"));
                    return;
                }
                
                String hashedPassword = PasswordHasher.hashPassword(password);
                User newUser = new User(0, username, hashedPassword, email, fullName, false, new Date());
                
                if (userDAO.addUser(newUser)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", I18nManager.getMessage("register.success"));
                    
                    sendResponse(201, "application/json", JsonParser.toJson(response));
                } else {
                    sendResponse(500, "application/json", 
                        JsonParser.createErrorResponse(I18nManager.getMessage("register.failed")));
                }
            } catch (Exception e) {
                sendResponse(500, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("error.general")));
            }
        }
        
        /**
         * Handle user logout
         */
        private void handleLogout(HttpRequest request) throws IOException {
            String token = request.headers.get("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring("Bearer ".length());
                SessionManager.invalidateSession(token);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logged out successfully");
            
            sendResponse(200, "application/json", JsonParser.toJson(response));
        }
        
        /**
         * Handle get books
         */
        private void handleGetBooks(HttpRequest request) throws IOException {
            try {
                List<Book> books = bookDAO.getAllBooks();
                List<Map<String, Object>> bookList = new ArrayList<>();
                
                for (Book book : books) {
                    bookList.add(bookToJson(book));
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("books", bookList);
                response.put("total", books.size());
                
                sendResponse(200, "application/json", JsonParser.toJson(response));
            } catch (Exception e) {
                sendResponse(500, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("error.general")));
            }
        }
        
        /**
         * Handle get recommendations
         */
        private void handleGetRecommendations(HttpRequest request) throws IOException {
            String token = request.headers.get("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                sendResponse(401, "application/json", 
                    JsonParser.createErrorResponse("Authentication required"));
                return;
            }
            
            token = token.substring("Bearer ".length());
            SessionManager.ClientSession session = SessionManager.getSession(token);
            if (session == null) {
                sendResponse(401, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("session.expired")));
                return;
            }
            
            try {
                String countParam = request.path.contains("count=") ? 
                    request.path.substring(request.path.indexOf("count=") + 6) : "10";
                int count = Integer.parseInt(countParam.split("&")[0]);
                
                List<Book> recommendations = recommendationEngine.getRecommendations(session.getUserId(), count);
                List<Map<String, Object>> bookList = new ArrayList<>();
                
                for (Book book : recommendations) {
                    bookList.add(bookToJson(book));
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("recommendations", bookList);
                response.put("total", recommendations.size());
                
                sendResponse(200, "application/json", JsonParser.toJson(response));
            } catch (Exception e) {
                sendResponse(500, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("error.general")));
            }
        }
        
        /**
         * Handle get similar books
         */
        private void handleGetSimilarBooks(HttpRequest request, int bookId) throws IOException {
            try {
                int count = 10; // Default count
                List<Book> similarBooks = recommendationEngine.getSimilarBooks(bookId, count);
                List<Map<String, Object>> bookList = new ArrayList<>();
                
                for (Book book : similarBooks) {
                    bookList.add(bookToJson(book));
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("similar_books", bookList);
                response.put("total", similarBooks.size());
                
                sendResponse(200, "application/json", JsonParser.toJson(response));
            } catch (Exception e) {
                sendResponse(500, "application/json", 
                    JsonParser.createErrorResponse(I18nManager.getMessage("error.general")));
            }
        }
        
        /**
         * Handle set language
         */
        private void handleSetLanguage(HttpRequest request) throws IOException {
            Map<String, Object> data = JsonParser.parseJson(request.body);
            String language = (String) data.get("language");
            
            if (language != null) {
                I18nManager.setLanguage(language);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Language set to " + language);
                response.put("language", language);
                response.put("available_languages", I18nManager.getAvailableLanguages());
                
                sendResponse(200, "application/json", JsonParser.toJson(response));
            } else {
                sendResponse(400, "application/json", 
                    JsonParser.createErrorResponse("Language parameter required"));
            }
        }
        
        /**
         * Send HTTP response
         */
        private void sendResponse(int statusCode, String contentType, String body) throws IOException {
            out.println("HTTP/1.1 " + statusCode + " " + getStatusMessage(statusCode));
            out.println("Content-Type: " + contentType + "; charset=UTF-8");
            out.println("Content-Length: " + body.getBytes(StandardCharsets.UTF_8).length);
            out.println("Access-Control-Allow-Origin: *");
            out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
            out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
            out.println();
            out.print(body);
            out.flush();
        }
        
        /**
         * Send CORS preflight response
         */
        private void sendCorsResponse() throws IOException {
            out.println("HTTP/1.1 200 OK");
            out.println("Access-Control-Allow-Origin: *");
            out.println("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS");
            out.println("Access-Control-Allow-Headers: Content-Type, Authorization");
            out.println("Content-Length: 0");
            out.println();
            out.flush();
        }
        
        // Stub implementations for remaining endpoints
        private void handleAddBook(HttpRequest request) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleGetBook(HttpRequest request, int bookId) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleUpdateBook(HttpRequest request, int bookId) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleDeleteBook(HttpRequest request, int bookId) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleSearch(HttpRequest request) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleGetBorrows(HttpRequest request) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleBorrowBook(HttpRequest request) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleReturnBook(HttpRequest request, int borrowId) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleGetUsers(HttpRequest request) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        private void handleGetUser(HttpRequest request, int userId) throws IOException {
            sendResponse(501, "application/json", 
                JsonParser.createErrorResponse("Not implemented yet"));
        }
        
        // Utility methods
        private String getStatusMessage(int code) {
            switch (code) {
                case 200: return "OK";
                case 201: return "Created";
                case 400: return "Bad Request";
                case 401: return "Unauthorized";
                case 404: return "Not Found";
                case 429: return "Too Many Requests";
                case 500: return "Internal Server Error";
                case 501: return "Not Implemented";
                default: return "Unknown";
            }
        }
        
        private Map<String, Object> bookToJson(Book book) {
            Map<String, Object> json = new HashMap<>();
            json.put("id", book.getId());
            json.put("title", book.getTitle());
            json.put("author", book.getAuthor());
            json.put("category", book.getCategory());
            json.put("description", book.getDescription());
            json.put("available", book.isAvailable());
            return json;
        }
        
        private Map<String, Object> userToJson(User user) {
            Map<String, Object> json = new HashMap<>();
            json.put("id", user.getId());
            json.put("username", user.getUsername());
            json.put("email", user.getEmail());
            json.put("fullName", user.getFullName());
            json.put("isAdmin", user.isAdmin());
            return json;
        }
        
        private String getApiDocumentation() {
            return "<!DOCTYPE html><html><head><title>Library API Documentation</title></head>" +
                   "<body><h1>Library Management REST API</h1>" +
                   "<p>Welcome to the Library Management System REST API with AI Recommendations and Multi-language Support</p>" +
                   "<h2>Endpoints:</h2>" +
                   "<ul>" +
                   "<li>POST /api/login - User login</li>" +
                   "<li>POST /api/register - User registration</li>" +
                   "<li>GET /api/books - Get all books</li>" +
                   "<li>GET /api/recommendations - Get personalized AI recommendations</li>" +
                   "<li>GET /api/books/{id}/similar - Get similar books</li>" +
                   "<li>POST /api/language - Set interface language (en/vi/zh/ja/ko)</li>" +
                   "</ul>" +
                   "<h2>Languages Supported:</h2>" +
                   "<p>English, Vietnamese (Tiếng Việt), Chinese (中文), Japanese (日本語), Korean (한국어)</p>" +
                   "</body></html>";
        }
    }
    
    /**
     * HTTP request data structure
     */
    private static class HttpRequest {
        String method;
        String path;
        String protocol;
        Map<String, String> headers = new HashMap<>();
        String body;
    }
}