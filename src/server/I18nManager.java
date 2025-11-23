package server;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Internationalization support for multi-language library system
 */
public class I18nManager {
    private static final String DEFAULT_LANGUAGE = "en";
    private static final Map<String, Properties> languageCache = new HashMap<>();
    private static String currentLanguage = DEFAULT_LANGUAGE;
    
    static {
        loadLanguage("en"); // English
        loadLanguage("vi"); // Vietnamese
        loadLanguage("zh"); // Chinese
        loadLanguage("ja"); // Japanese
        loadLanguage("ko"); // Korean
    }
    
    /**
     * Load language file from resources
     */
    private static void loadLanguage(String language) {
        try {
            Properties props = new Properties();
            String filename = "/i18n/messages_" + language + ".properties";
            
            // Try to load from classpath
            InputStream is = I18nManager.class.getResourceAsStream(filename);
            if (is != null) {
                props.load(is);
                languageCache.put(language, props);
                System.out.println("[I18N] Loaded language: " + language);
            } else {
                // Load default messages if file not found
                loadDefaultMessages(language);
            }
        } catch (IOException e) {
            System.err.println("[I18N] Error loading language " + language + ": " + e.getMessage());
            loadDefaultMessages(language);
        }
    }
    
    /**
     * Load default messages for a language
     */
    private static void loadDefaultMessages(String language) {
        Properties props = new Properties();
        
        switch (language) {
            case "vi":
                loadVietnameseMessages(props);
                break;
            case "zh":
                loadChineseMessages(props);
                break;
            case "ja":
                loadJapaneseMessages(props);
                break;
            case "ko":
                loadKoreanMessages(props);
                break;
            default:
                loadEnglishMessages(props);
                break;
        }
        
        languageCache.put(language, props);
    }
    
    /**
     * Load English messages
     */
    private static void loadEnglishMessages(Properties props) {
        props.setProperty("welcome", "Welcome to Library Management System");
        props.setProperty("login.success", "Login successful");
        props.setProperty("login.failed", "Invalid username or password");
        props.setProperty("register.success", "Registration successful");
        props.setProperty("register.failed", "Registration failed");
        props.setProperty("book.added", "Book added successfully");
        props.setProperty("book.deleted", "Book deleted successfully");
        props.setProperty("book.not.found", "Book not found");
        props.setProperty("book.borrowed", "Book borrowed successfully");
        props.setProperty("book.returned", "Book returned successfully");
        props.setProperty("book.not.available", "Book not available");
        props.setProperty("permission.denied", "Access denied");
        props.setProperty("admin.required", "Administrator privileges required");
        props.setProperty("user.not.found", "User not found");
        props.setProperty("account.locked", "Account locked, please contact librarian");
        props.setProperty("search.no.results", "No books found");
        props.setProperty("error.general", "An error occurred");
        props.setProperty("rate.limit.exceeded", "Too many requests, please slow down");
        props.setProperty("session.expired", "Session expired, please login again");
    }
    
    /**
     * Load Vietnamese messages
     */
    private static void loadVietnameseMessages(Properties props) {
        props.setProperty("welcome", "Chào mừng đến Hệ thống Quản lý Thư viện");
        props.setProperty("login.success", "Đăng nhập thành công");
        props.setProperty("login.failed", "Tên đăng nhập hoặc mật khẩu không đúng");
        props.setProperty("register.success", "Đăng ký thành công");
        props.setProperty("register.failed", "Đăng ký thất bại");
        props.setProperty("book.added", "Thêm sách thành công");
        props.setProperty("book.deleted", "Xóa sách thành công");
        props.setProperty("book.not.found", "Không tìm thấy sách");
        props.setProperty("book.borrowed", "Mượn sách thành công");
        props.setProperty("book.returned", "Trả sách thành công");
        props.setProperty("book.not.available", "Sách không có sẵn");
        props.setProperty("permission.denied", "Truy cập bị từ chối");
        props.setProperty("admin.required", "Yêu cầu quyền quản trị viên");
        props.setProperty("user.not.found", "Không tìm thấy người dùng");
        props.setProperty("account.locked", "Tài khoản bị khóa, vui lòng liên hệ thủ thư");
        props.setProperty("search.no.results", "Không tìm thấy sách nào");
        props.setProperty("error.general", "Đã xảy ra lỗi");
        props.setProperty("rate.limit.exceeded", "Quá nhiều yêu cầu, vui lòng chậm lại");
        props.setProperty("session.expired", "Phiên làm việc hết hạn, vui lòng đăng nhập lại");
    }
    
