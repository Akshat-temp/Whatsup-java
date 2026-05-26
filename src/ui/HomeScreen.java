package ui;

import database.GroupDAO;
import database.MessageDAO;
import database.UserDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import model.Group;
import model.User;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeScreen {

    private User  currentUser;
    private VBox  contactList;
    private Timer timer;

    public HomeScreen(User user) {
        this.currentUser = user;
    }

    public void show(Stage stage) {
        stage.setTitle("Whatsup - " + currentUser.getName());

        Label appName = new Label("Whatsup");
        appName.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        appName.setTextFill(Color.WHITE);

        Label userInfo = new Label("\uD83D\uDC64 " + currentUser.getName());
        userInfo.setTextFill(Color.web("#25D366"));

        Button newChatBtn = new Button("+ New Chat");
        styleButton(newChatBtn);
        newChatBtn.setOnAction(e -> new NewChatScreen(currentUser).show(new Stage()));

        Button newGroupBtn = new Button("+ Group");
        styleButton(newGroupBtn);
        newGroupBtn.setOnAction(e -> openCreateGroupDialog(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, appName, spacer, userInfo, newGroupBtn, newChatBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));
        topBar.setStyle("-fx-background-color: #1F2C34;");

        TextField searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D Search contacts...");
        searchField.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 20; -fx-padding: 8 12;");
        HBox searchBar = new HBox(searchField);
        searchBar.setPadding(new Insets(8, 12, 8, 12));
        searchBar.setStyle("-fx-background-color: #111B21;");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        contactList = new VBox(2);
        contactList.setStyle("-fx-background-color: #111B21;");
        ScrollPane scrollPane = new ScrollPane(contactList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #111B21; -fx-background: #111B21;");

        loadContacts(stage, "");

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Platform.runLater(() -> loadContacts(stage, searchField.getText()));
            }
        }, 3000, 3000);

        searchField.textProperty().addListener((obs, old, val) -> loadContacts(stage, val));

        VBox root = new VBox(topBar, searchBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.setStyle("-fx-background-color: #111B21;");

        stage.setScene(new Scene(root, 380, 620));
        stage.setOnCloseRequest(e -> { timer.cancel(); Platform.exit(); });
        stage.show();
    }

    private void loadContacts(Stage stage, String query) {
        contactList.getChildren().clear();

        List<Group> groups = GroupDAO.getGroupsOfUser(currentUser.getPhone());
        for (Group g : groups)
            if (g.getGroupName().toLowerCase().contains(query.toLowerCase()))
                contactList.getChildren().add(createGroupCard(g));

        List<User> users = UserDAO.getAllUsersExcept(currentUser.getPhone());
        for (User u : users)
            if (u.getName().toLowerCase().contains(query.toLowerCase()))
                contactList.getChildren().add(createContactCard(u));
    }

    private HBox createGroupCard(Group group) {
        Label avatar = new Label(group.getGroupName().substring(0, 1).toUpperCase());
        avatar.setMinSize(46, 46);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #128C7E; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 18; -fx-background-radius: 23;");

        Label name = new Label(group.getGroupName());
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Label preview = new Label("Group Chat");
        preview.setTextFill(Color.GRAY);
        preview.setFont(Font.font("Arial", 12));

        VBox info = new VBox(3, name, preview);
        HBox card = new HBox(12, avatar, info);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #111B21; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1F2C34; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #111B21; -fx-cursor: hand;"));
        card.setOnMouseClicked(e ->
            new GroupChatScreen(currentUser, String.valueOf(group.getId()), group.getGroupName()).show(new Stage())
        );
        return card;
    }

    private HBox createContactCard(User contact) {
        Label avatar = new Label(String.valueOf(contact.getName().charAt(0)).toUpperCase());
        avatar.setMinSize(46, 46);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 18; -fx-background-radius: 23;");

        Label name = new Label(contact.getName());
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        String lastMsg = MessageDAO.getLastMessage(currentUser.getPhone(), contact.getPhone());
        Label preview = new Label(lastMsg != null ? lastMsg : "Tap to chat");
        preview.setTextFill(Color.GRAY);
        preview.setFont(Font.font("Arial", 12));
        preview.setMaxWidth(220);

        VBox info = new VBox(3, name, preview);
        HBox card = new HBox(12, avatar, info);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #111B21; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1F2C34; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #111B21; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> new ChatScreen(currentUser, contact).show(new Stage()));
        return card;
    }

    private void openCreateGroupDialog(Stage owner) {
        Stage dialog = new Stage();
        dialog.setTitle("Create New Group");
        dialog.initOwner(owner);

        TextField groupNameField = new TextField();
        groupNameField.setPromptText("Group name");
        groupNameField.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 8; -fx-padding: 8;");

        Label membersLabel = new Label("Select members:");
        membersLabel.setTextFill(Color.WHITE);
        membersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        VBox checkboxList = new VBox(6);
        checkboxList.setStyle("-fx-background-color: #111B21;");
        List<User> users = UserDAO.getAllUsersExcept(currentUser.getPhone());
        for (User u : users) {
            CheckBox cb = new CheckBox(u.getName() + " (" + u.getPhone() + ")");
            cb.setStyle("-fx-text-fill: white;");
            cb.setUserData(u.getPhone());
            checkboxList.getChildren().add(cb);
        }

        ScrollPane scroll = new ScrollPane(checkboxList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        scroll.setStyle("-fx-background: #111B21; -fx-background-color: #111B21;");

        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#FF6B6B"));

        Button createBtn = new Button("Create Group");
        styleButton(createBtn);
        createBtn.setOnAction(e -> {
            String gName = groupNameField.getText().trim();
            if (gName.isEmpty()) { errorLabel.setText("Enter a group name."); return; }

            java.util.List<String> selected = new java.util.ArrayList<>();
            selected.add(currentUser.getPhone());
            for (javafx.scene.Node node : checkboxList.getChildren()) {
                if (node instanceof CheckBox cb && cb.isSelected())
                    selected.add((String) cb.getUserData());
            }
            if (selected.size() < 2) { errorLabel.setText("Select at least one member."); return; }

            int groupId = GroupDAO.createGroup(gName);
            if (groupId == -1) { errorLabel.setText("Failed to create group."); return; }
            for (String phone : selected) GroupDAO.addMember(groupId, phone);

            dialog.close();
            loadContacts(owner, "");
        });

        VBox root = new VBox(12, groupNameField, membersLabel, scroll, errorLabel, createBtn);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #111B21;");

        dialog.setScene(new Scene(root, 340, 400));
        dialog.show();
    }

    private void styleButton(Button b) {
        b.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}
