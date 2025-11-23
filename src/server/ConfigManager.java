package server;

import java.io.*;
import java.util.Properties;

/**
 * Configuration management for server settings
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "server.properties";
    private static final Properties config = new Properties();
    private static final Properties defaultConfig = new Properties();
    
    static {
        loadDefaultConfig();
        loadConfig();
    }
    
    /**
     * Load default configuration values
     */
    private static void loadDefaultConfig() {
        defaultConfig.setProperty("server.port", "12345");
        defaultConfig.setProperty("server.thread.pool.size", "20");
        defaultConfig.setProperty("database.url", "jdbc:sqlite:C:/data/library.db");
        defaultConfig.setProperty("database.pool.min", "5");
        defaultConfig.setProperty("database.pool.max", "20");
        defaultConfig.setProperty("session.timeout.minutes", "30");
        defaultConfig.setProperty("rate.limit.requests.per.minute", "60");
        defaultConfig.setProperty("rate.limit.requests.per.second", "10");
        defaultConfig.setProperty("backup.enabled", "true");
        defaultConfig.setProperty("backup.directory", "C:/data/backups");
        defaultConfig.setProperty("backup.max.count", "30");
        defaultConfig.setProperty("backup.interval.hours", "24");
        defaultConfig.setProperty("metrics.enabled", "true");
        defaultConfig.setProperty("logging.level", "INFO");
        defaultConfig.setProperty("security.password.min.length", "6");
        defaultConfig.setProperty("security.password.require.mixed.case", "true");
        defaultConfig.setProperty("security.password.require.numbers", "true");
        defaultConfig.setProperty("search.results.max.limit", "100");
        defaultConfig.setProperty("rest.api.enabled", "true");
        defaultConfig.setProperty("server.rest.api.port", "8082");
        defaultConfig.setProperty("i18n.default.language", "en");
        defaultConfig.setProperty("recommendation.enabled", "true");
    }
    
    /**
     * Load configuration from file
     */
    private static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
                System.out.println("[CONFIG] Loaded configuration from: " + CONFIG_FILE);
            } catch (IOException e) {
                System.err.println("[CONFIG] Error loading config file: " + e.getMessage());
                System.out.println("[CONFIG] Using default configuration");
            }
        } else {
            System.out.println("[CONFIG] No config file found, using defaults");
            createDefaultConfigFile();
        }
    }
    
    /**
     * Create default configuration file
     */
    private static void createDefaultConfigFile() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            defaultConfig.store(fos, "Library Server Configuration");
            System.out.println("[CONFIG] Created default config file: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("[CONFIG] Could not create config file: " + e.getMessage());
        }
    }
    
    /**
     * Get configuration value as string
     */
    public static String getString(String key) {
        return config.getProperty(key, defaultConfig.getProperty(key));
    }
    
    /**
     * Get configuration value as integer
     */
    public static int getInt(String key) {
        String value = getString(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("[CONFIG] Invalid integer value for " + key + ": " + value);
            return Integer.parseInt(defaultConfig.getProperty(key));
        }
    }
    
    /**
     * Get configuration value as boolean
     */
    public static boolean getBoolean(String key) {
        String value = getString(key);
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Get configuration value as long
     */
    public static long getLong(String key) {
        String value = getString(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            System.err.println("[CONFIG] Invalid long value for " + key + ": " + value);
            return Long.parseLong(defaultConfig.getProperty(key));
        }
    }
    
    /**
     * Set configuration value
     */
    public static void setProperty(String key, String value) {
        config.setProperty(key, value);
    }
    
    /**
     * Save configuration to file
     */
    public static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            config.store(fos, "Library Server Configuration - Updated: " + new java.util.Date());
            System.out.println("[CONFIG] Configuration saved to: " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("[CONFIG] Error saving config: " + e.getMessage());
        }
    }
    
    /**
     * Reload configuration from file
     */
    public static void reloadConfig() {
        config.clear();
        loadConfig();
        System.out.println("[CONFIG] Configuration reloaded");
    }
    
    /**
     * Get all configuration as Properties object
     */
    public static Properties getAllConfig() {
        Properties allConfig = new Properties();
        allConfig.putAll(defaultConfig);
        allConfig.putAll(config);
        return allConfig;
    }
    
    /**
     * Display current configuration
     */
    public static void printConfig() {
        System.out.println("=== CURRENT CONFIGURATION ===");
        Properties allConfig = getAllConfig();
        for (String key : allConfig.stringPropertyNames()) {
            System.out.println(key + " = " + allConfig.getProperty(key));
        }
        System.out.println("==============================");
    }
    
    /**
     * Validate configuration values
     */
    public static boolean validateConfig() {
        boolean valid = true;
        
        // Validate port range
        int port = getInt("server.port");
        if (port < 1024 || port > 65535) {
            System.err.println("[CONFIG] Invalid port number: " + port);
            valid = false;
        }
        
        // Validate thread pool size
        int threadPoolSize = getInt("server.thread.pool.size");
        if (threadPoolSize < 1 || threadPoolSize > 1000) {
            System.err.println("[CONFIG] Invalid thread pool size: " + threadPoolSize);
            valid = false;
        }
        
        // Validate database pool settings
        int minPool = getInt("database.pool.min");
        int maxPool = getInt("database.pool.max");
        if (minPool > maxPool || minPool < 1) {
            System.err.println("[CONFIG] Invalid database pool configuration");
            valid = false;
        }
        
        return valid;
    }
}