    /**
     * Load Chinese messages
     */
    private static void loadChineseMessages(Properties props) {
        props.setProperty("welcome", "欢迎使用图书管理系统");
        props.setProperty("login.success", "登录成功");
        props.setProperty("login.failed", "用户名或密码错误");
        props.setProperty("register.success", "注册成功");
        props.setProperty("register.failed", "注册失败");
        props.setProperty("book.added", "添加图书成功");
        props.setProperty("book.deleted", "删除图书成功");
        props.setProperty("book.not.found", "未找到图书");
        props.setProperty("book.borrowed", "借书成功");
        props.setProperty("book.returned", "还书成功");
        props.setProperty("book.not.available", "图书不可借");
        props.setProperty("permission.denied", "拒绝访问");
        props.setProperty("admin.required", "需要管理员权限");
        props.setProperty("user.not.found", "未找到用户");
        props.setProperty("account.locked", "账户已锁定，请联系管理员");
        props.setProperty("search.no.results", "没有找到图书");
        props.setProperty("error.general", "发生错误");
        props.setProperty("rate.limit.exceeded", "请求过多，请稍后再试");
        props.setProperty("session.expired", "会话已过期，请重新登录");
    }
    
    /**
     * Load Japanese messages
     */
    private static void loadJapaneseMessages(Properties props) {
        props.setProperty("welcome", "図書管理システムへようこそ");
        props.setProperty("login.success", "ログイン成功");
        props.setProperty("login.failed", "ユーザー名またはパスワードが間違っています");
        props.setProperty("book.added", "本の追加に成功しました");
        props.setProperty("book.borrowed", "貸出成功");
        props.setProperty("permission.denied", "アクセス拒否");
        props.setProperty("admin.required", "管理者権限が必要です");
        // Add more Japanese translations as needed
    }
    
    /**
     * Load Korean messages
     */
    private static void loadKoreanMessages(Properties props) {
        props.setProperty("welcome", "도서관 관리 시스템에 오신 것을 환영합니다");
        props.setProperty("login.success", "로그인 성공");
        props.setProperty("login.failed", "사용자명 또는 비밀번호가 틀렸습니다");
        props.setProperty("book.added", "책 추가 성공");
        props.setProperty("book.borrowed", "대출 성공");
        props.setProperty("permission.denied", "접근 거부");
        props.setProperty("admin.required", "관리자 권한이 필요합니다");
        // Add more Korean translations as needed
    }
    
    /**
     * Set current language
     */
    public static void setLanguage(String language) {
        if (languageCache.containsKey(language)) {
            currentLanguage = language;
            System.out.println("[I18N] Language changed to: " + language);
        } else {
            System.err.println("[I18N] Unsupported language: " + language);
        }
    }
    
    /**
     * Get localized message
     */
    public static String getMessage(String key) {
        return getMessage(key, currentLanguage);
    }
    
    /**
     * Get localized message for specific language
     */
    public static String getMessage(String key, String language) {
        Properties props = languageCache.get(language);
        if (props == null) {
            props = languageCache.get(DEFAULT_LANGUAGE);
        }
        
        String message = props.getProperty(key);
        if (message == null) {
            // Fallback to English if key not found
            Properties englishProps = languageCache.get(DEFAULT_LANGUAGE);
            message = englishProps.getProperty(key, "[" + key + "]");
        }
        
        return message;
    }
    
    /**
     * Get localized message with parameters
     */
    public static String getMessage(String key, Object... params) {
        String message = getMessage(key);
        
        for (int i = 0; i < params.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(params[i]));
        }
        
        return message;
    }
    
    /**
     * Detect language from request headers
     */
    public static String detectLanguage(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null || acceptLanguageHeader.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        
        // Simple language detection from Accept-Language header
        String[] languages = acceptLanguageHeader.toLowerCase().split(",");
        for (String lang : languages) {
            String langCode = lang.split(";")[0].trim();
            
            if (langCode.startsWith("vi")) return "vi";
            if (langCode.startsWith("zh")) return "zh";
            if (langCode.startsWith("ja")) return "ja";
            if (langCode.startsWith("ko")) return "ko";
            if (langCode.startsWith("en")) return "en";
        }
        
        return DEFAULT_LANGUAGE;
    }
    
    /**
     * Get available languages
     */
    public static String[] getAvailableLanguages() {
        return languageCache.keySet().toArray(new String[0]);
    }
    
    /**
     * Get current language
     */
    public static String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Format date according to locale
     */
    public static String formatDate(long timestamp, String language) {
        java.text.SimpleDateFormat sdf;
        
        switch (language) {
            case "vi":
                sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                break;
            case "zh":
                sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                break;
            case "ja":
                sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                break;
            case "ko":
                sdf = new java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH:mm:ss");
                break;
            default:
                sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                break;
        }
        
        return sdf.format(new java.util.Date(timestamp));
    }
}