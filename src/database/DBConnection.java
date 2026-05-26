package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database connection.
 *
 * LOCAL  → SQLite  (default, zero config)
 * CLOUD  → PostgreSQL on Render/Railway
 *
 * Set env var DATABASE_URL to a jdbc:postgresql://... string to switch.
 * Example (Render internal URL):
 *   DATABASE_URL=jdbc:postgresql://dpg-xxx:5432/whatsup?user=whatsup&password=secret
 */
public class DBConnection {

    private static Connection connection = null;

    public static Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) return connection;

            String dbUrl = System.getenv("DATABASE_URL");

            if (dbUrl != null && !dbUrl.isBlank()) {
                // ── PostgreSQL (Render / Railway) ──────────────────────────
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(dbUrl);
                System.out.println("[DB] Connected to PostgreSQL");
            } else {
                // ── SQLite (local development) ─────────────────────────────
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:whatsup.db");
                // WAL mode reduces lock contention under concurrent writes
                connection.createStatement().execute("PRAGMA journal_mode=WAL;");
                System.out.println("[DB] Connected to SQLite (local)");
            }

            return connection;
        } catch (Exception e) {
            System.err.println("[DB] Connection error: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    /** Call once at startup to create tables if they don't exist. */
    public static void initSchema() {
        try (var conn = getConnection();
             var st   = conn.createStatement()) {

            // Users
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    phone TEXT PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    email TEXT,
                    profile_photo TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

            // Private messages
            st.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_phone TEXT NOT NULL,
                    receiver_phone TEXT NOT NULL,
                    content TEXT NOT NULL,
                    media_path TEXT,
                    media_type TEXT,
                    timestamp TEXT NOT NULL,
                    is_delivered INTEGER DEFAULT 0,
                    is_read INTEGER DEFAULT 0
                )""");

            // Groups
            st.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_name TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

            // Group members
            st.execute("""
                CREATE TABLE IF NOT EXISTS group_members (
                    group_id INTEGER NOT NULL,
                    phone TEXT NOT NULL,
                    PRIMARY KEY (group_id, phone)
                )""");

            // Group messages
            st.execute("""
                CREATE TABLE IF NOT EXISTS group_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    sender_phone TEXT NOT NULL,
                    content TEXT NOT NULL,
                    media_path TEXT,
                    media_type TEXT,
                    timestamp TEXT NOT NULL
                )""");

            System.out.println("[DB] Schema ready");

        } catch (SQLException e) {
            System.err.println("[DB] Schema init error: " + e.getMessage());
        }
    }

    private DBConnection() {}
}
