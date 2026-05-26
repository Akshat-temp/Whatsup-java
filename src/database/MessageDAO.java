package database;

import model.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("hh:mm a");

    // ── Private messages ───────────────────────────────────────────────────

    public static boolean saveMessage(String senderPhone, String receiverPhone, String content) {
        return saveMessage(senderPhone, receiverPhone, content, null, null);
    }

    public static boolean saveMessage(String senderPhone, String receiverPhone,
                                      String content, String mediaPath, String mediaType) {
        String sql = """
            INSERT INTO messages
                (sender_phone, receiver_phone, content, media_path, media_type, timestamp, is_delivered)
            VALUES (?, ?, ?, ?, ?, ?, 0)""";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, senderPhone);
            ps.setString(2, receiverPhone);
            ps.setString(3, content);
            ps.setString(4, mediaPath);
            ps.setString(5, mediaType);
            ps.setString(6, LocalDateTime.now().format(FMT));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[MessageDAO] saveMessage: " + e.getMessage());
            return false;
        }
    }

    public static List<Message> getMessages(String phone1, String phone2) {
        List<Message> list = new ArrayList<>();
        String sql = """
            SELECT sender_phone, receiver_phone, content, media_path, media_type,
                   timestamp, is_delivered
            FROM messages
            WHERE (sender_phone = ? AND receiver_phone = ?)
               OR (sender_phone = ? AND receiver_phone = ?)
            ORDER BY id""";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone1); ps.setString(2, phone2);
            ps.setString(3, phone2); ps.setString(4, phone1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message(
                        rs.getString("sender_phone"),
                        rs.getString("receiver_phone"),
                        rs.getString("content"),
                        rs.getString("timestamp"));
                m.setMediaPath(rs.getString("media_path"));
                m.setMediaType(rs.getString("media_type"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getMessages: " + e.getMessage());
        }
        return list;
    }

    public static String getLastMessage(String phone1, String phone2) {
        String sql = """
            SELECT content, media_type FROM messages
            WHERE (sender_phone = ? AND receiver_phone = ?)
               OR (sender_phone = ? AND receiver_phone = ?)
            ORDER BY id DESC LIMIT 1""";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phone1); ps.setString(2, phone2);
            ps.setString(3, phone2); ps.setString(4, phone1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String mediaType = rs.getString("media_type");
                if (mediaType != null) {
                    return mediaType.startsWith("image") ? "📷 Photo" : "🎥 Video";
                }
                return rs.getString("content");
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getLastMessage: " + e.getMessage());
        }
        return null;
    }

    /** Mark pending (undelivered) messages for receiver as delivered. */
    public static List<Message> getPendingMessages(String receiverPhone) {
        List<Message> list = new ArrayList<>();
        String sql = """
            SELECT id, sender_phone, receiver_phone, content, media_path, media_type, timestamp
            FROM messages
            WHERE receiver_phone = ? AND is_delivered = 0""";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, receiverPhone);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message(
                        rs.getString("sender_phone"),
                        rs.getString("receiver_phone"),
                        rs.getString("content"),
                        rs.getString("timestamp"));
                m.setId(rs.getInt("id"));
                m.setMediaPath(rs.getString("media_path"));
                m.setMediaType(rs.getString("media_type"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getPendingMessages: " + e.getMessage());
        }
        return list;
    }

    public static void markDelivered(int messageId) {
        String sql = "UPDATE messages SET is_delivered = 1 WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MessageDAO] markDelivered: " + e.getMessage());
        }
    }

    public static void markRead(String senderPhone, String receiverPhone) {
        String sql = "UPDATE messages SET is_read = 1 WHERE sender_phone = ? AND receiver_phone = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, senderPhone);
            ps.setString(2, receiverPhone);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MessageDAO] markRead: " + e.getMessage());
        }
    }

    // ── Group messages ─────────────────────────────────────────────────────

    public static boolean saveGroupMessage(String groupId, String senderPhone, String content) {
        return saveGroupMessage(groupId, senderPhone, content, null, null);
    }

    public static boolean saveGroupMessage(String groupId, String senderPhone,
                                           String content, String mediaPath, String mediaType) {
        String sql = """
            INSERT INTO group_messages (group_id, sender_phone, content, media_path, media_type, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)""";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(groupId));
            ps.setString(2, senderPhone);
            ps.setString(3, content);
            ps.setString(4, mediaPath);
            ps.setString(5, mediaType);
            ps.setString(6, LocalDateTime.now().format(FMT));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[MessageDAO] saveGroupMessage: " + e.getMessage());
            return false;
        }
    }

    public static List<Message> getGroupMessages(String groupId) {
        List<Message> list = new ArrayList<>();
        String sql = """
            SELECT sender_phone, content, media_path, media_type, timestamp
            FROM group_messages WHERE group_id = ? ORDER BY id""";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(groupId));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Message m = new Message(
                        rs.getString("sender_phone"),
                        Integer.parseInt(groupId),
                        rs.getString("content"),
                        rs.getString("timestamp"));
                m.setMediaPath(rs.getString("media_path"));
                m.setMediaType(rs.getString("media_type"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getGroupMessages: " + e.getMessage());
        }
        return list;
    }
}
