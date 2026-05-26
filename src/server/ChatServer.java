package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatServer — multi-threaded TCP server.
 *
 * Listens on PORT (default 25565, override with SERVER_PORT env var).
 * Spawns a ClientHandler thread per connection.
 *
 * onlineUsers: phone → ClientHandler (used to route messages in real-time).
 *
 * Run this on your server/Railway/Render instance BEFORE launching clients.
 * Usage: java -cp ... server.ChatServer
 */
public class ChatServer {

    public static final int PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "25565"));

    // phone → handler (thread-safe map for concurrent client access)
    static final ConcurrentHashMap<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("[Server] Starting Whatsup server on port " + PORT + " ...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Listening. Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New connection: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    // ── Routing helpers called by ClientHandler ────────────────────────────

    static void registerUser(String phone, ClientHandler handler) {
        onlineUsers.put(phone, handler);
        System.out.println("[Server] " + phone + " is now online. Online: " + onlineUsers.size());
    }

    static void unregisterUser(String phone) {
        onlineUsers.remove(phone);
        System.out.println("[Server] " + phone + " went offline. Online: " + onlineUsers.size());
    }

    static ClientHandler getHandler(String phone) {
        return onlineUsers.get(phone);
    }

    static boolean isOnline(String phone) {
        return onlineUsers.containsKey(phone);
    }
}
