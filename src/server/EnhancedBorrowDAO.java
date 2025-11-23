package server;

import dao.BorrowDAO;
import model.Borrow;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enhanced Borrow DAO with additional methods for enterprise features
 */
public class EnhancedBorrowDAO extends BorrowDAO {
    
    private Connection getConn() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:C:/data/library.db");
    }
    
    /**
     * Get borrows by user ID
     */
    public List<Borrow> getBorrowsByUserId(int userId) {
        List<Borrow> borrows = new ArrayList<>();
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM borrow_records WHERE user_id = ?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Borrow borrow = new Borrow();
                borrow.setId(rs.getInt("id"));
                borrow.setUserId(rs.getInt("user_id"));
                borrow.setBookId(rs.getInt("book_id"));
                borrow.setBorrowDate(new Date(rs.getLong("borrow_date")));
                
                long returnDateLong = rs.getLong("return_date");
                if (returnDateLong > 0) {
                    borrow.setReturnDate(new Date(returnDateLong));
                }
                
                borrows.add(borrow);
            }
        } catch (Exception e) {
            System.err.println("Error getting borrows by user ID: " + e.getMessage());
        }
        return borrows;
    }
    
    /**
     * Get borrows by book ID
     */
    public List<Borrow> getBorrowsByBookId(int bookId) {
        List<Borrow> borrows = new ArrayList<>();
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("SELECT * FROM borrow_records WHERE book_id = ?");
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Borrow borrow = new Borrow();
                borrow.setId(rs.getInt("id"));
                borrow.setUserId(rs.getInt("user_id"));
                borrow.setBookId(rs.getInt("book_id"));
                borrow.setBorrowDate(new Date(rs.getLong("borrow_date")));
                
                long returnDateLong = rs.getLong("return_date");
                if (returnDateLong > 0) {
                    borrow.setReturnDate(new Date(returnDateLong));
                }
                
                borrows.add(borrow);
            }
        } catch (Exception e) {
            System.err.println("Error getting borrows by book ID: " + e.getMessage());
        }
        return borrows;
    }
    
    /**
     * Get borrow count by book ID
     */
    public int getBorrowCountByBookId(int bookId) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM borrow_records WHERE book_id = ?");
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("Error getting borrow count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Add new borrow record
     */
    public boolean addBorrow(Borrow borrow) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO borrow_records(user_id,book_id,borrow_date) VALUES(?,?,?)");
            ps.setInt(1, borrow.getUserId());
            ps.setInt(2, borrow.getBookId());
            ps.setLong(3, borrow.getBorrowDate().getTime());
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error adding borrow record: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Update return date
     */
    public boolean returnBook(int borrowId) {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("UPDATE borrow_records SET return_date = ? WHERE id = ?");
            ps.setLong(1, System.currentTimeMillis());
            ps.setInt(2, borrowId);
            
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error returning book: " + e.getMessage());
            return false;
        }
    }
}