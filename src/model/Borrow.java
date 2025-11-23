package model;

import java.util.Date;

public class Borrow {
    private int id;
    private int userId;
    private int bookId;
    private Date borrowDate;
    private Date returnDate;

    // Constructors
    public Borrow() {}
    
    public Borrow(int id, int userId, int bookId, Date borrowDate, Date returnDate) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }
    
    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }
    
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
    
    // Backward compatibility methods
    public String getBorrowDateString() { 
        return borrowDate != null ? borrowDate.toString() : null; 
    }
    
    public void setBorrowDate(String borrowDate) { 
        // For backward compatibility - convert string to Date if needed
        this.borrowDate = borrowDate != null ? new Date() : null;
    }
    
    public String getReturnDateString() { 
        return returnDate != null ? returnDate.toString() : null; 
    }
    
    public void setReturnDate(String returnDate) { 
        // For backward compatibility - convert string to Date if needed  
        this.returnDate = returnDate != null ? new Date() : null;
    }
}
