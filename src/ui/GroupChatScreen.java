package ui;

import database.GroupDAO;
import database.MessageDAO;
import database.UserDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Message;
import model.User;
import server.ChatClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class GroupChatScreen {

    private User       currentUser;
    private String     groupId;
    private String     groupName;
    private VBox       messagesBox;
    private ScrollPane scrollPane;
    private Timer      timer;
    private ChatClient client;
    private int        lastMessageCount = 0;

    public GroupChatScreen(User user, String groupId, String groupName) {
        this.currentUser = user;
        this.groupId     = groupId;
        this.groupName   = groupName;
    }

    public GroupChatScreen(User user, String groupId, String groupName, ChatClient existingClient) {
        this(user, groupId, groupName);
        this.client = existingClient;
    }

    public void show(Stage stage) {
        stage.setTitle("Whatsup - " + groupName);

        // ── Top bar ────────────────────────────────────────────────────────
        Label avatar = new Label(groupName.substring(0, 1).toUpperCase());
        avatar.setMinSize(38, 38);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 19;");

        Label nameLabel = new Label(groupName);
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        nameLabel.setTextFill(Color.WHITE);

        // Show member count
        List<String> members = GroupDAO.getMemberPhones(Integer.parseInt(groupId));
        Label membersLabel = new Label(members.size() + " members");
        membersLabel.setTextFill(Color.GRAY);
        membersLabel.setFont(Font.font("Arial", 11));

        Button backBtn = new Button("←");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; " +
                "-fx-font-size: 18; -fx-cursor: hand;");
        backBtn.setOnAction(e -> { stopTimer(); stage.close(); });

        VBox nameBox = new VBox(2, nameLabel, membersLabel);
        HBox topBar  = new HBox(10, backBtn, avatar, nameBox);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: #1F2C34;");

        // ── Messages area ──────────────────────────────────────────────────
        messagesBox = new VBox(8);
        messagesBox.setPadding(new Insets(12));
        messagesBox.setStyle("-fx-background-color: #0B141A;");

        scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #0B141A; -fx-background: #0B141A;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── Input bar ──────────────────────────────────────────────────────
        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");
        inputField.setPrefHeight(42);
        inputField.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 20; -fx-padding: 8 12;");

        Button attachBtn = new Button("📎");
        attachBtn.setPrefSize(42, 42);
        attachBtn.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-font-size: 16; -fx-background-radius: 21; -fx-cursor: hand;");
        attachBtn.setOnAction(e -> pickAndSendMedia(stage));

        Button sendBtn = new Button("➤");
        sendBtn.setPrefSize(42, 42);
        sendBtn.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-size: 16; -fx-background-radius: 21; -fx-cursor: hand;");

        HBox inputBar = new HBox(8, attachBtn, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputBar.setPadding(new Insets(10, 12, 10, 12));
        inputBar.setAlignment(Pos.CENTER);
        inputBar.setStyle("-fx-background-color: #1F2C34;");

        Runnable sendText = () -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                MessageDAO.saveGroupMessage(groupId, currentUser.getPhone(), text);
                if (client != null && client.isConnected())
                    client.sendGroup(groupId, text);
                addBubble(currentUser.getName(), text, getCurrentTime(), true, null, null);
                inputField.clear();
                scrollToBottom();
                lastMessageCount++;
            }
        };

        sendBtn.setOnAction(e -> sendText.run());
        inputField.setOnAction(e -> sendText.run());

        loadMessages();
        connectToServer();

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                List<Message> msgs = MessageDAO.getGroupMessages(groupId);
                if (msgs.size() != lastMessageCount) {
                    Platform.runLater(() -> {
                        lastMessageCount = msgs.size();
                        renderMessages(msgs);
                    });
                }
            }
        }, 2000, 2000);

        VBox root = new VBox(topBar, scrollPane, inputBar);
        root.setStyle("-fx-background-color: #0B141A;");

        stage.setScene(new Scene(root, 420, 620));
        stage.setOnCloseRequest(e -> stopTimer());
        stage.show();
    }

    private void connectToServer() {
        if (client != null) return;
        client = new ChatClient(this::handleServerMessage);
        new Thread(() -> { if (client.connect()) client.login(currentUser.getPhone()); }).start();
    }

    private void handleServerMessage(String raw) {
        String[] parts = raw.split("\\|", -1);
        if ("GROUP_MSG".equals(parts[0]) && parts.length >= 5 && parts[1].equals(groupId)) {
            String sender  = parts[2];
            String content = parts[3];
            String time    = parts[4];
            String name    = resolveDisplayName(sender);
            Platform.runLater(() -> {
                addBubble(name, content, time, false, null, null);
                scrollToBottom();
                lastMessageCount++;
            });
        } else if ("GROUP_MEDIA_MSG".equals(parts[0]) && parts.length >= 7 && parts[1].equals(groupId)) {
            String sender    = parts[2];
            String mediaType = parts[3];
            String b64       = parts[4];
            String caption   = parts[5];
            String time      = parts[6];
            String name      = resolveDisplayName(sender);
            Platform.runLater(() -> {
                addBubble(name, caption, time, false, b64, mediaType);
                scrollToBottom();
                lastMessageCount++;
            });
        }
    }

    private String resolveDisplayName(String phone) {
        User u = UserDAO.getUserByPhone(phone);
        return u != null ? u.getName() : phone;
    }

    private void pickAndSendMedia(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Send Image or Video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.avi")
        );
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        new Thread(() -> {
            try {
                byte[] bytes    = Files.readAllBytes(file.toPath());
                String b64      = Base64.getEncoder().encodeToString(bytes);
                String mimeType = file.getName().toLowerCase().endsWith(".mp4") ? "video/mp4" : "image/jpeg";
                String caption  = file.getName();

                MessageDAO.saveGroupMessage(groupId, currentUser.getPhone(), caption, b64, mimeType);
                if (client != null && client.isConnected())
                    client.sendGroupMedia(groupId, mimeType, b64, caption);

                Platform.runLater(() -> {
                    addBubble(currentUser.getName(), caption, getCurrentTime(), true, b64, mimeType);
                    scrollToBottom();
                    lastMessageCount++;
                });
            } catch (IOException e) {
                System.err.println("Media send error: " + e.getMessage());
            }
        }).start();
    }

    private void loadMessages() {
        List<Message> messages = MessageDAO.getGroupMessages(groupId);
        lastMessageCount = messages.size();
        renderMessages(messages);
    }

    private void renderMessages(List<Message> messages) {
        messagesBox.getChildren().clear();
        for (Message m : messages) {
            boolean isMine = m.getSenderPhone().equals(currentUser.getPhone());
            String name = isMine ? currentUser.getName() : resolveDisplayName(m.getSenderPhone());
            addBubble(name, m.getContent(), m.getTimestamp(), isMine, m.getMediaPath(), m.getMediaType());
        }
        scrollToBottom();
    }

    private void addBubble(String sender, String text, String time,
                            boolean isMine, String b64, String mimeType) {
        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(260);
        msgLabel.setTextFill(Color.WHITE);
        msgLabel.setFont(Font.font("Arial", 13));

        Label timeLabel = new Label(time + (isMine ? " ✓✓" : ""));
        timeLabel.setFont(Font.font("Arial", 9));
        timeLabel.setTextFill(Color.web(isMine ? "#53BDEB" : "#AAAAAA"));

        VBox content;
        if (!isMine) {
            Label senderLabel = new Label(sender);
            senderLabel.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            senderLabel.setTextFill(Color.web("#25D366"));
            content = new VBox(3, senderLabel);
        } else {
            content = new VBox(3);
        }

        // Media rendering
        if (b64 != null && mimeType != null && mimeType.startsWith("image")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(b64);
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        new java.io.ByteArrayInputStream(bytes));
                ImageView iv = new ImageView(img);
                iv.setFitWidth(220);
                iv.setPreserveRatio(true);
                content.getChildren().add(iv);
            } catch (Exception ignored) {
                content.getChildren().add(new Label("📷 " + text));
            }
        } else if (b64 != null && mimeType != null && mimeType.startsWith("video")) {
            content.getChildren().add(new Label("🎥 " + text));
        } else {
            content.getChildren().add(msgLabel);
        }

        content.getChildren().add(timeLabel);
        content.setPadding(new Insets(8, 12, 8, 12));
        content.setMaxWidth(280);

        if (isMine) {
            content.setStyle("-fx-background-color: #005C4B; -fx-background-radius: 12 12 0 12;");
            HBox row = new HBox(content);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(2, 8, 2, 50));
            messagesBox.getChildren().add(row);
        } else {
            content.setStyle("-fx-background-color: #1F2C34; -fx-background-radius: 12 12 12 0;");
            HBox row = new HBox(content);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 50, 2, 8));
            messagesBox.getChildren().add(row);
        }
    }

    private void scrollToBottom() { scrollPane.layout(); scrollPane.setVvalue(1.0); }
    private String getCurrentTime() { return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")); }
    private void stopTimer() { if (timer != null) timer.cancel(); }
}
