package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Session management for tracking client sessions and timeouts
 */
public class SessionManager {
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;
    
    private static final ConcurrentHashMap<String, ClientSession> sessions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService cleanupService = Executors.newSingleThreadScheduledExecutor();
    
    static {
        // Start cleanup task
        cleanupService.scheduleAtFixedRate(() -> cleanupExpiredSessions(), 
                                         CLEANUP_INTERVAL_MINUTES, 
                                         CLEANUP_INTERVAL_MINUTES, 
                                         TimeUnit.MINUTES);
    }
    
    public static class ClientSession {
        private final String sessionId;
        private final String clientAddress;
        private final long createdTime;
        private volatile long lastActivity;
        private volatile String username;
        private volatile String role;
        private volatile boolean authenticated;
        private volatile int userId;
        private volatile String token;
        
        public ClientSession(String sessionId, String clientAddress) {
            this.sessionId = sessionId;
            this.clientAddress = clientAddress;
            this.createdTime = System.currentTimeMillis();
            this.lastActivity = createdTime;
            this.authenticated = false;
            this.token = generateToken();
        }
        
        public ClientSession(String clientAddress, int userId) {
            this.sessionId = generateSessionId();
            this.clientAddress = clientAddress;
            this.userId = userId;
            this.createdTime = System.currentTimeMillis();
            this.lastActivity = createdTime;
            this.authenticated = true;
            this.token = generateToken();
        }
        
        private String generateToken() {
            return java.util.UUID.randomUUID().toString();
        }
        
        private String generateSessionId() {
            return "session_" + System.currentTimeMillis() + "_" + Math.random();
        }
        
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void authenticate(String username, String role) {
            this.username = username;
            this.role = role;
            this.authenticated = true;
            updateActivity();
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - lastActivity) > (SESSION_TIMEOUT_MINUTES * 60 * 1000);
        }
        
        public boolean isAuthenticated() { return authenticated; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getSessionId() { return sessionId; }
        public String getClientAddress() { return clientAddress; }
        public long getLastActivity() { return lastActivity; }
        public String getToken() { return token; }
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
    }
    
    /**
     * Create new session for client
     */
    public static ClientSession createSession(String clientAddress) {
        String sessionId = generateSessionId(clientAddress);
        ClientSession session = new ClientSession(sessionId, clientAddress);
        sessions.put(sessionId, session);
        System.out.println("[INFO] Session created: " + sessionId + " for " + clientAddress);
        return session;
    }
    
    /**
     * Create new session for client with userId
     */
    public static ClientSession createSession(String clientAddress, int userId) {
        ClientSession session = new ClientSession(clientAddress, userId);
        sessions.put(session.getSessionId(), session);
        System.out.println("[INFO] Session created with userId: " + session.getSessionId() + " for " + clientAddress);
        return session;
    }
    
    /**
     * Get session by ID
     */
    public static ClientSession getSession(String sessionId) {
        ClientSession session = sessions.get(sessionId);
        if (session != null && !session.isExpired()) {
            session.updateActivity();
            return session;
        } else if (session != null && session.isExpired()) {
            sessions.remove(sessionId);
            System.out.println("[INFO] Session expired and removed: " + sessionId);
        }
        return null;
    }
    
    /**
     * Remove session
     */
    public static void removeSession(String sessionId) {
        ClientSession removed = sessions.remove(sessionId);
        if (removed != null) {
            System.out.println("[INFO] Session removed: " + sessionId);
        }
    }
    
    /**
     * Invalidate session by token
     */
    public static void invalidateSession(String token) {
        // Find session by token
        for (ClientSession session : sessions.values()) {
            if (token.equals(session.getToken())) {
                sessions.remove(session.getSessionId());
                System.out.println("[INFO] Session invalidated: " + session.getSessionId());
                return;
            }
        }
    }
    
    /**
     * Clean up expired sessions
     */
    public static void cleanupExpiredSessions() {
        int removedCount = 0;
        for (ConcurrentHashMap.Entry<String, ClientSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                sessions.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            System.out.println("[INFO] Cleaned up " + removedCount + " expired sessions");
        }
        
        // Also cleanup rate limiter
        RateLimiter.cleanup();
    }
    
    /**
     * Generate unique session ID
     */
    private static String generateSessionId(String clientAddress) {
        return "SESSION_" + System.currentTimeMillis() + "_" + 
               clientAddress.hashCode() + "_" + 
               (int)(Math.random() * 10000);
    }
    
    /**
     * Get active session count
     */
    public static int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Shutdown session manager
     */
    public static void shutdown() {
        cleanupService.shutdown();
        sessions.clear();
        System.out.println("[INFO] Session manager shut down");
    }
}