package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance metrics collection and monitoring
 */
public class MetricsCollector {
    
    // Request metrics
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final AtomicLong successfulRequests = new AtomicLong(0);
    private static final AtomicLong failedRequests = new AtomicLong(0);
    
    // Response time tracking
    private static final AtomicLong totalResponseTime = new AtomicLong(0);
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    
    // Command specific metrics
    private static final ConcurrentHashMap<String, AtomicLong> commandCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> commandResponseTimes = new ConcurrentHashMap<>();
    
    // Error tracking
    private static final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    // Security metrics
    private static final AtomicLong rateLimitViolations = new AtomicLong(0);
    private static final AtomicLong authFailures = new AtomicLong(0);
    
    // Cloud metrics
    private static final AtomicLong cloudUploads = new AtomicLong(0);
    private static final AtomicLong cloudDownloads = new AtomicLong(0);
    private static final AtomicLong cloudErrors = new AtomicLong(0);
    private static final AtomicLong unauthorizedAccess = new AtomicLong(0);
    
    // Database metrics
    private static final AtomicLong dbQueries = new AtomicLong(0);
    private static final AtomicLong dbErrors = new AtomicLong(0);
    
    /**
     * Record request metrics
     */
    public static void recordRequest(String command, long responseTimeMs, boolean success) {
        totalRequests.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        // Track command specific metrics
        commandCounts.computeIfAbsent(command, k -> new AtomicLong(0)).incrementAndGet();
        commandResponseTimes.computeIfAbsent(command, k -> new AtomicLong(0)).addAndGet(responseTimeMs);
    }
    
    /**
     * Record error
     */
    public static void recordError(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Record security event
     */
    public static void recordSecurityEvent(String eventType) {
        switch (eventType.toLowerCase()) {
            case "rate_limit":
                rateLimitViolations.incrementAndGet();
                break;
            case "auth_failure":
                authFailures.incrementAndGet();
                break;
            case "unauthorized":
                unauthorizedAccess.incrementAndGet();
                break;
        }
    }
    
    /**
     * Record database operation
     */
    public static void recordDatabaseOperation(boolean success) {
        dbQueries.incrementAndGet();
        if (!success) {
            dbErrors.incrementAndGet();
        }
    }
    
    /**
     * Track connection count
     */
    public static void connectionOpened() {
        activeConnections.incrementAndGet();
    }
    
    public static void connectionClosed() {
        activeConnections.decrementAndGet();
    }
    
    /**
     * Get comprehensive metrics report
     */
    public static String getMetricsReport() {
        StringBuilder report = new StringBuilder();
        
        // Overall metrics
        long total = totalRequests.get();
        long avgResponseTime = total > 0 ? totalResponseTime.get() / total : 0;
        double successRate = total > 0 ? (successfulRequests.get() * 100.0 / total) : 0;
        
        report.append("=== PERFORMANCE METRICS ===\\n");
        report.append("Total Requests: ").append(total).append("\\n");
        report.append("Success Rate: ").append(String.format("%.2f%%", successRate)).append("\\n");
        report.append("Average Response Time: ").append(avgResponseTime).append("ms\\n");
        report.append("Active Connections: ").append(activeConnections.get()).append("\\n");
        report.append("Database Queries: ").append(dbQueries.get()).append("\\n");
        report.append("Database Error Rate: ");
        if (dbQueries.get() > 0) {
            double dbErrorRate = (dbErrors.get() * 100.0 / dbQueries.get());
            report.append(String.format("%.2f%%", dbErrorRate));
        } else {
            report.append("0%");
        }
        report.append("\\n\\n");
        
        // Command metrics
        if (!commandCounts.isEmpty()) {
            report.append("=== COMMAND METRICS ===\\n");
            for (Map.Entry<String, AtomicLong> entry : commandCounts.entrySet()) {
                String cmd = entry.getKey();
                long count = entry.getValue().get();
                long totalTime = commandResponseTimes.getOrDefault(cmd, new AtomicLong(0)).get();
                long avgTime = count > 0 ? totalTime / count : 0;
                
                report.append(cmd).append(": ")
                      .append(count).append(" requests, ")
                      .append(avgTime).append("ms avg\\n");
            }
            report.append("\\n");
        }
        
        // Security metrics
        report.append("=== SECURITY METRICS ===\\n");
        report.append("Rate Limit Violations: ").append(rateLimitViolations.get()).append("\\n");
        report.append("Authentication Failures: ").append(authFailures.get()).append("\\n");
        report.append("Unauthorized Access Attempts: ").append(unauthorizedAccess.get()).append("\\n");
        
        // Error summary
        if (!errorCounts.isEmpty()) {
            report.append("\n=== ERROR SUMMARY ===\n");
            for (Map.Entry<String, AtomicLong> entry : errorCounts.entrySet()) {
                report.append("    ").append(entry.getKey()).append(": ").append(entry.getValue().get()).append("\n");
            }
        }
        
        // Cloud metrics
        report.append("Cloud Operations:\n");
        report.append("  Uploads: ").append(cloudUploads.get()).append("\n");
        report.append("  Downloads: ").append(cloudDownloads.get()).append("\n");
        report.append("  Errors: ").append(cloudErrors.get()).append("\n");
        
        // Connection pool metrics
        report.append("\n=== CONNECTION POOL ===\n");
        report.append("Available Connections: ").append(DatabasePool.getInstance().getAvailableConnections()).append("\n");
        report.append("Used Connections: ").append(DatabasePool.getInstance().getUsedConnections()).append("\n");
        
        // Session metrics
        report.append("\n=== SESSION MANAGEMENT ===\n");
        report.append("Active Sessions: ").append(SessionManager.getActiveSessionCount()).append("\n");
        
        return report.toString();
    }
    
    /**
     * Get lightweight status for health checks
     */
    public static String getHealthStatus() {
        boolean healthy = true;
        StringBuilder status = new StringBuilder();
        
        // Check success rate
        long total = totalRequests.get();
        if (total > 100) { // Only check if we have enough data
            double successRate = (successfulRequests.get() * 100.0 / total);
            if (successRate < 95.0) {
                healthy = false;
                status.append("Low success rate: ").append(String.format("%.2f%%", successRate)).append("; ");
            }
        }
        
        // Check database error rate
        if (dbQueries.get() > 50) {
            double dbErrorRate = (dbErrors.get() * 100.0 / dbQueries.get());
            if (dbErrorRate > 5.0) {
                healthy = false;
                status.append("High DB error rate: ").append(String.format("%.2f%%", dbErrorRate)).append("; ");
            }
        }
        
        // Check connection pool
        if (DatabasePool.getInstance().getAvailableConnections() == 0) {
            healthy = false;
            status.append("Connection pool exhausted; ");
        }
        
        if (healthy) {
            return "HEALTHY|System operating normally";
        } else {
            return "UNHEALTHY|" + status.toString().trim();
        }
    }
    
    /**
     * Reset all metrics (for testing or periodic reset)
     */
    public static void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTime.set(0);
        
        commandCounts.clear();
        commandResponseTimes.clear();
        errorCounts.clear();
        
        rateLimitViolations.set(0);
        authFailures.set(0);
        unauthorizedAccess.set(0);
        
        dbQueries.set(0);
        dbErrors.set(0);
        
        System.out.println("[METRICS] All metrics reset");
    }
    
    /**
     * Record cloud upload operation
     */
    public static void recordCloudUpload() {
        System.out.println("[METRICS] Cloud upload recorded");
    }
}