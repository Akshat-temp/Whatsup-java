package server;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * ChatClient — manages the persistent TCP connection from the client side.
 *
 * Usage:
 *   ChatClient client = new ChatClient("192.168.1.5", 25565, this::onMessage);
 *   client.connect();
 *   client.login("9876543210");
 *   client.sendPrivate("1234567890", "Hello!");
 *   client.disconnect();
 *
 * The messageListener callback is called on the background thread —
 * always wrap UI updates in Platform.runLater().
 *
 * SERVER_HOST env var overrides the hard-coded host (useful for Railway/Render).
 */
public class ChatClient {

    // ── Connection config ──────────────────────────────────────────────────
    // Override SERVER_HOST env var for deployed server (e.g. your Railway URL)
    private static final String DEFAULT_HOST =
            System.getenv().getOrDefault("SERVER_HOST", "localhost");
    private static final int DEFAULT_PORT =
            Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "25565"));

    private final String   host;
    private final int      port;
    private final Consumer<String> messageListener;

    private Socket       socket;
    private PrintWriter  out;
    private Thread       listenerThread;
    private boolean      connected = false;

    public ChatClient(Consumer<String> messageListener) {
        this(DEFAULT_HOST, DEFAULT_PORT, messageListener);
    }

    public ChatClient(String host, int port, Consumer<String> messageListener) {
        this.host            = host;
        this.port            = port;
        this.messageListener = messageListener;
    }

    /** Connect and start background listener. Returns true if connected. */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            connected = true;
            startListener();
            System.out.println("[Client] Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Cannot connect to server: " + e.getMessage());
            return false;
        }
    }

    private void startListener() {
        listenerThread = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    messageListener.accept(msg);
                }
            } catch (IOException e) {
                if (connected)
                    System.err.println("[Client] Connection lost: " + e.getMessage());
            } finally {
                connected = false;
                messageListener.accept("STATUS|DISCONNECTED");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // ── Send helpers ───────────────────────────────────────────────────────

    public void login(String phone) {
        send("LOGIN|" + phone);
    }

    public void sendPrivate(String receiverPhone, String content) {
        send("PRIVATE|" + receiverPhone + "|" + content);
    }

    public void sendPrivateMedia(String receiverPhone, String mediaType, String base64Data, String caption) {
        send("PRIVATE_MEDIA|" + receiverPhone + "|" + mediaType + "|" + base64Data + "|" + caption);
    }

    public void sendGroup(String groupId, String content) {
        send("GROUP|" + groupId + "|" + content);
    }

    public void sendGroupMedia(String groupId, String mediaType, String base64Data, String caption) {
        send("GROUP_MEDIA|" + groupId + "|" + mediaType + "|" + base64Data + "|" + caption);
    }

    public void ping() { send("PING"); }

    public void disconnect() {
        connected = false;
        send("LOGOUT");
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }

    private synchronized void send(String msg) {
        if (out != null) out.println(msg);
    }
}
