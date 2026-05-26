package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static Connection connection = null;

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:whatsup.db");
                Statement st = connection.createStatement();
                st.execute("PRAGMA journal_mode=DELETE");
                st.execute("PRAGMA synchronous=FULL");
                st.close();
                System.out.println("[DB] Connected to SQLite");
                createTables();
            }
            return connection;
        } catch (Exception e) {
            System.err.println("[DB] Connection error: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    private static void createTables() {
        try {
            Statement st = connection.createStatement();
            st.execute("CREATE TABLE IF NOT EXISTS users (phone TEXT PRIMARY KEY, full_name TEXT NOT NULL, password TEXT, email TEXT, profile_photo TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, sender_phone TEXT, receiver_phone TEXT, content TEXT, media_path TEXT, media_type TEXT, timestamp TEXT, is_delivered INTEGER DEFAULT 0, is_read INTEGER DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS groups (id INTEGER PRIMARY KEY AUTOINCREMENT, group_name TEXT NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS group_members (group_id INTEGER, phone TEXT, PRIMARY KEY(group_id, phone))");
            st.execute("CREATE TABLE IF NOT EXISTS group_messages (id INTEGER PRIMARY KEY AUTOINCREMENT, group_id INTEGER, sender_phone TEXT, content TEXT, media_path TEXT, media_type TEXT, timestamp TEXT)");
            st.close();
            System.out.println("[DB] Schema ready");
        } catch (SQLException e) {
            System.err.println("[DB] Schema error: " + e.getMessage());
        }
    }

    public static void initSchema() {
        getConnection();
    }

    private DBConnection() {}
}
