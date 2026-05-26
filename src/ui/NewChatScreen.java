package ui;

import database.UserDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import model.User;

import java.util.List;

public class NewChatScreen {

    private User currentUser;

    public NewChatScreen(User user) {
        this.currentUser = user;
    }

    public void show(Stage stage) {
        stage.setTitle("New Chat");

        Label title = new Label("Select Contact");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        TextField searchField = new TextField();
        searchField.setPromptText("\uD83D\uDD0D Search by name...");
        searchField.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 20; -fx-padding: 8;");

        VBox contactList = new VBox(2);
        contactList.setStyle("-fx-background-color: #111B21;");

        List<User> users = UserDAO.getAllUsersExcept(currentUser.getPhone());

        Runnable loadList = () -> {
            contactList.getChildren().clear();
            String query = searchField.getText().toLowerCase();
            for (User u : users)
                if (u.getName().toLowerCase().contains(query))
                    contactList.getChildren().add(createCard(u, stage));
        };

        loadList.run();
        searchField.textProperty().addListener((obs, o, n) -> loadList.run());

        ScrollPane scroll = new ScrollPane(contactList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #111B21; -fx-background-color: #111B21;");

        VBox root = new VBox(12, title, searchField, scroll);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #111B21;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        stage.setScene(new Scene(root, 360, 500));
        stage.show();
    }

    private HBox createCard(User u, Stage stage) {
        Label avatar = new Label(String.valueOf(u.getName().charAt(0)).toUpperCase());
        avatar.setMinSize(42, 42);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 16; -fx-background-radius: 21;");

        Label name = new Label(u.getName());
        name.setTextFill(Color.WHITE);
        name.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Label phone = new Label(u.getPhone());
        phone.setTextFill(Color.GRAY);

        VBox info = new VBox(3, name, phone);
        HBox card = new HBox(12, avatar, info);
        card.setPadding(new Insets(10, 16, 10, 16));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #111B21; -fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1F2C34; -fx-cursor: hand;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color: #111B21; -fx-cursor: hand;"));
        card.setOnMouseClicked(e -> {
            stage.close();
            new ChatScreen(currentUser, u).show(new Stage());
        });
        return card;
    }
}
