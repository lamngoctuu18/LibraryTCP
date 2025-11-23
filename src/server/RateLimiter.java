package server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting implementation to prevent abuse
 */
public class RateLimiter {
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_REQUESTS_PER_SECOND = 10;
    private static final long MINUTE_IN_MILLIS = 60_000;
    private static final long SECOND_IN_MILLIS = 1_000;
    
    private static final ConcurrentHashMap<String, ClientRateInfo> clientRates = new ConcurrentHashMap<>();
    private static RateLimiter instance = new RateLimiter();
    
    private RateLimiter() {
        // Private constructor for singleton
    }
    
    public static RateLimiter getInstance() {
        return instance;
    }
    
    private static class ClientRateInfo {
        private final AtomicInteger requestsThisMinute = new AtomicInteger(0);
        private final AtomicInteger requestsThisSecond = new AtomicInteger(0);
        private volatile long lastMinuteReset = System.currentTimeMillis();
        private volatile long lastSecondReset = System.currentTimeMillis();
        
        public boolean isAllowed() {
            long now = System.currentTimeMillis();
            
            // Reset second counter
            if (now - lastSecondReset > SECOND_IN_MILLIS) {
                requestsThisSecond.set(0);
                lastSecondReset = now;
            }
            
            // Reset minute counter
            if (now - lastMinuteReset > MINUTE_IN_MILLIS) {
                requestsThisMinute.set(0);
                lastMinuteReset = now;
            }
            
            // Check limits
            if (requestsThisSecond.get() >= MAX_REQUESTS_PER_SECOND ||
                requestsThisMinute.get() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            
            // Increment counters
            requestsThisSecond.incrementAndGet();
            requestsThisMinute.incrementAndGet();
            return true;
        }
    }
    
    /**
     * Check if client is allowed to make a request
     */
    public boolean isAllowed(String clientIdentifier) {
        if (clientIdentifier == null) return false;
        
        ClientRateInfo rateInfo = clientRates.computeIfAbsent(clientIdentifier, k -> new ClientRateInfo());
        return rateInfo.isAllowed();
    }
    
    /**
     * Clean up old client rate information
     */
    public static void cleanup() {
        long now = System.currentTimeMillis();
        clientRates.entrySet().removeIf(entry -> {
            ClientRateInfo info = entry.getValue();
            return (now - info.lastMinuteReset) > MINUTE_IN_MILLIS * 5; // Keep for 5 minutes
        });
    }
}