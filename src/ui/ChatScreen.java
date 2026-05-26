package ui;

import database.MessageDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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

public class ChatScreen {

    private User        myUser;
    private User        contact;
    private VBox        messagesBox;
    private ScrollPane  scrollPane;
    private Timer       timer;
    private ChatClient  client;
    private int         lastMessageCount = 0;
    private Label       connectionStatus;

    public ChatScreen(User myUser, User contact) {
        this.myUser  = myUser;
        this.contact = contact;
    }

    public ChatScreen(User myUser, User contact, ChatClient existingClient) {
        this.myUser  = myUser;
        this.contact = contact;
        this.client  = existingClient;
    }

    public void show(Stage stage) {
        stage.setTitle("Whatsup - " + contact.getName());

        // ── Top bar ────────────────────────────────────────────────────────
        Label avatar = new Label(String.valueOf(contact.getName().charAt(0)).toUpperCase());
        avatar.setMinSize(38, 38);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 19;");

        Label nameLabel = new Label(contact.getName());
        nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        nameLabel.setTextFill(Color.WHITE);

        connectionStatus = new Label("● online");
        connectionStatus.setTextFill(Color.web("#25D366"));
        connectionStatus.setFont(Font.font("Arial", 11));

        Button backBtn = new Button("←");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; " +
                "-fx-font-size: 18; -fx-cursor: hand;");
        backBtn.setOnAction(e -> { stopTimer(); if (client != null) client.disconnect(); stage.close(); });

        VBox nameBox = new VBox(2, nameLabel, connectionStatus);
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

        loadChatHistory();

        // ── Input bar ──────────────────────────────────────────────────────
        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");
        inputField.setPrefHeight(42);
        inputField.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 20; -fx-padding: 8 12;");

