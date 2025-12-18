package tn.esprit.controllers;

import tn.esprit.entities.Demande;
import tn.esprit.entities.User;
import tn.esprit.enums.DemandeStatus;
import tn.esprit.services.DemandeService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class CreateDemandeController implements Initializable {

    @FXML private Label agentLabel;
    @FXML private Label selectedAgentName;
    @FXML private Label selectedAgentEmail;

    @FXML private TextField titleField;
    @FXML private Label titleError;

    @FXML private TextField destinationField;
    @FXML private Label destinationError;

    @FXML private TextField budgetField;
    @FXML private Label budgetError;

    @FXML private DatePicker startDatePicker;
    @FXML private Label startDateError;

    @FXML private DatePicker endDatePicker;
    @FXML private Label endDateError;

    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionError;

    @FXML private Label messageLabel;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private User selectedAgent;
    private User currentClient;
    private final DemandeService demandeService = new DemandeService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Get logged in client
        currentClient = LoginController.getLoggedInUser();

        // Get selected agent from previous screen
        // We'll need to pass this somehow - for now, we'll use a static variable
        selectedAgent = getSelectedAgentFromMemory();

        if (selectedAgent != null) {
            updateAgentInfo();
        }

        // Set default dates (today and 7 days from now)
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(7));

        // Clear error labels
        clearErrors();
    }

    private User getSelectedAgentFromMemory() {
        // In a real app, you'd pass this as a parameter
        // For now, we'll simulate it
        try {
            tn.esprit.services.UserService userService = new tn.esprit.services.UserService();
            var agents = userService.getAgents();
            if (!agents.isEmpty()) {
                return agents.get(0); // Return first agent for demo
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateAgentInfo() {
        if (selectedAgent != null) {
            selectedAgentName.setText("Agent: " + selectedAgent.getUsername());
            selectedAgentEmail.setText("Email: " + selectedAgent.getEmail());
            agentLabel.setText("Demande pour: " + selectedAgent.getUsername());
        }
    }

    private void clearErrors() {
        titleError.setVisible(false);
        destinationError.setVisible(false);
        budgetError.setVisible(false);
        startDateError.setVisible(false);
        endDateError.setVisible(false);
        descriptionError.setVisible(false);
        messageLabel.setVisible(false);
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        clearErrors();

        // Validate inputs
        boolean isValid = true;

        // Title validation
        String title = titleField.getText().trim();
        if (title.isEmpty() || title.length() < 5) {
            titleError.setText("Le titre doit contenir au moins 5 caractères");
            titleError.setVisible(true);
            isValid = false;
        }

        // Destination validation
        String destination = destinationField.getText().trim();
        if (destination.isEmpty()) {
            destinationError.setText("La destination est obligatoire");
            destinationError.setVisible(true);
            isValid = false;
        }

        // Budget validation
        double budget = 0;
        try {
            budget = Double.parseDouble(budgetField.getText().trim());
            if (budget <= 0) {
                budgetError.setText("Le budget doit être supérieur à 0");
                budgetError.setVisible(true);
                isValid = false;
            }
        } catch (NumberFormatException e) {
            budgetError.setText("Veuillez entrer un nombre valide");
            budgetError.setVisible(true);
            isValid = false;
        }

        // Date validation
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null) {
            startDateError.setText("La date de début est obligatoire");
            startDateError.setVisible(true);
            isValid = false;
        } else if (startDate.isBefore(LocalDate.now())) {
            startDateError.setText("La date de début ne peut pas être dans le passé");
            startDateError.setVisible(true);
            isValid = false;
        }

        if (endDate == null) {
            endDateError.setText("La date de fin est obligatoire");
            endDateError.setVisible(true);
            isValid = false;
        } else if (endDate.isBefore(startDate)) {
            endDateError.setText("La date de fin doit être après la date de début");
            endDateError.setVisible(true);
            isValid = false;
        }

        // Description validation
        String description = descriptionArea.getText().trim();
        if (description.isEmpty() || description.length() < 20) {
            descriptionError.setText("La description doit contenir au moins 20 caractères");
            descriptionError.setVisible(true);
            isValid = false;
        }

        if (!isValid) {
            showMessage("Veuillez corriger les erreurs dans le formulaire", "red");
            return;
        }

        // Create and save demande
        try {
            Demande demande = new Demande(
                    title,
                    description,
                    destination,
                    budget,
                    startDate,
                    endDate,
                    currentClient
            );

            demande.setAssignedAgent(selectedAgent);
            demande.setStatus(DemandeStatus.PENDING);

            demandeService.add(demande);

            showMessage("✅ Demande créée avec succès! Vous serez redirigé vers votre tableau de bord...", "green");

            // Disable submit button
            submitButton.setDisable(true);

            // Return to client dashboard after 2 seconds
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    returnToDashboard();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    },
                    2000
            );

        } catch (Exception e) {
            showMessage("❌ Erreur lors de la création de la demande: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        returnToDashboard();
    }

    private void returnToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientDashboard.fxml"));
            Parent root = loader.load();

            // Pass the logged in user to dashboard
            ClientDashboardController controller = loader.getController();
            controller.setLoggedInUser(currentClient);

            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Tableau de bord - Client");

        } catch (IOException e) {
            showMessage("Erreur de navigation: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setVisible(true);
    }

    // Method to set selected agent (called from ClientDashboard)
    public void setSelectedAgent(User agent) {
        this.selectedAgent = agent;
        updateAgentInfo();
    }

    // Method to set current client
    public void setCurrentClient(User client) {
        this.currentClient = client;
    }
}