package server;

import dao.BookDAO;
import model.Book;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Book DAO with additional methods for enterprise features
 */
public class EnhancedBookDAO extends BookDAO {
    
    private Connection getConn() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:C:/data/library.db");
    }
    
    /**
     * Get all books
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM books");
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setQuantity(rs.getInt("quantity"));
                
                // Handle optional fields
                try {
                    book.setCategory(rs.getString("category"));
                } catch (Exception e) {
                    book.setCategory("General");
                }
                
                try {
                    book.setDescription(rs.getString("description"));
                } catch (Exception e) {
                    book.setDescription("");
                }
                
                books.add(book);
            }
        } catch (Exception e) {
            System.err.println("Error getting all books: " + e.getMessage());
        }
        return books;
    }
    
    /**
     * Get book by ID
     */
    public Book getBookById(int id) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM books WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setTitle(rs.getString("title"));
                book.setAuthor(rs.getString("author"));
                book.setQuantity(rs.getInt("quantity"));
                
                try {
                    book.setCategory(rs.getString("category"));
                } catch (Exception e) {
                    book.setCategory("General");
                }
                
                try {
                    book.setDescription(rs.getString("description"));
                } catch (Exception e) {
                    book.setDescription("");
                }
                
                return book;
            }
        } catch (Exception e) {
            System.err.println("Error getting book by ID: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Add new book
     */
    public boolean addBook(Book book) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO books(title,author,quantity,category,description) VALUES(?,?,?,?,?)");
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, book.getQuantity());
            ps.setString(4, book.getCategory() != null ? book.getCategory() : "General");
            ps.setString(5, book.getDescription() != null ? book.getDescription() : "");
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error adding book: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update book
     */
    public boolean updateBook(Book book) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("UPDATE books SET title=?,author=?,quantity=?,category=?,description=? WHERE id=?");
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setInt(3, book.getQuantity());
            ps.setString(4, book.getCategory());
            ps.setString(5, book.getDescription());
            ps.setInt(6, book.getId());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error updating book: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete book
     */
    public boolean deleteBook(int id) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("DELETE FROM books WHERE id = ?");
            ps.setInt(1, id);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error deleting book: " + e.getMessage());
            return false;
        }
    }
}