        // Phase 3: attach image/video button
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
                // Save to DB
                MessageDAO.saveMessage(myUser.getPhone(), contact.getPhone(), text);
                // Send via server if connected
                if (client != null && client.isConnected())
                    client.sendPrivate(contact.getPhone(), text);
                addTextBubble(text, getCurrentTime(), true);
                inputField.clear();
                scrollToBottom();
                lastMessageCount++;
            }
        };

        sendBtn.setOnAction(e -> sendText.run());
        inputField.setOnAction(e -> sendText.run());

        // ── Connect to server ──────────────────────────────────────────────
        connectToServer();

        // ── Polling fallback (DB poll every 2s for messages not via socket) ─
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                List<Message> msgs = MessageDAO.getMessages(myUser.getPhone(), contact.getPhone());
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
        stage.setOnCloseRequest(e -> { stopTimer(); if (client != null) client.disconnect(); });
        stage.show();
    }

    private void connectToServer() {
        if (client != null) return;  // reuse passed-in client
        client = new ChatClient(this::handleServerMessage);
        new Thread(() -> {
            boolean ok = client.connect();
            Platform.runLater(() -> {
                if (ok) {
                    client.login(myUser.getPhone());
                    connectionStatus.setText("● online");
                    connectionStatus.setTextFill(Color.web("#25D366"));
                } else {
                    connectionStatus.setText("● offline (local only)");
                    connectionStatus.setTextFill(Color.GRAY);
                }
            });
        }).start();
    }

    private void handleServerMessage(String raw) {
        String[] parts = raw.split("\\|", -1);
        String cmd = parts[0];
        switch (cmd) {
            case "MSG" -> {
                // MSG|senderPhone|content|timestamp
                if (parts.length >= 4 && parts[1].equals(contact.getPhone())) {
                    Platform.runLater(() -> {
                        addTextBubble(parts[2], parts[3], false);
                        scrollToBottom();
                        lastMessageCount++;
                    });
                }
            }
            case "MSG_MEDIA" -> {
                // MSG_MEDIA|senderPhone|mediaType|base64Data|caption|timestamp
                if (parts.length >= 6 && parts[1].equals(contact.getPhone())) {
                    Platform.runLater(() -> {
                        addMediaBubble(parts[3], parts[2], parts[4], parts[5], false);
                        scrollToBottom();
                        lastMessageCount++;
                    });
                }
            }
            case "STATUS" -> {
                if (parts.length >= 2 && parts[1].equals("DISCONNECTED")) {
                    Platform.runLater(() -> {
                        connectionStatus.setText("● reconnecting...");
                        connectionStatus.setTextFill(Color.ORANGE);
                    });
                }
            }
        }
    }

    // ── Phase 3: Media picking ─────────────────────────────────────────────

    private void pickAndSendMedia(Stage owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Send Image or Video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.avi", "*.mkv")
        );
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        new Thread(() -> {
            try {
                byte[] bytes    = Files.readAllBytes(file.toPath());
                String b64      = Base64.getEncoder().encodeToString(bytes);
                String mimeType = file.getName().toLowerCase().endsWith(".mp4") ||
                                  file.getName().toLowerCase().endsWith(".mov") ? "video/mp4" : "image/jpeg";
                String caption  = file.getName();

                MessageDAO.saveMessage(myUser.getPhone(), contact.getPhone(), caption, b64, mimeType);
                if (client != null && client.isConnected())
                    client.sendPrivateMedia(contact.getPhone(), mimeType, b64, caption);

                Platform.runLater(() -> {
                    addMediaBubble(b64, mimeType, caption, getCurrentTime(), true);
                    scrollToBottom();
                    lastMessageCount++;
                });
            } catch (IOException e) {
                Platform.runLater(() -> showAlert("Failed to read file: " + e.getMessage()));
            }
        }).start();
    }

    // ── History & rendering ────────────────────────────────────────────────

    private void loadChatHistory() {
        List<Message> messages = MessageDAO.getMessages(myUser.getPhone(), contact.getPhone());
        lastMessageCount = messages.size();
        renderMessages(messages);
    }

    private void renderMessages(List<Message> messages) {
        messagesBox.getChildren().clear();
        for (Message m : messages) {
            boolean isMine = m.getSenderPhone().equals(myUser.getPhone());
            if (m.hasMedia() && m.getMediaPath() != null && m.getMediaPath().startsWith("media:")) {
                addMediaBubble(m.getMediaPath(), m.getMediaType(), m.getContent(), m.getTimestamp(), isMine);
            } else {
                addTextBubble(m.getContent(), m.getTimestamp(), isMine);
            }
        }
        scrollToBottom();
    }

    private void addTextBubble(String text, String time, boolean isMine) {
        Label msgLabel = new Label(text);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(260);
        msgLabel.setTextFill(Color.WHITE);
        msgLabel.setFont(Font.font("Arial", 13));

        Label timeLabel = new Label(time + (isMine ? " ✓✓" : ""));
        timeLabel.setFont(Font.font("Arial", 9));
        timeLabel.setTextFill(Color.web(isMine ? "#53BDEB" : "#AAAAAA"));

        VBox bubble = new VBox(4, msgLabel, timeLabel);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(280);
        wrapAndAdd(bubble, isMine);
    }

    private void addMediaBubble(String b64OrPath, String mimeType, String caption,
                                 String time, boolean isMine) {
        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(280);

        if (mimeType != null && mimeType.startsWith("image")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(b64OrPath);
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                        new java.io.ByteArrayInputStream(bytes));
                ImageView iv = new ImageView(img);
                iv.setFitWidth(240);
                iv.setPreserveRatio(true);
                bubble.getChildren().add(iv);
            } catch (Exception e) {
                bubble.getChildren().add(new Label("📷 " + caption));
            }
        } else {
            Label icon = new Label("🎥 " + caption);
            icon.setTextFill(Color.WHITE);
            bubble.getChildren().add(icon);
        }

        Label timeLabel = new Label(time + (isMine ? " ✓✓" : ""));
        timeLabel.setFont(Font.font("Arial", 9));
        timeLabel.setTextFill(Color.web(isMine ? "#53BDEB" : "#AAAAAA"));
        bubble.getChildren().add(timeLabel);

        wrapAndAdd(bubble, isMine);
    }

    private void wrapAndAdd(VBox bubble, boolean isMine) {
        if (isMine) {
            bubble.setStyle("-fx-background-color: #005C4B; -fx-background-radius: 12 12 0 12;");
            HBox row = new HBox(bubble);
            row.setAlignment(Pos.CENTER_RIGHT);
            row.setPadding(new Insets(2, 8, 2, 50));
            messagesBox.getChildren().add(row);
        } else {
            bubble.setStyle("-fx-background-color: #1F2C34; -fx-background-radius: 12 12 12 0;");
            HBox row = new HBox(bubble);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 50, 2, 8));
            messagesBox.getChildren().add(row);
        }
    }

    private void scrollToBottom() {
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    private String getCurrentTime() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    private void stopTimer() {
        if (timer != null) timer.cancel();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }
}
