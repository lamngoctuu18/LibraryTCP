package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import model.User;

public class UserDAO {
    private Connection getConn() throws Exception {

        return DriverManager.getConnection("jdbc:sqlite:C:/data/library.db");
    }

    public int createUser(String username, String password, String role, String phone, String email, String avatar) throws Exception {
        try (Connection c = getConn()) {
            PreparedStatement ps = c.prepareStatement("INSERT INTO users(username,password,role,phone,email,avatar) VALUES(?,?,?,?,?,?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, phone);
            ps.setString(5, email);
            ps.setString(6, avatar);
            return ps.executeUpdate();
        }
    }

    public int createUser(String username, String password, String role, String phone, String email) throws Exception {
        return createUser(username, password, role, phone, email, "");
    }

    public ResultSet findUser(String username, String password) throws Exception {
        Connection c = getConn();
        PreparedStatement ps = c.prepareStatement("SELECT id, role FROM users WHERE username=? AND password=?");
        ps.setString(1, username); ps.setString(2, password);
        return ps.executeQuery();
    }
    
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection conn = getConn()) {
            String sql = "SELECT * FROM users";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    users.add(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
}
