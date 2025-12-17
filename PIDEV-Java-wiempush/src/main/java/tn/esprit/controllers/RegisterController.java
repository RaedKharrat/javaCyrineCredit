package tn.esprit.controllers;

import tn.esprit.entities.User;
import tn.esprit.enums.UserRole;
import tn.esprit.services.AuthService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private ComboBox<UserRole> roleComboBox;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Button registerButton;
    @FXML private Button backButton;

    private final AuthService authService = new AuthService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup role combo box
        roleComboBox.setItems(FXCollections.observableArrayList(
                UserRole.CLIENT,
                UserRole.AGENT
        ));
        roleComboBox.getSelectionModel().selectFirst(); // Default to CLIENT

        // Clear messages
        messageLabel.setVisible(false);
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        // Get form data
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        UserRole role = roleComboBox.getValue();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Veuillez remplir tous les champs!", "red");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Les mots de passe ne correspondent pas!", "red");
            return;
        }

        if (password.length() < 6) {
            showMessage("Le mot de passe doit contenir au moins 6 caractères!", "red");
            return;
        }

        // Check if username/email already exists
        if (authService.usernameExists(username)) {
            showMessage("Ce nom d'utilisateur est déjà utilisé!", "red");
            return;
        }

        if (authService.emailExists(email)) {
            showMessage("Cet email est déjà utilisé!", "red");
            return;
        }

        try {
            // Create new user
            User newUser = new User(username, email, password, role);

            // Register user
            boolean success = authService.register(newUser);

            if (success) {
                showMessage("✅ Inscription réussie! Vous pouvez maintenant vous connecter.", "green");

                // Clear form
                clearForm();

                // Auto-navigate to login after 2 seconds
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                javafx.application.Platform.runLater(() -> {
                                    try {
                                        handleBack(null);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        },
                        2000
                );

            } else {
                showMessage("❌ Erreur lors de l'inscription. Veuillez réessayer.", "red");
            }

        } catch (Exception e) {
            showMessage("Erreur: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Go back to login
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Travel Agency");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setVisible(true);
    }

    private void clearForm() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleComboBox.getSelectionModel().selectFirst();
    }
}