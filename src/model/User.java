package model;

import java.util.Date;

public class User {
    private int id;
    private String username;
    private String password;
    private String role;
    private String phone;
    private String email;
    private String avatar;
    private String fullName;
    private boolean admin;
    private Date createdAt;

    // Constructors
    public User() {}
    
    public User(int id, String username, String password, String email, String fullName, boolean admin, Date createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.admin = admin;
        this.createdAt = createdAt;
        this.role = admin ? "admin" : "user";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getPasswordHash() { return password; }
    
    public String getRole() { return role; }
    public void setRole(String role) { 
        this.role = role; 
        this.admin = "admin".equals(role);
    }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { 
        this.admin = admin;
        this.role = admin ? "admin" : "user";
    }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
