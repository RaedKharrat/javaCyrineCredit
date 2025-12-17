package tn.esprit.controllers;

import tn.esprit.entities.User;
import tn.esprit.enums.UserRole;
import tn.esprit.services.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    private final AuthService authService = new AuthService();
    private static User loggedInUser; // Store logged in user

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs!");
            return;
        }

        try {
            // Authenticate user
            User user = authService.login(username, password);

            if (user != null) {
                loggedInUser = user; // Store the logged in user
                System.out.println("✅ Login successful! User: " + user.getUsername() + " Role: " + user.getRole());

                // Navigate based on role
                navigateBasedOnRole(user);

            } else {
                showError("Nom d'utilisateur ou mot de passe incorrect!");
            }

        } catch (Exception e) {
            showError("Erreur de connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            // Load register screen
            Parent root = FXMLLoader.load(getClass().getResource("/Register.fxml"));
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Inscription - Travel Agency");

        } catch (IOException e) {
            showError("Erreur lors du chargement de la page d'inscription");
            e.printStackTrace();
        }
    }

    private void navigateBasedOnRole(User user) {
        try {
            String fxmlFile = "";
            String title = "";

            switch (user.getRole()) {
                case CLIENT:
                    fxmlFile = "/ClientDashboard.fxml";
                    title = "Tableau de bord - Client";
                    break;
                case AGENT:
                    fxmlFile = "/AgentDashboard.fxml";
                    title = "Tableau de bord - Agent";
                    break;
                case ADMIN:
                    fxmlFile = "/AdminDashboard.fxml";
                    title = "Tableau de bord - Admin";
                    break;
            }

            // Load the appropriate dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Pass the logged in user to the dashboard controller
            Object controller = loader.getController();
            if (controller instanceof DashboardInterface) {
                ((DashboardInterface) controller).setLoggedInUser(user);
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.centerOnScreen();

        } catch (IOException e) {
            showError("Erreur de navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    // Static getter for logged in user
    public static User getLoggedInUser() {
        return loggedInUser;
    }

    // Static setter (for logout)
    public static void setLoggedInUser(User user) {
        loggedInUser = user;
    }
}

// Interface for passing user to dashboards
interface DashboardInterface {
    void setLoggedInUser(User user);
}