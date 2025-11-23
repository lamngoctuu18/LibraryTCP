package server;

import dao.BookDAO;
import dao.BorrowDAO;
import dao.UserDAO;
import model.Book;
import model.Borrow;
import model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI-powered recommendation system for personalized book recommendations
 */
public class RecommendationEngine {
    private final BookDAO bookDAO;
    private final BorrowDAO borrowDAO;
    private final UserDAO userDAO;
    
    // Cache for user preferences and book features
    private final Map<Integer, UserProfile> userProfiles = new ConcurrentHashMap<>();
    private final Map<Integer, BookFeatures> bookFeatures = new ConcurrentHashMap<>();
    
    // Similarity matrices
    private final Map<Integer, Map<Integer, Double>> userSimilarity = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, Double>> bookSimilarity = new ConcurrentHashMap<>();
    
    // Last update timestamps
    private long lastModelUpdate = 0;
    private static final long MODEL_UPDATE_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
    
    public RecommendationEngine(BookDAO bookDAO, BorrowDAO borrowDAO, UserDAO userDAO) {
        this.bookDAO = bookDAO;
        this.borrowDAO = borrowDAO;
        this.userDAO = userDAO;
        
        // Initialize recommendation models
        initializeModels();
    }
    
    /**
     * Initialize recommendation models
     */
    private void initializeModels() {
        try {
            System.out.println("[AI] Initializing recommendation models...");
            
            // Build user profiles
            buildUserProfiles();
            
            // Extract book features
            extractBookFeatures();
            
            // Calculate similarity matrices
            calculateUserSimilarity();
            calculateBookSimilarity();
            
            lastModelUpdate = System.currentTimeMillis();
            System.out.println("[AI] Recommendation models initialized successfully");
            
        } catch (Exception e) {
            System.err.println("[AI] Error initializing models: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Build user reading profiles based on borrowing history
     */
    private void buildUserProfiles() {
        try {
            List<User> users = userDAO.getAllUsers();
            
            for (User user : users) {
                UserProfile profile = new UserProfile(user.getId());
                List<Borrow> borrowHistory = borrowDAO.getBorrowsByUserId(user.getId());
                
                // Analyze borrowing patterns
                Map<String, Integer> categoryCount = new HashMap<>();
                Map<String, Integer> authorCount = new HashMap<>();
                Set<String> genres = new HashSet<>();
                
                double totalRating = 0;
                int ratingCount = 0;
                
                for (Borrow borrow : borrowHistory) {
                    Book book = bookDAO.getBookById(borrow.getBookId());
                    if (book != null) {
                        // Category preferences
                        String category = book.getCategory();
                        if (category != null && !category.isEmpty()) {
                            categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                        }
                        
                        // Author preferences
                        String author = book.getAuthor();
                        if (author != null && !author.isEmpty()) {
                            authorCount.put(author, authorCount.getOrDefault(author, 0) + 1);
                        }
                        
                        // Genre extraction (simplified - from title/description keywords)
                        extractGenres(book, genres);
                        
                        // Simulate rating based on borrowing frequency and return time
                        double implicitRating = calculateImplicitRating(borrow);
                        totalRating += implicitRating;
                        ratingCount++;
                    }
                }
                
                // Set profile preferences
                profile.setCategoryPreferences(categoryCount);
                profile.setAuthorPreferences(authorCount);
                profile.setGenres(genres);
                profile.setAverageRating(ratingCount > 0 ? totalRating / ratingCount : 3.0);
                profile.setTotalBorrows(borrowHistory.size());
                
                userProfiles.put(user.getId(), profile);
            }
            
            System.out.println("[AI] Built profiles for " + userProfiles.size() + " users");
            
        } catch (Exception e) {
            System.err.println("[AI] Error building user profiles: " + e.getMessage());
        }
    }
    
    /**
     * Extract book features for content-based filtering
     */
    private void extractBookFeatures() {
        try {
            List<Book> books = bookDAO.getAllBooks();
            
            for (Book book : books) {
                BookFeatures features = new BookFeatures(book.getId());
                
                // Basic features
                features.setCategory(book.getCategory());
                features.setAuthor(book.getAuthor());
                features.setTitle(book.getTitle());
                
                // Extract keywords from title and description
                Set<String> keywords = extractKeywords(book.getTitle() + " " + 
                    (book.getDescription() != null ? book.getDescription() : ""));
                features.setKeywords(keywords);
                
                // Calculate popularity score based on borrowing frequency
                int borrowCount = borrowDAO.getBorrowCountByBookId(book.getId());
                features.setPopularityScore(Math.log(borrowCount + 1)); // Log to reduce skew
                
                // Calculate average rating (simulated)
                double avgRating = calculateBookRating(book.getId());
                features.setAverageRating(avgRating);
                
                bookFeatures.put(book.getId(), features);
            }
            
            System.out.println("[AI] Extracted features for " + bookFeatures.size() + " books");
            
        } catch (Exception e) {
            System.err.println("[AI] Error extracting book features: " + e.getMessage());
        }
    }
    
    /**
     * Calculate user-user similarity for collaborative filtering
     */
    private void calculateUserSimilarity() {
        try {
            List<Integer> userIds = new ArrayList<>(userProfiles.keySet());
            
            for (int i = 0; i < userIds.size(); i++) {
                Integer userId1 = userIds.get(i);
                Map<Integer, Double> similarities = new HashMap<>();
                
                for (int j = i + 1; j < userIds.size(); j++) {
                    Integer userId2 = userIds.get(j);
                    
                    double similarity = calculateCosineSimilarity(
                        userProfiles.get(userId1), 
                        userProfiles.get(userId2)
                    );
                    
                    similarities.put(userId2, similarity);
                    
                    // Add symmetric relationship
                    userSimilarity.computeIfAbsent(userId2, k -> new HashMap<>())
                               .put(userId1, similarity);
                }
                
                userSimilarity.put(userId1, similarities);
            }
            
            System.out.println("[AI] Calculated user similarity matrix");
            
        } catch (Exception e) {
            System.err.println("[AI] Error calculating user similarity: " + e.getMessage());
        }
    }
    
    /**
     * Calculate book-book similarity for content-based filtering
     */
    private void calculateBookSimilarity() {
        try {
            List<Integer> bookIds = new ArrayList<>(bookFeatures.keySet());
            
            for (int i = 0; i < bookIds.size(); i++) {
                Integer bookId1 = bookIds.get(i);
                Map<Integer, Double> similarities = new HashMap<>();
                
                for (int j = i + 1; j < bookIds.size(); j++) {
                    Integer bookId2 = bookIds.get(j);
                    
                    double similarity = calculateBookSimilarity(
                        bookFeatures.get(bookId1), 
                        bookFeatures.get(bookId2)
                    );
                    
                    if (similarity > 0.1) { // Only store significant similarities
                        similarities.put(bookId2, similarity);
                        
                        // Add symmetric relationship
                        bookSimilarity.computeIfAbsent(bookId2, k -> new HashMap<>())
                                     .put(bookId1, similarity);
                    }
                }
                
                if (!similarities.isEmpty()) {
                    bookSimilarity.put(bookId1, similarities);
                }
            }
            
            System.out.println("[AI] Calculated book similarity matrix");
            
        } catch (Exception e) {
            System.err.println("[AI] Error calculating book similarity: " + e.getMessage());
        }
    }
    
    /**
     * Get personalized book recommendations for a user
     */
    public List<Book> getRecommendations(int userId, int count) {
        updateModelsIfNeeded();
        
        Set<Integer> recommendedBookIds = new HashSet<>();
        
        // Collaborative filtering recommendations (60% weight)
        List<Integer> collaborativeRecs = getCollaborativeRecommendations(userId, count * 2);
        recommendedBookIds.addAll(collaborativeRecs);
        
        // Content-based recommendations (40% weight)
        List<Integer> contentRecs = getContentBasedRecommendations(userId, count * 2);
        recommendedBookIds.addAll(contentRecs);
        
        // Popularity-based recommendations (for new users or cold start)
        if (recommendedBookIds.size() < count) {
            List<Integer> popularRecs = getPopularityRecommendations(count);
            recommendedBookIds.addAll(popularRecs);
        }
        
        // Remove books already borrowed by user
        Set<Integer> borrowedBooks = getBorrowedBookIds(userId);
        recommendedBookIds.removeAll(borrowedBooks);
        
        // Convert to Book objects and limit results
        return recommendedBookIds.stream()
            .limit(count)
            .map(id -> bookDAO.getBookById(id))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get similar books based on content
     */
    public List<Book> getSimilarBooks(int bookId, int count) {
        updateModelsIfNeeded();
        
        Map<Integer, Double> similarities = bookSimilarity.get(bookId);
        if (similarities == null || similarities.isEmpty()) {
            return getPopularityRecommendations(count).stream()
                .map(id -> bookDAO.getBookById(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        
        return similarities.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .limit(count)
            .map(entry -> bookDAO.getBookById(entry.getKey()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Get collaborative filtering recommendations
     */
    private List<Integer> getCollaborativeRecommendations(int userId, int count) {
        Map<Integer, Double> userSims = userSimilarity.get(userId);
        if (userSims == null || userSims.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Find top similar users
        List<Integer> similarUsers = userSims.entrySet().stream()
            .filter(entry -> entry.getValue() > 0.3) // Minimum similarity threshold
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Aggregate recommendations from similar users
        Map<Integer, Double> bookScores = new HashMap<>();
        Set<Integer> userBooks = getBorrowedBookIds(userId);
        
        for (Integer similarUserId : similarUsers) {
            Set<Integer> similarUserBooks = getBorrowedBookIds(similarUserId);
            double userWeight = userSims.get(similarUserId);
            
            for (Integer bookId : similarUserBooks) {
                if (!userBooks.contains(bookId)) {
                    bookScores.put(bookId, bookScores.getOrDefault(bookId, 0.0) + userWeight);
                }
            }
        }
        
        return bookScores.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Get content-based recommendations
     */
    private List<Integer> getContentBasedRecommendations(int userId, int count) {
        UserProfile profile = userProfiles.get(userId);
        if (profile == null) {
            return new ArrayList<>();
        }
        
        Map<Integer, Double> bookScores = new HashMap<>();
        Set<Integer> userBooks = getBorrowedBookIds(userId);
        
        for (Map.Entry<Integer, BookFeatures> entry : bookFeatures.entrySet()) {
            Integer bookId = entry.getKey();
            if (userBooks.contains(bookId)) continue;
            
            BookFeatures features = entry.getValue();
            double score = calculateContentScore(profile, features);
            
            if (score > 0) {
                bookScores.put(bookId, score);
            }
        }
        
        return bookScores.entrySet().stream()
            .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Get popularity-based recommendations
     */
    private List<Integer> getPopularityRecommendations(int count) {
        return bookFeatures.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue().getPopularityScore(), 
                                             e1.getValue().getPopularityScore()))
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Update models if needed
     */
    private void updateModelsIfNeeded() {
        if (System.currentTimeMillis() - lastModelUpdate > MODEL_UPDATE_INTERVAL) {
            System.out.println("[AI] Updating recommendation models...");
            initializeModels();
        }
    }
    
    // Helper methods
    private void extractGenres(Book book, Set<String> genres) {
        String text = (book.getTitle() + " " + (book.getDescription() != null ? book.getDescription() : "")).toLowerCase();
        
        // Simple genre detection based on keywords
        if (text.contains("romance") || text.contains("love")) genres.add("romance");
        if (text.contains("mystery") || text.contains("detective")) genres.add("mystery");
        if (text.contains("science") || text.contains("technology")) genres.add("science");
        if (text.contains("history") || text.contains("historical")) genres.add("history");
        if (text.contains("fantasy") || text.contains("magic")) genres.add("fantasy");
        if (text.contains("horror") || text.contains("scary")) genres.add("horror");
        if (text.contains("biography") || text.contains("memoir")) genres.add("biography");
        if (text.contains("business") || text.contains("management")) genres.add("business");
    }
    
    private Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z\\s]", "").split("\\s+");
        
        Set<String> stopWords = new HashSet<>();
        stopWords.add("the"); stopWords.add("and"); stopWords.add("or"); stopWords.add("but");
        stopWords.add("in"); stopWords.add("on"); stopWords.add("at"); stopWords.add("to");
        stopWords.add("for"); stopWords.add("of"); stopWords.add("with"); stopWords.add("by");
        stopWords.add("a"); stopWords.add("an");
        
        for (String word : words) {
            if (word.length() > 3 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
    
    private double calculateImplicitRating(Borrow borrow) {
        // Simulate rating based on borrowing patterns
        long borrowTime = borrow.getBorrowDate().getTime();
        long returnTime = borrow.getReturnDate() != null ? borrow.getReturnDate().getTime() : System.currentTimeMillis();
        long duration = returnTime - borrowTime;
        
        // Longer borrowing time suggests higher interest
        double durationScore = Math.min(5.0, 1.0 + (duration / (7 * 24 * 60 * 60 * 1000.0)) * 2); // Max 5 stars
        return Math.max(1.0, durationScore);
    }
    
    private double calculateBookRating(int bookId) {
        List<Borrow> borrows = borrowDAO.getBorrowsByBookId(bookId);
        if (borrows.isEmpty()) return 3.0;
        
        double totalRating = 0;
        for (Borrow borrow : borrows) {
            totalRating += calculateImplicitRating(borrow);
        }
        
        return totalRating / borrows.size();
    }
    
    private double calculateCosineSimilarity(UserProfile profile1, UserProfile profile2) {
        // Calculate similarity based on category preferences
        Set<String> allCategories = new HashSet<>(profile1.getCategoryPreferences().keySet());
        allCategories.addAll(profile2.getCategoryPreferences().keySet());
        
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (String category : allCategories) {
            double pref1 = profile1.getCategoryPreferences().getOrDefault(category, 0);
            double pref2 = profile2.getCategoryPreferences().getOrDefault(category, 0);
            
            dotProduct += pref1 * pref2;
            norm1 += pref1 * pref1;
            norm2 += pref2 * pref2;
        }
        
        if (norm1 == 0 || norm2 == 0) return 0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private double calculateBookSimilarity(BookFeatures features1, BookFeatures features2) {
        double similarity = 0;
        
        // Category similarity
        if (features1.getCategory() != null && features1.getCategory().equals(features2.getCategory())) {
            similarity += 0.4;
        }
        
        // Author similarity
        if (features1.getAuthor() != null && features1.getAuthor().equals(features2.getAuthor())) {
            similarity += 0.3;
        }
        
        // Keyword similarity
        Set<String> common = new HashSet<>(features1.getKeywords());
        common.retainAll(features2.getKeywords());
        
        Set<String> union = new HashSet<>(features1.getKeywords());
        union.addAll(features2.getKeywords());
        
        if (!union.isEmpty()) {
            similarity += 0.3 * (double) common.size() / union.size();
        }
        
        return similarity;
    }
    
    private double calculateContentScore(UserProfile profile, BookFeatures features) {
        double score = 0;
        
        // Category preference score
        String category = features.getCategory();
        if (category != null && profile.getCategoryPreferences().containsKey(category)) {
            double categoryPref = (double) profile.getCategoryPreferences().get(category) / profile.getTotalBorrows();
            score += categoryPref * 0.4;
        }
        
        // Author preference score
        String author = features.getAuthor();
        if (author != null && profile.getAuthorPreferences().containsKey(author)) {
            double authorPref = (double) profile.getAuthorPreferences().get(author) / profile.getTotalBorrows();
            score += authorPref * 0.3;
        }
        
        // Popularity boost
        score += features.getPopularityScore() * 0.2;
        
        // Rating boost
        score += features.getAverageRating() * 0.1;
        
        return score;
    }
    
    private Set<Integer> getBorrowedBookIds(int userId) {
        try {
            return borrowDAO.getBorrowsByUserId(userId).stream()
                .map(borrow -> borrow.getBookId())
                .collect(Collectors.toSet());
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
    
    // Inner classes for data structures
    private static class UserProfile {
        private final int userId;
        private Map<String, Integer> categoryPreferences = new HashMap<>();
        private Map<String, Integer> authorPreferences = new HashMap<>();
        private Set<String> genres = new HashSet<>();
        private double averageRating;
        private int totalBorrows;
        
        public UserProfile(int userId) {
            this.userId = userId;
        }
        
        // Getters and setters
        public int getUserId() { return userId; }
        public Map<String, Integer> getCategoryPreferences() { return categoryPreferences; }
        public void setCategoryPreferences(Map<String, Integer> categoryPreferences) { this.categoryPreferences = categoryPreferences; }
        public Map<String, Integer> getAuthorPreferences() { return authorPreferences; }
        public void setAuthorPreferences(Map<String, Integer> authorPreferences) { this.authorPreferences = authorPreferences; }
        public Set<String> getGenres() { return genres; }
        public void setGenres(Set<String> genres) { this.genres = genres; }
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
        public int getTotalBorrows() { return totalBorrows; }
        public void setTotalBorrows(int totalBorrows) { this.totalBorrows = totalBorrows; }
    }
    
    private static class BookFeatures {
        private final int bookId;
        private String category;
        private String author;
        private String title;
        private Set<String> keywords = new HashSet<>();
        private double popularityScore;
        private double averageRating;
        
        public BookFeatures(int bookId) {
            this.bookId = bookId;
        }
        
        // Getters and setters
        public int getBookId() { return bookId; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Set<String> getKeywords() { return keywords; }
        public void setKeywords(Set<String> keywords) { this.keywords = keywords; }
        public double getPopularityScore() { return popularityScore; }
        public void setPopularityScore(double popularityScore) { this.popularityScore = popularityScore; }
        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    }
}