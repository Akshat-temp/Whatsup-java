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
import util.OTPService;

public class RegisterScreen {

    private TextField     nameField, phoneField, emailField, otpField;
    private PasswordField passwordField;
    private Button        sendOtpBtn, verifyBtn;

    public void show(Stage stage) {
        stage.setTitle("Whatsup - Register");

        Text title = new Text("Whatsup");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.web("#25D366"));

        nameField = new TextField();
        nameField.setPromptText("Your Name");
        nameField.setPrefHeight(42);
        styleField(nameField);

        phoneField = new TextField();
        phoneField.setPromptText("Phone Number (digits only)");
        phoneField.setPrefHeight(42);
        styleField(phoneField);

        passwordField = new PasswordField();
        passwordField.setPromptText("Create Password");
        passwordField.setPrefHeight(42);
        styleField(passwordField);

        emailField = new TextField();
        emailField.setPromptText("Email Address (OTP will be sent here)");
        emailField.setPrefHeight(42);
        styleField(emailField);

        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(300);
        statusLabel.setVisible(false);

        sendOtpBtn = new Button("Send OTP to Email");
        sendOtpBtn.setPrefWidth(300);
        sendOtpBtn.setPrefHeight(42);
        styleButton(sendOtpBtn, "#25D366");

        otpField = new TextField();
        otpField.setPromptText("Enter 6-digit OTP from your email");
        otpField.setPrefHeight(42);
        otpField.setVisible(false);
        styleField(otpField);

        verifyBtn = new Button("Verify OTP & Register");
        verifyBtn.setPrefWidth(300);
        verifyBtn.setPrefHeight(42);
        verifyBtn.setVisible(false);
        styleButton(verifyBtn, "#128C7E");

        Label loginLabel = new Label("Already have an account? Login");
        loginLabel.setTextFill(Color.web("#25D366"));
        loginLabel.setStyle("-fx-cursor: hand;");

        sendOtpBtn.setOnAction(e -> {
            String name     = nameField.getText().trim();
            String phone    = phoneField.getText().trim();
            String password = passwordField.getText().trim();
            String email    = emailField.getText().trim();

            if (name.isEmpty()) { showError(statusLabel, "Please enter your name."); return; }
            if (phone.isEmpty()) { showError(statusLabel, "Please enter your phone number."); return; }
            if (!phone.matches("\\d{7,15}")) { showError(statusLabel, "Phone must be 7–15 digits only."); return; }
            if (password.isEmpty()) { showError(statusLabel, "Please create a password."); return; }
            if (password.length() < 6) { showError(statusLabel, "Password must be at least 6 characters."); return; }
            if (email.isEmpty()) { showError(statusLabel, "Please enter your email address."); return; }
            if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                showError(statusLabel, "Please enter a valid email address."); return;
            }
            if (UserDAO.isPhoneRegistered(phone)) {
                showError(statusLabel, "This phone number is already registered. Please login."); return;
            }

            sendOtpBtn.setDisable(true);
            sendOtpBtn.setText("Sending OTP...");
            showInfo(statusLabel, "Sending OTP to " + email + " ...");

            new Thread(() -> {
                boolean sent = OTPService.generateAndSendOTP(phone, email);
                javafx.application.Platform.runLater(() -> {
                    sendOtpBtn.setDisable(false);
                    sendOtpBtn.setText("Resend OTP");
                    if (sent) {
                        otpField.setVisible(true);
                        verifyBtn.setVisible(true);
                        showInfo(statusLabel, "✅ OTP sent to " + email + " — check inbox and spam.");
                    } else {
                        showError(statusLabel, "❌ Failed to send OTP. Check terminal for the code.");
                    }
                });
            }).start();
        });

        verifyBtn.setOnAction(e -> {
            String phone    = phoneField.getText().trim();
            String name     = nameField.getText().trim();
            String password = passwordField.getText().trim();
            String email    = emailField.getText().trim();
            String otp      = otpField.getText().trim();

            if (otp.isEmpty()) { showError(statusLabel, "Please enter the OTP."); return; }
            if (!otp.matches("\\d{6}")) { showError(statusLabel, "OTP must be exactly 6 digits."); return; }

            if (OTPService.verifyOTP(phone, otp)) {
                boolean success = UserDAO.registerUser(name, phone, password, email);
                if (success) {
                    OTPService.clearOTP(phone);
                    showInfo(statusLabel, "✅ Registration successful! Redirecting to login...");
                    new Thread(() -> {
                        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                        javafx.application.Platform.runLater(() -> {
                            stage.close();
                            new LoginScreen().show(new Stage());
                        });
                    }).start();
                } else {
                    showError(statusLabel, "Phone already registered. Please login.");
                }
            } else {
                showError(statusLabel, "❌ Invalid OTP. Try again.");
                otpField.clear();
            }
        });

        loginLabel.setOnMouseClicked(e -> {
            stage.close();
            new LoginScreen().show(new Stage());
        });

        VBox layout = new VBox(12,
                title,
                nameField, phoneField, passwordField, emailField,
                sendOtpBtn,
                otpField, verifyBtn,
                statusLabel,
                loginLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));
        layout.setStyle("-fx-background-color: #111B21;");
        layout.setMaxWidth(360);

        StackPane root = new StackPane(layout);
        root.setStyle("-fx-background-color: #111B21;");

        stage.setScene(new Scene(root, 420, 620));
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

    private void styleButton(Button b, String color) {
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}
