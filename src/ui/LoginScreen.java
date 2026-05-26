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

public class LoginScreen {

    public void show(Stage stage) {
        stage.setTitle("Whatsup - Login");

        Text title = new Text("Whatsup");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.web("#25D366"));

        Text subtitle = new Text("The better WhatsApp \uD83D\uDE04");
        subtitle.setFont(Font.font("Arial", 13));
        subtitle.setFill(Color.GRAY);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        phoneField.setPrefHeight(42);
        styleField(phoneField);

        Label errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#FF6B6B"));
        errorLabel.setFont(Font.font("Arial", 12));
        errorLabel.setVisible(false);

        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(300);
        loginBtn.setPrefHeight(42);
        styleButton(loginBtn);

        Label registerLabel = new Label("New user? Register here");
        registerLabel.setTextFill(Color.web("#25D366"));
        registerLabel.setStyle("-fx-cursor: hand;");

        loginBtn.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            if (phone.isEmpty()) {
                showError(errorLabel, "Please enter your phone number.");
                return;
            }
            if (!phone.matches("\\d{7,15}")) {
                showError(errorLabel, "Enter a valid phone number (digits only).");
                return;
            }
            User user = UserDAO.getUserByPhone(phone);
            if (user != null) {
                errorLabel.setVisible(false);
                stage.close();
                new HomeScreen(user).show(new Stage());
            } else {
                showError(errorLabel, "Phone not registered. Please sign up.");
            }
        });

        phoneField.setOnAction(e -> loginBtn.fire());

        registerLabel.setOnMouseClicked(e -> {
            stage.close();
            new RegisterScreen().show(new Stage());
        });

        VBox layout = new VBox(14, title, subtitle, phoneField, errorLabel, loginBtn, registerLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50));
        layout.setStyle("-fx-background-color: #111B21;");

        StackPane root = new StackPane(layout);
        root.setStyle("-fx-background-color: #111B21;");

        stage.setScene(new Scene(root, 420, 440));
        stage.setResizable(false);
        stage.show();
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
    }

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 8; -fx-padding: 8;");
        f.setPrefWidth(300);
    }

    private void styleButton(Button b) {
        b.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}
