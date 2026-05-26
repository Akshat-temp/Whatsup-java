package database;

import model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static boolean registerUser(String fullName, String phone, String password, String email) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("INSERT INTO users (phone, full_name, password, email) VALUES (?, ?, ?, ?)");
            ps.setString(1, phone);
            ps.setString(2, fullName);
            ps.setString(3, password);
            ps.setString(4, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[UserDAO] registerUser: " + e.getMessage());
            return false;
        }
    }

    public static User loginUser(String phone, String password) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT full_name, phone FROM users WHERE phone = ? AND password = ?");
            ps.setString(1, phone);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new User(rs.getString("full_name"), rs.getString("phone"));
        } catch (SQLException e) {
            System.err.println("[UserDAO] loginUser: " + e.getMessage());
        }
        return null;
    }

    public static User getUserByPhone(String phone) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT full_name, phone FROM users WHERE phone = ?");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new User(rs.getString("full_name"), rs.getString("phone"));
        } catch (SQLException e) {
            System.err.println("[UserDAO] getUserByPhone: " + e.getMessage());
        }
        return null;
    }

    public static boolean isPhoneRegistered(String phone) {
        return getUserByPhone(phone) != null;
    }

    public static List<User> getAllUsersExcept(String phone) {
        List<User> users = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT full_name, phone FROM users WHERE phone != ? ORDER BY full_name");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(new User(rs.getString("full_name"), rs.getString("phone")));
        } catch (SQLException e) {
            System.err.println("[UserDAO] getAllUsersExcept: " + e.getMessage());
        }
        return users;
    }

    public static boolean updateProfilePhoto(String phone, String photoPath) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("UPDATE users SET profile_photo = ? WHERE phone = ?");
            ps.setString(1, photoPath);
            ps.setString(2, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateProfilePhoto: " + e.getMessage());
            return false;
        }
    }

    public static String getProfilePhoto(String phone) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT profile_photo FROM users WHERE phone = ?");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("profile_photo");
        } catch (SQLException e) {
            System.err.println("[UserDAO] getProfilePhoto: " + e.getMessage());
        }
        return null;
    }

    public static String getEmail(String phone) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT email FROM users WHERE phone = ?");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) {
            System.err.println("[UserDAO] getEmail: " + e.getMessage());
        }
        return null;
    }

    public static boolean updateUser(String phone, String fullName, String email) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("UPDATE users SET full_name = ?, email = ? WHERE phone = ?");
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateUser: " + e.getMessage());
            return false;
        }
    }
}
