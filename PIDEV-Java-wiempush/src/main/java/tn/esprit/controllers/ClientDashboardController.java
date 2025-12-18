package tn.esprit.controllers;

import tn.esprit.entities.Demande;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.User;
import tn.esprit.enums.DemandeStatus;
import tn.esprit.enums.ReservationStatus;
import tn.esprit.enums.UserRole;
import tn.esprit.services.DemandeService;
import tn.esprit.services.ReservationService;
import tn.esprit.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ClientDashboardController implements Initializable, DashboardInterface {

    // Services
    private final UserService userService = new UserService();
    private final DemandeService demandeService = new DemandeService();
    private final ReservationService reservationService = new ReservationService();

    // Logged in user
    private User currentUser;

    // FXML Components
    @FXML private Label welcomeLabel;
    @FXML private Button logoutButton;
    @FXML private TabPane mainTabPane;

    // Tab 1: Agents
    @FXML private TextField searchAgentField;
    @FXML private GridPane agentsGrid;
    @FXML private Label agentsMessageLabel;

    // Tab 2: Demandes
    @FXML private Button createDemandeButton;
    @FXML private TableView<Demande> demandesTable;
    @FXML private TableColumn<Demande, String> colTitle;
    @FXML private TableColumn<Demande, String> colDestination;
    @FXML private TableColumn<Demande, String> colBudget;
    @FXML private TableColumn<Demande, String> colDates;
    @FXML private TableColumn<Demande, String> colStatus;
    @FXML private TableColumn<Demande, String> colAgent;
    @FXML private TableColumn<Demande, Void> colActions;
    @FXML private Label noDemandesLabel;

    // Tab 3: Reservations
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, String> colResCode;
    @FXML private TableColumn<Reservation, String> colResDemande;
    @FXML private TableColumn<Reservation, String> colResDestination;
    @FXML private TableColumn<Reservation, String> colResPrice;
    @FXML private TableColumn<Reservation, String> colResStatus;
    @FXML private TableColumn<Reservation, String> colResDate;
    @FXML private TableColumn<Reservation, Void> colResActions;
    @FXML private Label noReservationsLabel;

    @FXML private Label statusLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Will be called after setLoggedInUser
    }

    @Override
    public void setLoggedInUser(User user) {
        this.currentUser = user;
        updateUI();
        loadData();
    }

    private void updateUI() {
        welcomeLabel.setText("👋 Bienvenue, " + currentUser.getUsername());
    }

    private void loadData() {
        try {
            loadAgents();
            loadDemandes();
            loadReservations();
            statusLabel.setText("✅ Données chargées avec succès");
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("❌ Erreur de chargement des données");
        }
    }

    // ========== TAB 1: AGENTS ==========
    private void loadAgents() {
        try {
            agentsGrid.getChildren().clear();
            List<User> agents = userService.getAgents();

            if (agents.isEmpty()) {
                agentsMessageLabel.setText("Aucun agent disponible pour le moment.");
                return;
            }

            agentsMessageLabel.setText(agents.size() + " agent(s) disponible(s)");

            int col = 0;
            int row = 0;
            int maxCols = 3;

            for (User agent : agents) {
                VBox agentCard = createAgentCard(agent);
                agentsGrid.add(agentCard, col, row);

                col++;
                if (col >= maxCols) {
                    col = 0;
                    row++;
                }
            }
        } catch (SQLException e) {
            agentsMessageLabel.setText("Erreur de chargement des agents");
            e.printStackTrace();
        }
    }

    private VBox createAgentCard(User agent) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-pref-width: 250; -fx-pref-height: 200;");

        Label nameLabel = new Label("👨‍💼 " + agent.getUsername());
        nameLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label emailLabel = new Label("📧 " + agent.getEmail());
        emailLabel.setStyle("-fx-text-fill: #666;");

        Label memberSinceLabel = new Label("📅 Membre depuis: " +
                agent.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        memberSinceLabel.setStyle("-fx-text-fill: #666;");

        Button createDemandeBtn = new Button("📝 Créer une demande");
        createDemandeBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-pref-width: 200; -fx-pref-height: 35;");
        createDemandeBtn.setOnAction(e -> handleCreateDemandeForAgent(agent));

        card.getChildren().addAll(nameLabel, emailLabel, memberSinceLabel, createDemandeBtn);
        return card;
    }

    @FXML
    private void handleSearchAgent(ActionEvent event) {
        String keyword = searchAgentField.getText().trim();
        showAlert("Info", "Recherche d'agent: " + keyword);
    }

    @FXML
    private void handleRefreshAgents(ActionEvent event) {
        loadAgents();
        statusLabel.setText("✅ Liste des agents actualisée");
    }

    // ========== TAB 2: DEMANDES ==========
    private void loadDemandes() {
        try {
            List<Demande> demandes = demandeService.getDemandesByClient(currentUser.getId());

            if (demandes.isEmpty()) {
                noDemandesLabel.setVisible(true);
                demandesTable.setVisible(false);
                return;
            }

            noDemandesLabel.setVisible(false);
            demandesTable.setVisible(true);

            // Configure columns
            colTitle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
            colDestination.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDestination()));
            colBudget.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f DT", data.getValue().getBudget())));
            colDates.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getStartDate() + " → " + data.getValue().getEndDate()
            ));
            colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                    getDemandeStatusLabel(data.getValue().getStatus())
            ));
            colAgent.setCellValueFactory(data -> {
                User agent = data.getValue().getAssignedAgent();
                return new SimpleStringProperty(agent != null ? agent.getUsername() : "Non assigné");
            });

            // Actions column
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button viewButton = new Button("👁️ Voir");
                private final Button editButton = new Button("✏️ Modifier");
                private final Button deleteButton = new Button("🗑️ Supprimer");

                {
                    viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    editButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                    deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                    viewButton.setOnAction(e -> {
                        Demande demande = getTableView().getItems().get(getIndex());
                        viewDemande(demande);
                    });

                    editButton.setOnAction(e -> {
                        Demande demande = getTableView().getItems().get(getIndex());
                        editDemande(demande);
                    });

                    deleteButton.setOnAction(e -> {
                        Demande demande = getTableView().getItems().get(getIndex());
                        deleteDemande(demande);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, viewButton, editButton, deleteButton);
                        setGraphic(buttons);
                    }
                }
            });

            demandesTable.setItems(FXCollections.observableArrayList(demandes));

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des demandes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreateDemande(ActionEvent event) {
        showAgentSelectionDialog();
    }

    private void handleCreateDemandeForAgent(User agent) {
        openDemandeForm(agent);
    }

    private void showAgentSelectionDialog() {
        try {
            List<User> agents = userService.getAgents();

            if (agents.isEmpty()) {
                showAlert("Info", "Aucun agent disponible pour le moment.");
                return;
            }

            ChoiceDialog<User> dialog = new ChoiceDialog<>(agents.get(0), agents);
            dialog.setTitle("Sélectionner un Agent");
            dialog.setHeaderText("Choisissez l'agent pour votre demande");
            dialog.setContentText("Agent:");

            dialog.showAndWait().ifPresent(this::openDemandeForm);

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des agents: " + e.getMessage());
        }
    }

    private void openDemandeForm(User agent) {
        try {
            // Load the CreateDemande form
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CreateDemande.fxml"));
            Parent root = loader.load();

            // Pass data to the CreateDemande controller
            CreateDemandeController controller = loader.getController();
            controller.setSelectedAgent(agent);
            controller.setCurrentClient(currentUser);

            // Open in new window or replace current scene
            Stage stage = new Stage();
            stage.setTitle("Nouvelle Demande - " + agent.getUsername());
            stage.setScene(new Scene(root, 900, 700));
            stage.show();

            // Optional: Close the current window if you want
            // Stage currentStage = (Stage) createDemandeButton.getScene().getWindow();
            // currentStage.close();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de demande: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewDemande(Demande demande) {
        showAlert("Détails Demande",
                "Titre: " + demande.getTitle() + "\n" +
                        "Destination: " + demande.getDestination() + "\n" +
                        "Budget: " + demande.getBudget() + " DT\n" +
                        "Dates: " + demande.getStartDate() + " → " + demande.getEndDate() + "\n" +
                        "Statut: " + getDemandeStatusLabel(demande.getStatus()) + "\n" +
                        "Agent: " + (demande.getAssignedAgent() != null ? demande.getAssignedAgent().getUsername() : "Non assigné")
        );
    }

    private void editDemande(Demande demande) {
        if (demande.getStatus() != DemandeStatus.PENDING) {
            showAlert("Attention", "Vous ne pouvez modifier que les demandes en attente.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(demande.getTitle());
        dialog.setTitle("Modifier Demande");
        dialog.setHeaderText("Modifier le titre de la demande");
        dialog.setContentText("Nouveau titre:");

        dialog.showAndWait().ifPresent(newTitle -> {
            demande.setTitle(newTitle);
            demandeService.update(demande);
            loadDemandes();
            statusLabel.setText("✅ Demande modifiée avec succès!");
        });
    }

    private void deleteDemande(Demande demande) {
        if (demande.getStatus() != DemandeStatus.PENDING) {
            showAlert("Attention", "Vous ne pouvez supprimer que les demandes en attente.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la demande");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette demande?\n" + demande.getTitle());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                demandeService.delete(demande);
                loadDemandes();
                statusLabel.setText("✅ Demande supprimée avec succès!");
            }
        });
    }

    // ========== TAB 3: RESERVATIONS ==========
    private void loadReservations() {
        try {
            List<Reservation> reservations = reservationService.getReservationsByClient(currentUser.getId());

            if (reservations.isEmpty()) {
                noReservationsLabel.setVisible(true);
                reservationsTable.setVisible(false);
                return;
            }

            noReservationsLabel.setVisible(false);
            reservationsTable.setVisible(true);

            // Configure columns
            colResCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReservationCode()));
            colResDemande.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDemande().getTitle()));
            colResDestination.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDemande().getDestination()));
            colResPrice.setCellValueFactory(data -> new SimpleStringProperty(String.format("%.2f DT", data.getValue().getTotalPrice())));
            colResStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
            colResDate.setCellValueFactory(data -> new SimpleStringProperty(
                    data.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            ));

            // Actions column
            colResActions.setCellFactory(param -> new TableCell<>() {
                private final Button viewButton = new Button("👁️ Voir");
                private final Button cancelButton = new Button("❌ Annuler");

                {
                    viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                    viewButton.setOnAction(e -> {
                        Reservation reservation = getTableView().getItems().get(getIndex());
                        viewReservation(reservation);
                    });

                    cancelButton.setOnAction(e -> {
                        Reservation reservation = getTableView().getItems().get(getIndex());
                        cancelReservation(reservation);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Reservation reservation = getTableView().getItems().get(getIndex());
                        cancelButton.setDisable(!(
                                reservation.getStatus() == ReservationStatus.PENDING ||
                                        reservation.getStatus() == ReservationStatus.CONFIRMED
                        ));

                        HBox buttons = new HBox(5, viewButton, cancelButton);
                        setGraphic(buttons);
                    }
                }
            });

            reservationsTable.setItems(FXCollections.observableArrayList(reservations));

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des réservations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewReservation(Reservation reservation) {
        showAlert("Détails Réservation",
                "Code: " + reservation.getReservationCode() + "\n" +
                        "Demande: " + reservation.getDemande().getTitle() + "\n" +
                        "Destination: " + reservation.getDemande().getDestination() + "\n" +
                        "Prix Total: " + reservation.getTotalPrice() + " DT\n" +
                        "Statut: " + reservation.getStatusLabel() + "\n" +
                        "Date de création: " + reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n" +
                        "Agent: " + reservation.getAgent().getUsername()
        );
    }

    private void cancelReservation(Reservation reservation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation d'annulation");
        confirm.setHeaderText("Annuler la réservation");
        confirm.setContentText("Êtes-vous sûr de vouloir annuler cette réservation?\n" +
                "Code: " + reservation.getReservationCode() + "\n" +
                "Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                reservation.setStatus(ReservationStatus.CANCELLED);
                reservationService.update(reservation);
                loadReservations();
                statusLabel.setText("✅ Réservation annulée avec succès!");
            }
        });
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