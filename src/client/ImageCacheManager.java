package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ImageCacheManager {

    private static ImageCacheManager instance;

    private static final String CACHE_DIR = "C:/data/library_images/";

    private final Map<String, ImageIcon> memoryCache;

    private static final int MAX_MEMORY_CACHE = 100;

    private ImageCacheManager() {
        this.memoryCache = new HashMap<>();
        initializeCacheDirectory();
    }

    public static synchronized ImageCacheManager getInstance() {
        if (instance == null) {
            instance = new ImageCacheManager();
        }
        return instance;
    }

    private void initializeCacheDirectory() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
                System.out.println("✅ Đã tạo thư mục cache ảnh: " + CACHE_DIR);
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi tạo thư mục cache: " + e.getMessage());
        }
    }

    public String downloadAndCacheImage(String imageUrl, String bookId) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        try {

            String fileName = generateFileName(imageUrl, bookId);
            String localPath = CACHE_DIR + fileName;

            File localFile = new File(localPath);
            if (localFile.exists()) {
                System.out.println("📁 Ảnh đã tồn tại trong cache: " + fileName);
                return localPath;
            }

            System.out.println("⬇️ Đang tải ảnh từ: " + imageUrl);
            URL url = new URL(imageUrl);
            BufferedImage image = ImageIO.read(url);

            if (image == null) {
                System.err.println("❌ Không thể đọc ảnh từ URL: " + imageUrl);
                return null;
            }

            String fileExtension = getFileExtension(imageUrl);
            ImageIO.write(image, fileExtension, localFile);

            System.out.println("✅ Đã lưu ảnh vào: " + localPath);
            return localPath;

        } catch (Exception e) {
            System.err.println("❌ Lỗi tải ảnh: " + e.getMessage());
            return null;
        }
    }

    public ImageIcon getImage(String imagePath, String bookId, int maxWidth, int maxHeight) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }

        String cacheKey = imagePath + "_" + maxWidth + "x" + maxHeight;

        if (memoryCache.containsKey(cacheKey)) {
            System.out.println("🚀 Load ảnh từ RAM cache");
            return memoryCache.get(cacheKey);
        }

        ImageIcon icon = null;

        try {

            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {

                String localPath = downloadAndCacheImage(imagePath, bookId);
                if (localPath != null) {
                    icon = loadImageFromFile(localPath);
                }
            } else {

                icon = loadImageFromFile(imagePath);
            }

            if (icon != null && icon.getIconWidth() > 0) {
                icon = scaleImage(icon, maxWidth, maxHeight);

                addToMemoryCache(cacheKey, icon);
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi load ảnh: " + e.getMessage());
        }

        return icon;
    }

    private ImageIcon loadImageFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("❌ File không tồn tại: " + filePath);
                return null;
            }

            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                return null;
            }

            System.out.println("📂 Load ảnh từ ổ đĩa: " + filePath);
            return new ImageIcon(img);

        } catch (Exception e) {
            System.err.println("❌ Lỗi đọc file ảnh: " + e.getMessage());
            return null;
        }
    }

    private ImageIcon scaleImage(ImageIcon original, int maxWidth, int maxHeight) {
        int originalWidth = original.getIconWidth();
        int originalHeight = original.getIconHeight();

        double scaleWidth = (double) maxWidth / originalWidth;
        double scaleHeight = (double) maxHeight / originalHeight;
        double scale = Math.min(scaleWidth, scaleHeight);

        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        Image scaledImage = original.getImage().getScaledInstance(
            scaledWidth, scaledHeight, Image.SCALE_SMOOTH
        );

        return new ImageIcon(scaledImage);
    }

    private void addToMemoryCache(String key, ImageIcon icon) {

        if (memoryCache.size() >= MAX_MEMORY_CACHE) {
            String oldestKey = memoryCache.keySet().iterator().next();
            memoryCache.remove(oldestKey);
            System.out.println("🗑️ Đã xóa ảnh cũ khỏi RAM cache");
        }

        memoryCache.put(key, icon);
    }

    public void removeImageFromCache(String bookId) {
        try {

            memoryCache.entrySet().removeIf(entry ->
                entry.getKey().contains("book_" + bookId)
            );

            File cacheDir = new File(CACHE_DIR);
            File[] files = cacheDir.listFiles((dir, name) ->
                name.startsWith("book_" + bookId)
            );

            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        System.out.println("🗑️ Đã xóa ảnh: " + file.getName());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi xóa cache: " + e.getMessage());
        }
    }

    public void clearMemoryCache() {
        memoryCache.clear();
        System.out.println("🗑️ Đã xóa toàn bộ RAM cache");
    }

    private String generateFileName(String url, String bookId) {
        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(url.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            String extension = getFileExtension(url);
            return "book_" + bookId + "_" + hexString.substring(0, 8) + "." + extension;

        } catch (Exception e) {

            return "book_" + bookId + "_" + System.currentTimeMillis() + ".jpg";
        }
    }

    private String getFileExtension(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return "png";
        if (lower.contains(".gif")) return "gif";
        if (lower.contains(".bmp")) return "bmp";
        return "jpg";
    }

    public String getCacheInfo() {
        File cacheDir = new File(CACHE_DIR);
        int diskCacheCount = 0;
        long totalSize = 0;

        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                diskCacheCount = files.length;
                for (File file : files) {
                    totalSize += file.length();
                }
            }
        }

        return String.format(
            "💾 Cache Info:\n" +
            "- Ảnh trong RAM: %d/%d\n" +
            "- Ảnh trên ổ đĩa: %d\n" +
            "- Dung lượng: %.2f MB",
            memoryCache.size(), MAX_MEMORY_CACHE,
            diskCacheCount,
            totalSize / 1024.0 / 1024.0
        );
    }
}
