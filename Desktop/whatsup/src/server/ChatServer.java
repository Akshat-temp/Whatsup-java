package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class ChatServer {

    public static final int PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "25565"));

  
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
