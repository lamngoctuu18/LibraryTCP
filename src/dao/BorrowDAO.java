package dao;

import java.util.*;
import java.sql.*;
import model.Borrow;

public class BorrowDAO {
    private Connection getConn() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:C:/data/library.db");
    }
    
    public List<Borrow> getBorrowsByUserId(int userId) {
        List<Borrow> borrows = new ArrayList<>();
        try (Connection conn = getConn()) {
            String sql = "SELECT * FROM borrow_records WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Borrow borrow = new Borrow();
                        borrow.setId(rs.getInt("id"));
                        borrow.setUserId(rs.getInt("user_id"));
                        borrow.setBookId(rs.getInt("book_id"));
                        borrows.add(borrow);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting borrows by user ID: " + e.getMessage());
        }
        return borrows;
    }
    
    public List<Borrow> getBorrowsByBookId(int bookId) {
        List<Borrow> borrows = new ArrayList<>();
        try (Connection conn = getConn()) {
            String sql = "SELECT * FROM borrow_records WHERE book_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Borrow borrow = new Borrow();
                        borrow.setId(rs.getInt("id"));
                        borrow.setUserId(rs.getInt("user_id"));
                        borrow.setBookId(rs.getInt("book_id"));
                        borrows.add(borrow);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting borrows by book ID: " + e.getMessage());
        }
        return borrows;
    }
    
    public int getBorrowCountByBookId(int bookId) {
        try (Connection conn = getConn()) {
            String sql = "SELECT COUNT(*) FROM borrow_records WHERE book_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, bookId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting borrow count: " + e.getMessage());
        }
        return 0;
    }
}
