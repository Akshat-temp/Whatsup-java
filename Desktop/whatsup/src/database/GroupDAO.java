package database;

import model.Group;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {

    public static int createGroup(String groupName) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("INSERT INTO groups (group_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, groupName);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.err.println("[GroupDAO] createGroup: " + e.getMessage());
        }
        return -1;
    }

    public static boolean addMember(int groupId, String phone) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("INSERT INTO group_members (group_id, phone) VALUES (?, ?)");
            ps.setInt(1, groupId);
            ps.setString(2, phone);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[GroupDAO] addMember: " + e.getMessage());
            return false;
        }
    }

    public static List<Group> getGroupsOfUser(String phone) {
        List<Group> groups = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                "SELECT g.id, g.group_name FROM groups g JOIN group_members gm ON g.id = gm.group_id WHERE gm.phone = ? ORDER BY g.group_name");
            ps.setString(1, phone);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) groups.add(new Group(rs.getInt("id"), rs.getString("group_name")));
        } catch (SQLException e) {
            System.err.println("[GroupDAO] getGroupsOfUser: " + e.getMessage());
        }
        return groups;
    }

    public static List<String> getMemberPhones(int groupId) {
        List<String> phones = new ArrayList<>();
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT phone FROM group_members WHERE group_id = ?");
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) phones.add(rs.getString("phone"));
        } catch (SQLException e) {
            System.err.println("[GroupDAO] getMemberPhones: " + e.getMessage());
        }
        return phones;
    }

    public static boolean isMember(int groupId, String phone) {
        try {
            PreparedStatement ps = DBConnection.getConnection()
                .prepareStatement("SELECT 1 FROM group_members WHERE group_id = ? AND phone = ?");
            ps.setInt(1, groupId);
            ps.setString(2, phone);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[GroupDAO] isMember: " + e.getMessage());
            return false;
        }
    }
}
