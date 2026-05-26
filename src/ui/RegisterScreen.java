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

    private TextField nameField, phoneField, emailField, otpField;
    private Button    sendOtpBtn, verifyBtn;

    public void show(Stage stage) {
        stage.setTitle("Whatsup - Register");

        Text title = new Text("Whatsup");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.web("#25D366"));

        Text subtitle = new Text("Create your account");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setFill(Color.GRAY);

        // ── Name field ──────────────────────────────────────────────────────
        nameField = new TextField();
        nameField.setPromptText("Your Name");
        nameField.setPrefHeight(42);
        styleField(nameField);

        // ── Phone field ─────────────────────────────────────────────────────
        phoneField = new TextField();
        phoneField.setPromptText("Phone Number (digits only, e.g. 9876543210)");
        phoneField.setPrefHeight(42);
        styleField(phoneField);

        // ── Email field (NEW) ───────────────────────────────────────────────
        emailField = new TextField();
        emailField.setPromptText("Email Address (OTP will be sent here)");
        emailField.setPrefHeight(42);
        styleField(emailField);

        // ── Status / error label ────────────────────────────────────────────
        Label statusLabel = new Label("");
        statusLabel.setFont(Font.font("Arial", 12));
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(300);
        statusLabel.setVisible(false);

        // ── Send OTP button ─────────────────────────────────────────────────
        sendOtpBtn = new Button("Send OTP to Email");
        sendOtpBtn.setPrefWidth(300);
        sendOtpBtn.setPrefHeight(42);
        styleButton(sendOtpBtn, "#25D366");

        // ── OTP field (hidden until OTP is sent) ────────────────────────────
        otpField = new TextField();
        otpField.setPromptText("Enter 6-digit OTP from your email");
        otpField.setPrefHeight(42);
        otpField.setVisible(false);
        styleField(otpField);

        // ── Verify & Register button (hidden until OTP sent) ────────────────
        verifyBtn = new Button("Verify OTP & Register");
        verifyBtn.setPrefWidth(300);
        verifyBtn.setPrefHeight(42);
        verifyBtn.setVisible(false);
        styleButton(verifyBtn, "#128C7E");

        // ── Login link ──────────────────────────────────────────────────────
        Label loginLabel = new Label("Already have an account? Login");
        loginLabel.setTextFill(Color.web("#25D366"));
        loginLabel.setStyle("-fx-cursor: hand;");

        // ── Send OTP action ─────────────────────────────────────────────────
        sendOtpBtn.setOnAction(e -> {
            String name  = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();

            // Validate name
            if (name.isEmpty()) {
                showError(statusLabel, "Please enter your name.");
                return;
            }

            // Validate phone
            if (phone.isEmpty()) {
                showError(statusLabel, "Please enter your phone number.");
                return;
            }
            if (!phone.matches("\\d{7,15}")) {
                showError(statusLabel, "Phone must be 7–15 digits only (no spaces or dashes).");
                return;
            }

            // Validate email
            if (email.isEmpty()) {
                showError(statusLabel, "Please enter your email address.");
                return;
            }
            if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
                showError(statusLabel, "Please enter a valid email address.");
                return;
            }

            // Check if phone already registered
            if (UserDAO.isPhoneRegistered(phone)) {
                showError(statusLabel, "This phone number is already registered. Please login.");
                return;
            }

            // Disable button and show sending state
            sendOtpBtn.setDisable(true);
            sendOtpBtn.setText("Sending OTP...");
            showInfo(statusLabel, "Sending OTP to " + email + " ...");

            // Send OTP in background thread (so UI doesn't freeze)
            new Thread(() -> {
                boolean sent = OTPService.generateAndSendOTP(phone, email);
                javafx.application.Platform.runLater(() -> {
                    sendOtpBtn.setDisable(false);
                    sendOtpBtn.setText("Resend OTP");
                    if (sent) {
                        otpField.setVisible(true);
                        verifyBtn.setVisible(true);
                        showInfo(statusLabel, "✅ OTP sent to " + email + " — check your inbox (and spam folder).");
                    } else {
                        showError(statusLabel, "❌ Failed to send email. Check your internet or the sender Gmail config in OTPService.java.");
                    }
                });
            }).start();
        });

        // ── Verify OTP action ────────────────────────────────────────────────
        verifyBtn.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            String name  = nameField.getText().trim();
            String otp   = otpField.getText().trim();

            if (otp.isEmpty()) {
                showError(statusLabel, "Please enter the OTP from your email.");
                return;
            }
            if (!otp.matches("\\d{6}")) {
                showError(statusLabel, "OTP must be exactly 6 digits.");
                return;
            }

            if (OTPService.verifyOTP(phone, otp)) {
                boolean success = UserDAO.registerUser(name, phone);
                if (success) {
                    OTPService.clearOTP(phone);
                    showInfo(statusLabel, "Registration successful! Redirecting to login...");
                    // Small delay so user can see success message
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
                showError(statusLabel, "❌ Invalid OTP. Please check your email and try again.");
                otpField.clear();
            }
        });

        // ── Login link action ────────────────────────────────────────────────
        loginLabel.setOnMouseClicked(e -> {
            stage.close();
            new LoginScreen().show(new Stage());
        });

        // ── Layout ───────────────────────────────────────────────────────────
        VBox layout = new VBox(12,
                title, subtitle,
                nameField, phoneField, emailField,
                sendOtpBtn,
                otpField, verifyBtn,
                statusLabel,
                loginLabel);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.setStyle("-fx-background-color: #111B21;");
        layout.setMaxWidth(360);

        StackPane root = new StackPane(layout);
        root.setStyle("-fx-background-color: #111B21;");

        stage.setScene(new Scene(root, 420, 600));
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

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color: #2A3942; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: gray; -fx-background-radius: 8; -fx-padding: 8;");
        f.setPrefWidth(300);
    }

    private void styleButton(Button b, String color) {
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}
