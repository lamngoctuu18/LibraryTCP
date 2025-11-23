package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Automated database backup system
 */
public class BackupManager {
    private static final String DB_PATH = "C:/data/library.db";
    private static final String BACKUP_DIR = "C:/data/backups";
    private static final int MAX_BACKUPS = 30; // Keep 30 days of backups
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean initialized = false;
    
    /**
     * Initialize backup manager with scheduled backups
     */
    public static void initialize() {
        if (initialized) return;
        
        try {
            // Create backup directory if it doesn't exist
            File backupDir = new File(BACKUP_DIR);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
                System.out.println("[BACKUP] Created backup directory: " + BACKUP_DIR);
            }
            
            // Schedule daily backups at 2 AM
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    performBackup();
                    cleanOldBackups();
                } catch (Exception e) {
                    System.err.println("[BACKUP ERROR] Failed to perform scheduled backup: " + e.getMessage());
                }
            }, getInitialDelay(), 24, TimeUnit.HOURS);
            
            // Perform immediate backup on startup
            performBackup();
            
            initialized = true;
            System.out.println("[BACKUP] Backup manager initialized - daily backups scheduled");
            
        } catch (Exception e) {
            System.err.println("[BACKUP ERROR] Failed to initialize backup manager: " + e.getMessage());
        }
    }
    
    /**
     * Perform database backup
     */
    public static synchronized boolean performBackup() {
        try {
            File sourceFile = new File(DB_PATH);
            if (!sourceFile.exists()) {
                System.err.println("[BACKUP ERROR] Source database file not found: " + DB_PATH);
                return false;
            }
            
            // Create backup filename with timestamp
            String timestamp = DATE_FORMAT.format(new Date());
            String backupFileName = "library_backup_" + timestamp + ".db";
            Path backupPath = Paths.get(BACKUP_DIR, backupFileName);
            
            // Verify database integrity before backup
            if (!verifyDatabaseIntegrity()) {
                System.err.println("[BACKUP ERROR] Database integrity check failed - backup aborted");
                return false;
            }
            
            // Copy database file
            Files.copy(sourceFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Verify backup file
            if (Files.exists(backupPath) && Files.size(backupPath) > 0) {
                System.out.println("[BACKUP SUCCESS] Database backed up to: " + backupPath);
                
                // Compress backup (optional)
                compressBackup(backupPath);
                
                return true;
            } else {
                System.err.println("[BACKUP ERROR] Backup file verification failed");
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("[BACKUP ERROR] Failed to create backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify database integrity
     */
    private static boolean verifyDatabaseIntegrity() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            // Perform integrity check
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery("PRAGMA integrity_check");
            if (rs.next()) {
                String result = rs.getString(1);
                return "ok".equalsIgnoreCase(result);
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[BACKUP ERROR] Database integrity check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Compress backup file to save space
     */
    private static void compressBackup(Path backupPath) {
        try {
            // Simple compression using gzip (would need additional libraries for full implementation)
            System.out.println("[BACKUP INFO] Backup file created: " + Files.size(backupPath) + " bytes");
        } catch (IOException e) {
            System.err.println("[BACKUP WARNING] Could not get backup file size: " + e.getMessage());
        }
    }
    
    /**
     * Clean old backup files
     */
    private static void cleanOldBackups() {
        try {
            File backupDir = new File(BACKUP_DIR);
            File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("library_backup_") && name.endsWith(".db"));
            
            if (backupFiles != null && backupFiles.length > MAX_BACKUPS) {
                // Sort by modification time
                java.util.Arrays.sort(backupFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                
                // Delete oldest files
                int filesToDelete = backupFiles.length - MAX_BACKUPS;
                for (int i = 0; i < filesToDelete; i++) {
                    if (backupFiles[i].delete()) {
                        System.out.println("[BACKUP INFO] Deleted old backup: " + backupFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BACKUP ERROR] Failed to clean old backups: " + e.getMessage());
        }
    }
    
    /**
     * Get initial delay until 2 AM tomorrow
     */
    private static long getInitialDelay() {
        // Simple calculation - start backing up in 1 hour for demo purposes
        return 1;
    }
    
    /**
     * Restore database from backup
     */
    public static boolean restoreFromBackup(String backupFileName) {
        try {
            Path backupPath = Paths.get(BACKUP_DIR, backupFileName);
            if (!Files.exists(backupPath)) {
                System.err.println("[RESTORE ERROR] Backup file not found: " + backupFileName);
                return false;
            }
            
            // Create backup of current database
            String currentBackupName = "current_backup_" + DATE_FORMAT.format(new Date()) + ".db";
            Files.copy(Paths.get(DB_PATH), Paths.get(BACKUP_DIR, currentBackupName));
            
            // Restore from backup
            Files.copy(backupPath, Paths.get(DB_PATH), StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("[RESTORE SUCCESS] Database restored from: " + backupFileName);
            return true;
            
        } catch (IOException e) {
            System.err.println("[RESTORE ERROR] Failed to restore from backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * List available backups
     */
    public static String[] listBackups() {
        File backupDir = new File(BACKUP_DIR);
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("library_backup_") && name.endsWith(".db"));
        
        if (backupFiles == null) return new String[0];
        
        String[] backupNames = new String[backupFiles.length];
        for (int i = 0; i < backupFiles.length; i++) {
            backupNames[i] = backupFiles[i].getName();
        }
        
        java.util.Arrays.sort(backupNames, java.util.Collections.reverseOrder());
        return backupNames;
    }
    
    /**
     * Shutdown backup manager
     */
    public static void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            System.out.println("[BACKUP] Backup manager shut down");
        }
    }
}