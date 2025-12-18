package tn.esprit.controllers;

import tn.esprit.entities.Demande;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.User;
import tn.esprit.enums.ReservationStatus;
import tn.esprit.services.ReservationService;
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
import java.util.ResourceBundle;

public class CreateReservationController implements Initializable {

    @FXML private Label demandeTitle;
    @FXML private Label demandeDestination;
    @FXML private Label demandeBudget;
    @FXML private Label demandeAgent;

    @FXML private TextField totalPriceField;
    @FXML private Label priceError;

    @FXML private TextArea notesArea;
    @FXML private Label messageLabel;

    @FXML private Button createButton;
    @FXML private Button cancelButton;

    private Demande selectedDemande;
    private User currentClient;
    private final ReservationService reservationService = new ReservationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Get demande from previous screen
        selectedDemande = getSelectedDemandeFromMemory();
        currentClient = LoginController.getLoggedInUser();

        if (selectedDemande != null) {
            updateDemandeInfo();
        }

        // Set default price as demande budget
        if (selectedDemande != null) {
            totalPriceField.setText(String.format("%.2f", selectedDemande.getBudget()));
        }
    }

    private Demande getSelectedDemandeFromMemory() {
        // In real app, pass as parameter
        // For demo, create a mock
        if (currentClient != null) {
            try {
                tn.esprit.services.DemandeService demandeService = new tn.esprit.services.DemandeService();
                var demandes = demandeService.getDemandesByClient(currentClient.getId());
                if (!demandes.isEmpty()) {
                    return demandes.get(0); // Return first for demo
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void updateDemandeInfo() {
        if (selectedDemande != null) {
            demandeTitle.setText(selectedDemande.getTitle());
            demandeDestination.setText(selectedDemande.getDestination());
            demandeBudget.setText(String.format("%.2f DT", selectedDemande.getBudget()));

            User agent = selectedDemande.getAssignedAgent();
            demandeAgent.setText(agent != null ? agent.getUsername() : "Non assigné");
        }
    }

    @FXML
    private void handleCreate(ActionEvent event) {
        priceError.setVisible(false);
        messageLabel.setVisible(false);

        // Validate price
        double totalPrice = 0;
        try {
            totalPrice = Double.parseDouble(totalPriceField.getText().trim());
            if (totalPrice <= 0) {
                priceError.setText("Le prix doit être supérieur à 0");
                priceError.setVisible(true);
                return;
            }
        } catch (NumberFormatException e) {
            priceError.setText("Veuillez entrer un nombre valide");
            priceError.setVisible(true);
            return;
        }

        // Check if demande already has a reservation
        if (reservationService.demandeHasReservation(selectedDemande.getId())) {
            showMessage("❌ Cette demande a déjà une réservation", "red");
            return;
        }

        // Create reservation
        try {
            Reservation reservation = new Reservation(
                    selectedDemande,
                    currentClient,
                    selectedDemande.getAssignedAgent(),
                    totalPrice
            );

            reservation.setStatus(ReservationStatus.PENDING);
            reservationService.add(reservation);

            showMessage("✅ Réservation créée avec succès! Code: " + reservation.getReservationCode(), "green");

            createButton.setDisable(true);

            // Return to dashboard after 3 seconds
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
                    3000
            );

        } catch (Exception e) {
            showMessage("❌ Erreur lors de la création: " + e.getMessage(), "red");
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

    public void setSelectedDemande(Demande demande) {
        this.selectedDemande = demande;
        updateDemandeInfo();
    }

    public void setCurrentClient(User client) {
        this.currentClient = client;
    }
}