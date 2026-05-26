package database;

import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static boolean registerUser(String fullName, String phone) {
        String sql = "INSERT INTO users (phone, full_name) VALUES (?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, fullName);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[UserDAO] registerUser: " + e.getMessage());
            return false;
        }
    }

    public static boolean registerUser(String fullName, String phone, String email) {
        String sql = "INSERT INTO users (phone, full_name, email) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[UserDAO] registerUser: " + e.getMessage());
            return false;
        }
    }

    public static User getUserByPhone(String phone) {
        String sql = "SELECT full_name, phone FROM users WHERE phone = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
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
        String sql = "SELECT full_name, phone FROM users WHERE phone != ? ORDER BY full_name";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) users.add(new User(rs.getString("full_name"), rs.getString("phone")));
        } catch (SQLException e) {
            System.err.println("[UserDAO] getAllUsersExcept: " + e.getMessage());
        }
        return users;
    }

    public static boolean updateProfilePhoto(String phone, String photoPath) {
        String sql = "UPDATE users SET profile_photo = ? WHERE phone = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, photoPath);
            ps.setString(2, phone);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] updateProfilePhoto: " + e.getMessage());
            return false;
        }
    }

    public static String getProfilePhoto(String phone) {
        String sql = "SELECT profile_photo FROM users WHERE phone = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("profile_photo");
        } catch (SQLException e) {
            System.err.println("[UserDAO] getProfilePhoto: " + e.getMessage());
        }
        return null;
    }
}
