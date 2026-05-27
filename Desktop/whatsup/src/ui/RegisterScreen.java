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

public class RegisterScreen {

    public void show(Stage stage) {
        stage.setTitle("Whatsup - Register");

        Text title = new Text("Whatsup");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.web("#25D366"));

        Text subtitle = new Text("from gehu");
        subtitle.setFont(Font.font("Arial", 12));
        subtitle.setFill(Color.GRAY);

        TextField nameField = new TextField();
        nameField.setPromptText("Your Name");
        nameField.setPrefHeight(42);
        styleField(nameField);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number (digits only)");
        phoneField.setPrefHeight(42);
        styleField(phoneField);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Create Password (min 6 chars)");
        passwordField.setPrefHeight(42);
        styleField(passwordField);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm Password");
        confirmField.setPrefHeight(42);
        styleField(confirmField);

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(300);
        statusLabel.setVisible(false);

        Button registerBtn = new Button("Register");
        registerBtn.setPrefWidth(300);
        registerBtn.setPrefHeight(42);
        styleButton(registerBtn);

        Label loginLabel = new Label("Already have an account? Login");
        loginLabel.setTextFill(Color.web("#25D366"));
        loginLabel.setStyle("-fx-cursor: hand;");

        registerBtn.setOnAction(e -> {
            String name     = nameField.getText().trim();
            String phone    = phoneField.getText().trim();
            String password = passwordField.getText().trim();
            String confirm  = confirmField.getText().trim();

            if (name.isEmpty()) { showError(statusLabel, "Please enter your name."); return; }
            if (phone.isEmpty()) { showError(statusLabel, "Please enter your phone number."); return; }
            if (!phone.matches("\\d{7,15}")) { showError(statusLabel, "Phone must be digits only."); return; }
            if (password.isEmpty()) { showError(statusLabel, "Please create a password."); return; }
            if (password.length() < 6) { showError(statusLabel, "Password must be at least 6 characters."); return; }
            if (!password.equals(confirm)) { showError(statusLabel, "Passwords do not match."); return; }
            if (UserDAO.isPhoneRegistered(phone)) { showError(statusLabel, "Phone already registered. Please login."); return; }

            boolean success = UserDAO.registerUser(name, phone, password, null);
            if (success) {
                showInfo(statusLabel, "✅ Registered successfully! Please login.");
                new Thread(() -> {
                    try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> {
                        stage.close();
                        new LoginScreen().show(new Stage());
                    });
                }).start();
            } else {
                showError(statusLabel, "Registration failed. Try again.");
            }
        });

        loginLabel.setOnMouseClicked(e -> {
            stage.close();
            new LoginScreen().show(new Stage());
        });

        VBox layout = new VBox(12, title, subtitle, nameField, phoneField, passwordField, confirmField, registerBtn, statusLabel, loginLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.setStyle("-fx-background-color: #111B21;");

        StackPane root = new StackPane(layout);
        root.setStyle("-fx-background-color: #111B21;");

        stage.setScene(new Scene(root, 420, 560));
        stage.setResizable(false);
        stage.show();
    }

    private void showError(Label label, String msg) {
        label.setTextFill(Color.web("#FF6B6B"));
        label.setText(msg);
        label.setVisible(true);
    }

    private void showInfo(Label label, String msg) {
        label.setTextFill(Color.web("#25D366"));
        label.setText(msg);
        label.setVisible(true);
    }

    private void styleField(Control f) {
        f.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 8; -fx-padding: 8;");
        f.setPrefWidth(300);
    }

    private void styleButton(Button b) {
        b.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}