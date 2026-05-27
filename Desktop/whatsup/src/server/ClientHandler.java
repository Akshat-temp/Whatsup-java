package server;

import database.DBConnection;
import database.GroupDAO;
import database.MessageDAO;
import model.Message;

import java.io.*;
import java.net.Socket;
import java.util.List;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader  in;
    private PrintWriter     out;
    private String          phone;   // set after LOGIN

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[Handler] Client disconnected: " + (phone != null ? phone : "unknown"));
        } finally {
            cleanup();
        }
    }

    private void handleLine(String line) {
        if (line.isEmpty()) return;
        String[] parts = line.split("\\|", -1);
        String cmd = parts[0].toUpperCase();

        switch (cmd) {

            case "LOGIN" -> {
                if (parts.length < 2) { send("STATUS|ERROR|Missing phone"); return; }
                this.phone = parts[1].trim();
                ChatServer.registerUser(phone, this);
                send("STATUS|OK");
                deliverPendingMessages();
            }

            case "PRIVATE" -> {
                // PRIVATE|receiverPhone|content
                if (parts.length < 3) { send("STATUS|ERROR|Bad PRIVATE format"); return; }
                String to      = parts[1];
                String content = parts[2];
                handlePrivate(to, content, null, null);
            }

            case "PRIVATE_MEDIA" -> {
                // PRIVATE_MEDIA|receiverPhone|mediaType|base64Data|caption
                if (parts.length < 5) { send("STATUS|ERROR|Bad PRIVATE_MEDIA format"); return; }
                String to        = parts[1];
                String mediaType = parts[2];
                String b64       = parts[3];
                String caption   = parts[4];
                handlePrivate(to, caption.isEmpty() ? mediaType : caption, b64, mediaType);
            }

            case "GROUP" -> {
                // GROUP|groupId|content
                if (parts.length < 3) { send("STATUS|ERROR|Bad GROUP format"); return; }
                int    groupId = Integer.parseInt(parts[1]);
                String content = parts[2];
                handleGroup(groupId, content, null, null);
            }

            case "GROUP_MEDIA" -> {
                // GROUP_MEDIA|groupId|mediaType|base64Data|caption
                if (parts.length < 5) { send("STATUS|ERROR|Bad GROUP_MEDIA format"); return; }
                int    groupId   = Integer.parseInt(parts[1]);
                String mediaType = parts[2];
                String b64       = parts[3];
                String caption   = parts[4];
                handleGroup(groupId, caption.isEmpty() ? mediaType : caption, b64, mediaType);
            }

            case "PING" -> send("PONG");

            case "LOGOUT" -> {
                cleanup();
            }

            default -> send("STATUS|ERROR|Unknown command: " + cmd);
        }
    }



    private void handlePrivate(String to, String content, String b64, String mediaType) {
        if (phone == null) { send("STATUS|ERROR|Not logged in"); return; }

     
        MessageDAO.saveMessage(phone, to, content, b64 != null ? "media:" + to : null, mediaType);

        
        ClientHandler receiver = ChatServer.getHandler(to);
        if (receiver != null) {
            if (b64 != null) {
                receiver.send("MSG_MEDIA|" + phone + "|" + mediaType + "|" + b64 + "|" + content + "|" + now());
            } else {
                receiver.send("MSG|" + phone + "|" + content + "|" + now());
            }
          
        }
  
        send("STATUS|OK");
    }

    

    private void handleGroup(int groupId, String content, String b64, String mediaType) {
        if (phone == null) { send("STATUS|ERROR|Not logged in"); return; }

        // Persist
        MessageDAO.saveGroupMessage(String.valueOf(groupId), phone, content,
                b64 != null ? "media:group:" + groupId : null, mediaType);

        // Broadcast to all online group members except sender
        List<String> members = GroupDAO.getMemberPhones(groupId);
        for (String member : members) {
            if (member.equals(phone)) continue;
            ClientHandler h = ChatServer.getHandler(member);
            if (h != null) {
                if (b64 != null) {
                    h.send("GROUP_MEDIA_MSG|" + groupId + "|" + phone + "|" + mediaType + "|" + b64 + "|" + content + "|" + now());
                } else {
                    h.send("GROUP_MSG|" + groupId + "|" + phone + "|" + content + "|" + now());
                }
            }
        }
        send("STATUS|OK");
    }


    private void deliverPendingMessages() {
        List<Message> pending = MessageDAO.getPendingMessages(phone);
        for (Message m : pending) {
            send("MSG|" + m.getSenderPhone() + "|" + m.getContent() + "|" + m.getTimestamp());
            MessageDAO.markDelivered(m.getId());   // atomic: deliver then mark
        }
        if (!pending.isEmpty())
            System.out.println("[Handler] Delivered " + pending.size() + " pending messages to " + phone);
    }


    public synchronized void send(String message) {
        if (out != null) out.println(message);
    }

    private void cleanup() {
        if (phone != null) ChatServer.unregisterUser(phone);
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private String now() {
        return java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"));
    }
}
