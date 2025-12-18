package tn.esprit.controllers;

import tn.esprit.entities.Demande;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.User;
import tn.esprit.enums.DemandeStatus;
import tn.esprit.enums.ReservationStatus;
import tn.esprit.services.DemandeService;
import tn.esprit.services.ReservationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AgentDashboardController implements Initializable, DashboardInterface {

    // Services
    private final DemandeService demandeService = new DemandeService();
    private final ReservationService reservationService = new ReservationService();

    // Logged in user
    private User currentAgent;

    // FXML Components
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;

    // Tab 1: New Demandes
    @FXML private Label pendingCount;
    @FXML private Label inProgressCount;
    @FXML private Label validatedCount;
    @FXML private TableView<Demande> newDemandesTable;
    @FXML private TableColumn<Demande, String> colNewTitle;
    @FXML private TableColumn<Demande, String> colNewClient;
    @FXML private TableColumn<Demande, String> colNewDestination;
    @FXML private TableColumn<Demande, String> colNewBudget;
    @FXML private TableColumn<Demande, String> colNewDates;
    @FXML private TableColumn<Demande, String> colNewStatus;
    @FXML private TableColumn<Demande, Void> colNewActions;
    @FXML private Label noNewDemandesLabel;

    // Tab 2: My Demandes
    @FXML private TextField searchDemandeField;
    @FXML private TableView<Demande> myDemandesTable;
    @FXML private TableColumn<Demande, String> colMyTitle;
    @FXML private TableColumn<Demande, String> colMyClient;
    @FXML private TableColumn<Demande, String> colMyDestination;
    @FXML private TableColumn<Demande, String> colMyBudget;
    @FXML private TableColumn<Demande, String> colMyStatus;
    @FXML private TableColumn<Demande, Void> colMyActions;

    // Tab 3: Reservations
    @FXML private Label totalReservations;
    @FXML private Label confirmedReservations;
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> colResCode;
    @FXML private TableColumn<Reservation, String> colResClient;
    @FXML private TableColumn<Reservation, String> colResDemande;
    @FXML private TableColumn<Reservation, String> colResPrice;
    @FXML private TableColumn<Reservation, String> colResStatus;
    @FXML private TableColumn<Reservation, String> colResDate;
    @FXML private TableColumn<Reservation, Void> colResActions;

    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Will be called after setLoggedInUser
    }

    @Override
    public void setLoggedInUser(User user) {
        this.currentAgent = user;
        updateUI();
        loadData();
    }

    private void updateUI() {
        welcomeLabel.setText("👨‍💼 Agent: " + currentAgent.getUsername());
    }

    private void loadData() {
        try {
            loadNewDemandes();
            loadMyDemandes();
            loadReservations();
            updateStats();
            statusLabel.setText("✅ Données chargées avec succès");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("❌ Erreur de chargement");
        }
    }

    private void updateStats() throws SQLException {
        // Update demande counts
        pendingCount.setText(String.valueOf(demandeService.countByStatus(DemandeStatus.PENDING)));
        inProgressCount.setText(String.valueOf(demandeService.countByStatus(DemandeStatus.IN_PROGRESS)));
        validatedCount.setText(String.valueOf(demandeService.countByStatus(DemandeStatus.VALIDATED)));

        // Update reservation counts
        List<Reservation> allReservations = reservationService.getReservationsByAgent(currentAgent.getId());
        totalReservations.setText(String.valueOf(allReservations.size()));

        long confirmedCount = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.COMPLETED)
                .count();
        confirmedReservations.setText(String.valueOf(confirmedCount));
    }

    // ========== TAB 1: NEW DEMANDES ==========
    private void loadNewDemandes() throws SQLException {
        List<Demande> pendingDemandes = demandeService.getPendingDemandes();

        if (pendingDemandes.isEmpty()) {
            noNewDemandesLabel.setVisible(true);
            newDemandesTable.setVisible(false);
            return;
        }

        noNewDemandesLabel.setVisible(false);
        newDemandesTable.setVisible(true);

        // Configure columns
        colNewTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colNewClient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClient().getUsername()));
        colNewDestination.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDestination()));
        colNewBudget.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f DT", data.getValue().getBudget())));
        colNewDates.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStartDate() + " → " + data.getValue().getEndDate()
        ));
        colNewStatus.setCellValueFactory(data -> new SimpleStringProperty(getDemandeStatusLabel(data.getValue().getStatus())));

        // Actions column - Accept/Reject buttons
        colNewActions.setCellFactory(param -> new TableCell<>() {
            private final Button acceptButton = new Button("✅ Accepter");
            private final Button rejectButton = new Button("❌ Rejeter");
            private final Button viewButton = new Button("👁️ Voir");

            {
                acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                rejectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

                acceptButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    acceptDemande(demande);
                });

                rejectButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    rejectDemande(demande);
                });

                viewButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    viewDemandeDetails(demande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewButton, acceptButton, rejectButton);
                    setGraphic(buttons);
                }
            }
        });

        newDemandesTable.setItems(FXCollections.observableArrayList(pendingDemandes));
    }

    private void acceptDemande(Demande demande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accepter la demande");
        confirm.setHeaderText("Confirmer l'acceptation");
        confirm.setContentText("Voulez-vous accepter cette demande?\n" +
                "Titre: " + demande.getTitle() + "\n" +
                "Client: " + demande.getClient().getUsername());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Assign demande to agent and set status to IN_PROGRESS
                    boolean success = demandeService.assignToAgent(demande.getId(), currentAgent.getId());

                    if (success) {
                        showAlert("Succès", "Demande acceptée avec succès!");
                        loadData(); // Refresh all data
                        statusLabel.setText("✅ Demande acceptée");
                    } else {
                        showAlert("Erreur", "Erreur lors de l'acceptation de la demande");
                    }
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void rejectDemande(Demande demande) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rejeter la demande");
        dialog.setHeaderText("Raison du rejet");
        dialog.setContentText("Veuillez indiquer la raison:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reason -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmer le rejet");
            confirm.setHeaderText("Rejeter la demande");
            confirm.setContentText("Raison: " + reason + "\n\nConfirmer le rejet?");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        demande.setStatus(DemandeStatus.REJECTED);
                        demandeService.update(demande);

                        showAlert("Succès", "Demande rejetée avec succès!");
                        loadData(); // Refresh
                        statusLabel.setText("❌ Demande rejetée");
                    } catch (Exception e) {
                        showAlert("Erreur", "Erreur: " + e.getMessage());
                    }
                }
            });
        });
    }

    private void viewDemandeDetails(Demande demande) {
        String details = "Titre: " + demande.getTitle() + "\n" +
                "Client: " + demande.getClient().getUsername() + "\n" +
                "Email: " + demande.getClient().getEmail() + "\n" +
                "Destination: " + demande.getDestination() + "\n" +
                "Budget: " + demande.getBudget() + " DT\n" +
                "Dates: " + demande.getStartDate() + " → " + demande.getEndDate() + "\n" +
                "Description: " + demande.getDescription();

        TextArea textArea = new TextArea(details);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(500, 300);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la demande");
        alert.setHeaderText("Détails complets");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    // ========== TAB 2: MY DEMANDES ==========
    private void loadMyDemandes() throws SQLException {
        List<Demande> myDemandes = demandeService.getDemandesByAgent(currentAgent.getId());

        // Configure columns
        colMyTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        colMyClient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClient().getUsername()));
        colMyDestination.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDestination()));
        colMyBudget.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f DT", data.getValue().getBudget())));
        colMyStatus.setCellValueFactory(data -> new SimpleStringProperty(getDemandeStatusLabel(data.getValue().getStatus())));

        // Actions column - Update status buttons
        colMyActions.setCellFactory(param -> new TableCell<>() {
            private final Button validateButton = new Button("✅ Valider");
            private final Button rejectButton = new Button("❌ Rejeter");
            private final Button createReservationButton = new Button("🎫 Créer Réservation");
            private final Button viewButton = new Button("👁️");

            {
                validateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                rejectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                createReservationButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                viewButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");

                validateButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    validateDemande(demande);
                });

                rejectButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    rejectMyDemande(demande);
                });

                createReservationButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    createReservationForDemande(demande);
                });

                viewButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    viewDemandeDetails(demande);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Demande demande = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5, viewButton);

                    // Show appropriate buttons based on status
                    if (demande.getStatus() == DemandeStatus.IN_PROGRESS) {
                        buttons.getChildren().addAll(validateButton, rejectButton);
                    } else if (demande.getStatus() == DemandeStatus.VALIDATED) {
                        // Check if reservation already exists
                        boolean hasReservation = reservationService.demandeHasReservation(demande.getId());
                        if (!hasReservation) {
                            buttons.getChildren().add(createReservationButton);
                        } else {
                            Label hasResLabel = new Label("✅ Réservé");
                            hasResLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            buttons.getChildren().add(hasResLabel);
                        }
                    }

                    setGraphic(buttons);
                }
            }
        });

        myDemandesTable.setItems(FXCollections.observableArrayList(myDemandes));
    }

    private void validateDemande(Demande demande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Valider la demande");
        confirm.setHeaderText("Confirmer la validation");
        confirm.setContentText("Voulez-vous valider cette demande?\n" +
                "Titre: " + demande.getTitle() + "\n" +
                "Client: " + demande.getClient().getUsername());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    demande.setStatus(DemandeStatus.VALIDATED);
                    demandeService.update(demande);

                    showAlert("Succès", "Demande validée avec succès! Le client peut maintenant créer une réservation.");
                    loadData(); // Refresh
                    statusLabel.setText("✅ Demande validée");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void rejectMyDemande(Demande demande) {
        rejectDemande(demande); // Reuse same method
    }

    private void createReservationForDemande(Demande demande) {
        try {
            // Check if reservation already exists
            if (reservationService.demandeHasReservation(demande.getId())) {
                showAlert("Information", "Une réservation existe déjà pour cette demande.");
                return;
            }

            // Create reservation
            Reservation reservation = new Reservation(
                    demande,
                    demande.getClient(),
                    currentAgent,
                    demande.getBudget() // Use demande budget as default
            );

            reservationService.add(reservation);

            showAlert("Succès", "Réservation créée avec succès!\n" +
                    "Code: " + reservation.getReservationCode() + "\n" +
                    "Prix: " + reservation.getTotalPrice() + " DT");

            loadData(); // Refresh
            statusLabel.setText("✅ Réservation créée");

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la création: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearchDemande(ActionEvent event) {
        String keyword = searchDemandeField.getText().trim();
        if (!keyword.isEmpty()) {
            try {
                List<Demande> results = demandeService.searchDemandes(keyword);
                myDemandesTable.setItems(FXCollections.observableArrayList(results));
                statusLabel.setText("🔍 " + results.size() + " résultat(s) trouvé(s)");
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur de recherche: " + e.getMessage());
            }
        } else {
            try {
                loadMyDemandes();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleRefreshDemandes(ActionEvent event) {
        try {
            loadMyDemandes();
            statusLabel.setText("✅ Demandes actualisées");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur d'actualisation: " + e.getMessage());
        }
    }

    // ========== TAB 3: RESERVATIONS ==========
    private void loadReservations() throws SQLException {
        List<Reservation> reservations = reservationService.getReservationsByAgent(currentAgent.getId());

        // Configure columns
        colResCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReservationCode()));
        colResClient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClient().getUsername()));
        colResDemande.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDemande().getTitle()));
        colResPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f DT", data.getValue().getTotalPrice())));
        colResStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        colResDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        ));

        // Actions column
        colResActions.setCellFactory(param -> new TableCell<>() {
            private final Button confirmButton = new Button("✅ Confirmer");
            private final Button completeButton = new Button("🏁 Terminer");
            private final Button cancelButton = new Button("❌ Annuler");
            private final Button viewButton = new Button("👁️");

            {
                confirmButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                completeButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                viewButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");

                confirmButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    confirmReservation(reservation);
                });

                completeButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    completeReservation(reservation);
                });

                cancelButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    cancelReservation(reservation);
                });

                viewButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    viewReservationDetails(reservation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5, viewButton);

                    // Show appropriate buttons based on status
                    switch (reservation.getStatus()) {
                        case PENDING:
                            buttons.getChildren().addAll(confirmButton, cancelButton);
                            break;
                        case CONFIRMED:
                            buttons.getChildren().addAll(completeButton, cancelButton);
                            break;
                        case COMPLETED:
                            Label completedLabel = new Label("✅ Terminé");
                            completedLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                            buttons.getChildren().add(completedLabel);
                            break;
                        case CANCELLED:
                            Label cancelledLabel = new Label("❌ Annulé");
                            cancelledLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                            buttons.getChildren().add(cancelledLabel);
                            break;
                    }

                    setGraphic(buttons);
                }
            }
        });

        reservationsTable.setItems(FXCollections.observableArrayList(reservations));
    }

    private void confirmReservation(Reservation reservation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la réservation");
        confirm.setHeaderText("Confirmer la réservation");
        confirm.setContentText("Code: " + reservation.getReservationCode() + "\n" +
                "Client: " + reservation.getClient().getUsername() + "\n" +
                "Prix: " + reservation.getTotalPrice() + " DT");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    reservation.setStatus(ReservationStatus.CONFIRMED);
                    reservationService.update(reservation);

                    showAlert("Succès", "Réservation confirmée avec succès!");
                    loadData(); // Refresh
                    statusLabel.setText("✅ Réservation confirmée");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void completeReservation(Reservation reservation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Marquer comme terminée");
        confirm.setHeaderText("Terminer la réservation");
        confirm.setContentText("Code: " + reservation.getReservationCode() + "\n" +
                "Cette action marquera la réservation comme terminée.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    reservation.setStatus(ReservationStatus.COMPLETED);
                    reservationService.update(reservation);

                    showAlert("Succès", "Réservation marquée comme terminée!");
                    loadData(); // Refresh
                    statusLabel.setText("✅ Réservation terminée");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void cancelReservation(Reservation reservation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Annuler la réservation");
        confirm.setHeaderText("Annuler la réservation");
        confirm.setContentText("Code: " + reservation.getReservationCode() + "\n" +
                "Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    reservation.setStatus(ReservationStatus.CANCELLED);
                    reservationService.update(reservation);

                    showAlert("Succès", "Réservation annulée!");
                    loadData(); // Refresh
                    statusLabel.setText("❌ Réservation annulée");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void viewReservationDetails(Reservation reservation) {
        String details = "Code: " + reservation.getReservationCode() + "\n" +
                "Client: " + reservation.getClient().getUsername() + "\n" +
                "Email: " + reservation.getClient().getEmail() + "\n" +
                "Demande: " + reservation.getDemande().getTitle() + "\n" +
                "Destination: " + reservation.getDemande().getDestination() + "\n" +
                "Prix Total: " + reservation.getTotalPrice() + " DT\n" +
                "Statut: " + reservation.getStatusLabel() + "\n" +
                "Date de création: " + reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        showAlert("Détails Réservation", details);
    }

    // ========== UTILITY METHODS ==========
    private String getDemandeStatusLabel(DemandeStatus status) {
        switch (status) {
            case PENDING: return "⏳ En attente";
            case IN_PROGRESS: return "🔄 En cours";
            case VALIDATED: return "✅ Validée";
            case REJECTED: return "❌ Rejetée";
            default: return status.toString();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            LoginController.setLoggedInUser(null);

            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion - Travel Agency");

        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
}