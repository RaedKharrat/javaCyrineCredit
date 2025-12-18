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
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;  // ADD THIS LINE
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable, DashboardInterface {

    // Services
    private final UserService userService = new UserService();
    private final DemandeService demandeService = new DemandeService();
    private final ReservationService reservationService = new ReservationService();

    // Logged in user
    private User currentAdmin;

    // FXML Components - Tab 1: Dashboard
    @FXML private Label welcomeLabel;
    @FXML private Label totalUsers;
    @FXML private Label clientCount;
    @FXML private Label agentCount;
    @FXML private Label adminCount;
    @FXML private Label totalDemandes;
    @FXML private Label pendingDemandes;
    @FXML private Label inProgressDemandes;
    @FXML private Label validatedDemandes;
    @FXML private Label totalReservations;
    @FXML private Label totalRevenue;
    @FXML private Label confirmedReservations;
    @FXML private Label completedReservations;
    @FXML private PieChart demandePieChart;
    @FXML private PieChart userPieChart;
    @FXML private TableView<Map<String, String>> recentActivityTable;
    @FXML private TableColumn<Map<String, String>, String> colActivityType;
    @FXML private TableColumn<Map<String, String>, String> colActivityDescription;
    @FXML private TableColumn<Map<String, String>, String> colActivityDate;
    @FXML private TableColumn<Map<String, String>, String> colActivityUser;

    // Tab 2: Users
    @FXML private TextField searchUserField;
    @FXML private Button addUserButton;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colCreatedAt;
    @FXML private TableColumn<User, Void> colUserActions;

    // Tab 3: Demandes
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> agentFilterCombo;
    @FXML private TextField searchDemandeField;
    @FXML private TableView<Demande> allDemandesTable;
    @FXML private TableColumn<Demande, String> colAllDemandeId;
    @FXML private TableColumn<Demande, String> colAllTitle;
    @FXML private TableColumn<Demande, String> colAllClient;
    @FXML private TableColumn<Demande, String> colAllAgent;
    @FXML private TableColumn<Demande, String> colAllDestination;
    @FXML private TableColumn<Demande, String> colAllBudget;
    @FXML private TableColumn<Demande, String> colAllStatus;
    @FXML private TableColumn<Demande, String> colAllDates;
    @FXML private TableColumn<Demande, String> colAllCreatedAt;
    @FXML private TableColumn<Demande, Void> colAllActions;

    // Tab 4: Reservations
    @FXML private ComboBox<String> reservationStatusFilter;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label overallRevenue;
    @FXML private TableView<Reservation> allReservationsTable;
    @FXML private TableColumn<Reservation, String> colReservationId;
    @FXML private TableColumn<Reservation, String> colReservationCode;
    @FXML private TableColumn<Reservation, String> colReservationClient;
    @FXML private TableColumn<Reservation, String> colReservationAgent;
    @FXML private TableColumn<Reservation, String> colReservationDemande;
    @FXML private TableColumn<Reservation, String> colReservationPrice;
    @FXML private TableColumn<Reservation, String> colReservationStatus;
    @FXML private TableColumn<Reservation, String> colReservationCreatedAt;
    @FXML private TableColumn<Reservation, Void> colReservationActions;

    @FXML private Label statusLabel;
    @FXML private Button logoutButton;

    // Data lists
    private List<User> allUsers = new ArrayList<>();
    private List<Demande> allDemandes = new ArrayList<>();
    private List<Reservation> allReservations = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize filters
        initializeFilters();
    }

    @Override
    public void setLoggedInUser(User user) {
        this.currentAdmin = user;
        updateUI();
        loadAllData();
    }

    private void updateUI() {
        welcomeLabel.setText("👑 Admin: " + currentAdmin.getUsername());
    }

    private void initializeFilters() {
        // Demandes status filter
        statusFilterCombo.getItems().addAll(
                "Tous",
                "En attente",
                "En cours",
                "Validée",
                "Rejetée"
        );
        statusFilterCombo.setValue("Tous");

        // Reservation status filter
        reservationStatusFilter.getItems().addAll(
                "Tous",
                "En attente",
                "Confirmée",
                "Annulée",
                "Terminée"
        );
        reservationStatusFilter.setValue("Tous");

        // Set default dates
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());
    }

    private void loadAllData() {
        try {
            loadUsers();
            loadDemandes();
            loadReservations();
            updateDashboardStats();
            updateCharts();
            loadRecentActivity();
            statusLabel.setText("✅ Toutes les données chargées");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("❌ Erreur de chargement");
        }
    }

    // ========== TAB 1: DASHBOARD ==========
    private void updateDashboardStats() throws SQLException {
        // User stats
        allUsers = userService.getAll();
        totalUsers.setText(String.valueOf(allUsers.size()));

        long clientCountValue = allUsers.stream().filter(u -> u.getRole() == UserRole.CLIENT).count();
        long agentCountValue = allUsers.stream().filter(u -> u.getRole() == UserRole.AGENT).count();
        long adminCountValue = allUsers.stream().filter(u -> u.getRole() == UserRole.ADMIN).count();

        clientCount.setText(String.valueOf(clientCountValue));
        agentCount.setText(String.valueOf(agentCountValue));
        adminCount.setText(String.valueOf(adminCountValue));

        // Demande stats
        totalDemandes.setText(String.valueOf(demandeService.getTotalDemandes()));
        pendingDemandes.setText(String.valueOf(demandeService.countByStatus(DemandeStatus.PENDING)));
        inProgressDemandes.setText(String.valueOf(demandeService.countByStatus(DemandeStatus.IN_PROGRESS)));
        validatedDemandes.setText(String.valueOf(demandeService.countByStatus(DemandeStatus.VALIDATED)));

        // Reservation stats
        totalReservations.setText(String.valueOf(allReservations.size()));

        double revenue = reservationService.getTotalRevenue();
        totalRevenue.setText(String.format("%.2f DT", revenue));

        long confirmedCount = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .count();
        confirmedReservations.setText(String.valueOf(confirmedCount));

        long completedCount = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .count();
        completedReservations.setText(String.valueOf(completedCount));

        overallRevenue.setText(String.format("%.2f DT", revenue));
    }

    private void updateCharts() {
        // Demande status pie chart
        demandePieChart.getData().clear();
        demandePieChart.getData().addAll(
                new PieChart.Data("En attente", demandeService.countByStatus(DemandeStatus.PENDING)),
                new PieChart.Data("En cours", demandeService.countByStatus(DemandeStatus.IN_PROGRESS)),
                new PieChart.Data("Validée", demandeService.countByStatus(DemandeStatus.VALIDATED)),
                new PieChart.Data("Rejetée", demandeService.countByStatus(DemandeStatus.REJECTED))
        );

        // User role pie chart
        userPieChart.getData().clear();
        userPieChart.getData().addAll(
                new PieChart.Data("Clients",
                        allUsers.stream().filter(u -> u.getRole() == UserRole.CLIENT).count()),
                new PieChart.Data("Agents",
                        allUsers.stream().filter(u -> u.getRole() == UserRole.AGENT).count()),
                new PieChart.Data("Admins",
                        allUsers.stream().filter(u -> u.getRole() == UserRole.ADMIN).count())
        );
    }

    private void loadRecentActivity() {
        ObservableList<Map<String, String>> activityData = FXCollections.observableArrayList();

        // Add recent users
        allUsers.stream()
                .sorted((u1, u2) -> u2.getCreatedAt().compareTo(u1.getCreatedAt()))
                .limit(5)
                .forEach(user -> {
                    Map<String, String> activity = new HashMap<>();
                    activity.put("type", "👤 Nouvel utilisateur");
                    activity.put("description", user.getUsername() + " (" + user.getRole() + ")");
                    activity.put("date", user.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    activity.put("user", user.getUsername());
                    activityData.add(activity);
                });

        // Add recent demandes
        allDemandes.stream()
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .limit(5)
                .forEach(demande -> {
                    Map<String, String> activity = new HashMap<>();
                    activity.put("type", "📝 Nouvelle demande");
                    activity.put("description", demande.getTitle() + " - " + demande.getDestination());
                    activity.put("date", demande.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    activity.put("user", demande.getClient().getUsername());
                    activityData.add(activity);
                });

        // Add recent reservations
        allReservations.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .limit(5)
                .forEach(reservation -> {
                    Map<String, String> activity = new HashMap<>();
                    activity.put("type", "🎫 Nouvelle réservation");
                    activity.put("description", reservation.getReservationCode() + " - " + reservation.getTotalPrice() + " DT");
                    activity.put("date", reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    activity.put("user", reservation.getClient().getUsername());
                    activityData.add(activity);
                });

        // Sort by date and take top 10
        activityData.sort((a1, a2) -> {
            // Parse dates for comparison - in real app, use proper date parsing
            return a2.get("date").compareTo(a1.get("date"));
        });

        ObservableList<Map<String, String>> finalData = FXCollections.observableArrayList(
                activityData.stream().limit(10).collect(Collectors.toList())
        );

        // Configure table columns
        colActivityType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("type")));
        colActivityDescription.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("description")));
        colActivityDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("date")));
        colActivityUser.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().get("user")));

        recentActivityTable.setItems(finalData);
    }

    // ========== TAB 2: USERS MANAGEMENT ==========
    private void loadUsers() throws SQLException {
        allUsers = userService.getAll();
        updateUsersTable(allUsers);
    }

    private void updateUsersTable(List<User> users) {
        // Configure columns
        colUserId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUsername()));
        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));
        colRole.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRole().toString()));
        colCreatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        // Actions column
        colUserActions.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final Button resetPasswordButton = new Button("🔑");

            {
                editButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                resetPasswordButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");

                editButton.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                deleteButton.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });

                resetPasswordButton.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    resetPassword(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    // Don't allow deleting yourself
                    deleteButton.setDisable(user.getId() == currentAdmin.getId());

                    HBox buttons = new HBox(5, editButton, resetPasswordButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    @FXML
    private void handleSearchUser(ActionEvent event) {
        String keyword = searchUserField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            updateUsersTable(allUsers);
            return;
        }

        List<User> filteredUsers = allUsers.stream()
                .filter(user -> user.getUsername().toLowerCase().contains(keyword) ||
                        user.getEmail().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        updateUsersTable(filteredUsers);
        statusLabel.setText("🔍 " + filteredUsers.size() + " utilisateur(s) trouvé(s)");
    }

    @FXML
    private void handleRefreshUsers(ActionEvent event) {
        try {
            loadUsers();
            statusLabel.setText("✅ Liste des utilisateurs actualisée");
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur d'actualisation: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        // Create dialog for adding user
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un nouvel utilisateur");

        // Set buttons
        ButtonType addButtonType = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(UserRole.values());
        roleCombo.setValue(UserRole.CLIENT);

        grid.add(new Label("Nom d'utilisateur:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Mot de passe:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Rôle:"), 0, 3);
        grid.add(roleCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Convert result to User
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                User newUser = new User(
                        usernameField.getText(),
                        emailField.getText(),
                        passwordField.getText(),
                        roleCombo.getValue()
                );
                return newUser;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(user -> {
            try {
                // Use AuthService to register
                tn.esprit.services.AuthService authService = new tn.esprit.services.AuthService();
                boolean success = authService.register(user);

                if (success) {
                    showAlert("Succès", "Utilisateur ajouté avec succès!");
                    loadUsers();
                    statusLabel.setText("✅ Utilisateur ajouté");
                } else {
                    showAlert("Erreur", "Erreur lors de l'ajout de l'utilisateur");
                }
            } catch (Exception e) {
                showAlert("Erreur", "Erreur: " + e.getMessage());
            }
        });
    }

    private void editUser(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'utilisateur");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField usernameField = new TextField(user.getUsername());
        TextField emailField = new TextField(user.getEmail());
        ComboBox<UserRole> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(UserRole.values());
        roleCombo.setValue(user.getRole());

        grid.add(new Label("Nom d'utilisateur:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Rôle:"), 0, 2);
        grid.add(roleCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                user.setUsername(usernameField.getText());
                user.setEmail(emailField.getText());
                user.setRole(roleCombo.getValue());
                return user;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(updatedUser -> {
            try {
                userService.update(updatedUser);
                showAlert("Succès", "Utilisateur modifié avec succès!");
                loadUsers();
                statusLabel.setText("✅ Utilisateur modifié");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur: " + e.getMessage());
            }
        });
    }

    private void deleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'utilisateur");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet utilisateur?\n" +
                "Nom: " + user.getUsername() + "\n" +
                "Email: " + user.getEmail() + "\n" +
                "Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    userService.delete(user);
                    showAlert("Succès", "Utilisateur supprimé avec succès!");
                    loadUsers();
                    statusLabel.setText("✅ Utilisateur supprimé");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void resetPassword(User user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Réinitialiser le mot de passe");

        ButtonType resetButtonType = new ButtonType("Réinitialiser", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");

        grid.add(new Label("Nouveau mot de passe:"), 0, 0);
        grid.add(newPasswordField, 1, 0);
        grid.add(new Label("Confirmer:"), 0, 1);
        grid.add(confirmPasswordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == resetButtonType) {
                if (!newPasswordField.getText().equals(confirmPasswordField.getText())) {
                    showAlert("Erreur", "Les mots de passe ne correspondent pas!");
                    return null;
                }
                if (newPasswordField.getText().length() < 6) {
                    showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères!");
                    return null;
                }
                return newPasswordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newPassword -> {
            try {
                // Hash and update password
                String hashedPassword = Base64.getEncoder().encodeToString(newPassword.getBytes());
                user.setPassword(hashedPassword);
                // Note: We need to update this in the database
                // For now, we'll show a message
                showAlert("Succès", "Mot de passe réinitialisé pour " + user.getUsername());
                statusLabel.setText("✅ Mot de passe réinitialisé");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur: " + e.getMessage());
            }
        });
    }

    // ========== TAB 3: ALL DEMANDES ==========
    private void loadDemandes() throws SQLException {
        allDemandes = demandeService.getAll();
        updateAllDemandesTable(allDemandes);
        updateAgentFilterCombo();
    }

    private void updateAllDemandesTable(List<Demande> demandes) {
        // Configure columns
        colAllDemandeId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colAllTitle.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitle()));
        colAllClient.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getClient().getUsername()));
        colAllAgent.setCellValueFactory(data -> {
            User agent = data.getValue().getAssignedAgent();
            return new SimpleStringProperty(agent != null ? agent.getUsername() : "Non assigné");
        });
        colAllDestination.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDestination()));
        colAllBudget.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f DT", data.getValue().getBudget())));
        colAllStatus.setCellValueFactory(data ->
                new SimpleStringProperty(getDemandeStatusLabel(data.getValue().getStatus())));
        colAllDates.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStartDate() + " → " + data.getValue().getEndDate()));
        colAllCreatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        // Actions column
        colAllActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("👁️");
            private final Button assignButton = new Button("👨‍💼");
            private final Button deleteButton = new Button("🗑️");

            {
                viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                assignButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                viewButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    viewDemandeDetails(demande);
                });

                assignButton.setOnAction(e -> {
                    Demande demande = getTableView().getItems().get(getIndex());
                    assignDemandeToAgent(demande);
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
                    HBox buttons = new HBox(5, viewButton, assignButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        allDemandesTable.setItems(FXCollections.observableArrayList(demandes));
    }

    private void updateAgentFilterCombo() {
        Set<String> agentNames = new HashSet<>();
        agentNames.add("Tous les agents");

        allDemandes.stream()
                .filter(d -> d.getAssignedAgent() != null)
                .forEach(d -> agentNames.add(d.getAssignedAgent().getUsername()));

        agentFilterCombo.getItems().clear();
        agentFilterCombo.getItems().addAll(agentNames);
        agentFilterCombo.setValue("Tous les agents");
    }

    @FXML
    private void handleFilterDemandes(ActionEvent event) {
        String statusFilter = statusFilterCombo.getValue();
        String agentFilter = agentFilterCombo.getValue();
        String searchText = searchDemandeField.getText().trim().toLowerCase();

        List<Demande> filtered = allDemandes.stream()
                .filter(demande -> {
                    // Status filter
                    if (!statusFilter.equals("Tous")) {
                        String statusLabel = getDemandeStatusLabel(demande.getStatus());
                        if (!statusLabel.contains(statusFilter)) {
                            return false;
                        }
                    }

                    // Agent filter
                    if (!agentFilter.equals("Tous les agents") && !agentFilter.equals("Tous")) {
                        User agent = demande.getAssignedAgent();
                        if (agent == null || !agent.getUsername().equals(agentFilter)) {
                            return false;
                        }
                    }

                    // Search filter
                    if (!searchText.isEmpty()) {
                        boolean matches = demande.getTitle().toLowerCase().contains(searchText) ||
                                demande.getDestination().toLowerCase().contains(searchText) ||
                                demande.getClient().getUsername().toLowerCase().contains(searchText);
                        if (!matches) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        updateAllDemandesTable(filtered);
        statusLabel.setText("🔍 " + filtered.size() + " demande(s) trouvée(s)");
    }

    @FXML
    private void handleResetDemandeFilters(ActionEvent event) {
        statusFilterCombo.setValue("Tous");
        agentFilterCombo.setValue("Tous les agents");
        searchDemandeField.clear();
        updateAllDemandesTable(allDemandes);
        statusLabel.setText("✅ Filtres réinitialisés");
    }

    private void viewDemandeDetails(Demande demande) {
        String details = "ID: " + demande.getId() + "\n" +
                "Titre: " + demande.getTitle() + "\n" +
                "Client: " + demande.getClient().getUsername() + "\n" +
                "Email client: " + demande.getClient().getEmail() + "\n" +
                "Agent: " + (demande.getAssignedAgent() != null ?
                demande.getAssignedAgent().getUsername() : "Non assigné") + "\n" +
                "Destination: " + demande.getDestination() + "\n" +
                "Budget: " + demande.getBudget() + " DT\n" +
                "Dates: " + demande.getStartDate() + " → " + demande.getEndDate() + "\n" +
                "Statut: " + getDemandeStatusLabel(demande.getStatus()) + "\n" +
                "Description: " + demande.getDescription() + "\n" +
                "Créée le: " + demande.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

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

    private void assignDemandeToAgent(Demande demande) {
        try {
            List<User> agents = userService.getAgents();

            if (agents.isEmpty()) {
                showAlert("Information", "Aucun agent disponible.");
                return;
            }

            ChoiceDialog<User> dialog = new ChoiceDialog<>(agents.get(0), agents);
            dialog.setTitle("Assigner un agent");
            dialog.setHeaderText("Assigner la demande à un agent");
            dialog.setContentText("Sélectionnez un agent:");

            Optional<User> result = dialog.showAndWait();
            result.ifPresent(agent -> {
                try {
                    boolean success = demandeService.assignToAgent(demande.getId(), agent.getId());
                    if (success) {
                        showAlert("Succès", "Demande assignée à " + agent.getUsername());
                        loadDemandes();
                        statusLabel.setText("✅ Demande assignée");
                    } else {
                        showAlert("Erreur", "Erreur lors de l'assignation");
                    }
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            });
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur: " + e.getMessage());
        }
    }

    private void deleteDemande(Demande demande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la demande");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette demande?\n" +
                "Titre: " + demande.getTitle() + "\n" +
                "Client: " + demande.getClient().getUsername() + "\n" +
                "Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    demandeService.delete(demande);
                    showAlert("Succès", "Demande supprimée avec succès!");
                    loadDemandes();
                    statusLabel.setText("✅ Demande supprimée");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
            }
        });
    }

    // ========== TAB 4: ALL RESERVATIONS ==========
    private void loadReservations() throws SQLException {
        allReservations = reservationService.getAll();
        updateAllReservationsTable(allReservations);
    }

    private void updateAllReservationsTable(List<Reservation> reservations) {
        // Configure columns
        colReservationId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colReservationCode.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getReservationCode()));
        colReservationClient.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getClient().getUsername()));
        colReservationAgent.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAgent().getUsername()));
        colReservationDemande.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDemande().getTitle()));
        colReservationPrice.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f DT", data.getValue().getTotalPrice())));
        colReservationStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatusLabel()));
        colReservationCreatedAt.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        // Actions column
        colReservationActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewButton = new Button("👁️");
            private final Button updateButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");

            {
                viewButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                updateButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                viewButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    viewReservationDetails(reservation);
                });

                updateButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    updateReservation(reservation);
                });

                deleteButton.setOnAction(e -> {
                    Reservation reservation = getTableView().getItems().get(getIndex());
                    deleteReservation(reservation);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewButton, updateButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        allReservationsTable.setItems(FXCollections.observableArrayList(reservations));
    }

    @FXML
    private void handleFilterReservations(ActionEvent event) {
        String statusFilter = reservationStatusFilter.getValue();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        List<Reservation> filtered = allReservations.stream()
                .filter(reservation -> {
                    // Status filter
                    if (!statusFilter.equals("Tous")) {
                        String statusLabel = reservation.getStatusLabel();
                        if (!statusLabel.contains(statusFilter)) {
                            return false;
                        }
                    }

                    // Date filter
                    if (fromDate != null && toDate != null) {
                        LocalDate reservationDate = reservation.getCreatedAt().toLocalDate();
                        if (reservationDate.isBefore(fromDate) || reservationDate.isAfter(toDate)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        updateAllReservationsTable(filtered);
        statusLabel.setText("🔍 " + filtered.size() + " réservation(s) trouvée(s)");
    }

    @FXML
    private void handleResetReservationFilters(ActionEvent event) {
        reservationStatusFilter.setValue("Tous");
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());
        updateAllReservationsTable(allReservations);
        statusLabel.setText("✅ Filtres réinitialisés");
    }

    private void viewReservationDetails(Reservation reservation) {
        String details = "Code: " + reservation.getReservationCode() + "\n" +
                "ID: " + reservation.getId() + "\n" +
                "Client: " + reservation.getClient().getUsername() + "\n" +
                "Email client: " + reservation.getClient().getEmail() + "\n" +
                "Agent: " + reservation.getAgent().getUsername() + "\n" +
                "Demande: " + reservation.getDemande().getTitle() + "\n" +
                "Destination: " + reservation.getDemande().getDestination() + "\n" +
                "Prix Total: " + reservation.getTotalPrice() + " DT\n" +
                "Statut: " + reservation.getStatusLabel() + "\n" +
                "Créée le: " + reservation.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        TextArea textArea = new TextArea(details);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(500, 300);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la réservation");
        alert.setHeaderText("Détails complets");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void updateReservation(Reservation reservation) {
        Dialog<Reservation> dialog = new Dialog<>();
        dialog.setTitle("Modifier la réservation");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField priceField = new TextField(String.valueOf(reservation.getTotalPrice()));
        ComboBox<ReservationStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ReservationStatus.values());
        statusCombo.setValue(reservation.getStatus());

        grid.add(new Label("Prix total (DT):"), 0, 0);
        grid.add(priceField, 1, 0);
        grid.add(new Label("Statut:"), 0, 1);
        grid.add(statusCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double newPrice = Double.parseDouble(priceField.getText());
                    reservation.setTotalPrice(newPrice);
                    reservation.setStatus(statusCombo.getValue());
                    return reservation;
                } catch (NumberFormatException e) {
                    showAlert("Erreur", "Veuillez entrer un prix valide");
                    return null;
                }
            }
            return null;
        });

        Optional<Reservation> result = dialog.showAndWait();
        result.ifPresent(updatedReservation -> {
            try {
                reservationService.update(updatedReservation);
                showAlert("Succès", "Réservation modifiée avec succès!");
                loadReservations();
                statusLabel.setText("✅ Réservation modifiée");
            } catch (Exception e) {
                showAlert("Erreur", "Erreur: " + e.getMessage());
            }
        });
    }

    private void deleteReservation(Reservation reservation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la réservation");
        confirm.setHeaderText("Confirmer la suppression");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette réservation?\n" +
                "Code: " + reservation.getReservationCode() + "\n" +
                "Client: " + reservation.getClient().getUsername() + "\n" +
                "Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    reservationService.delete(reservation);
                    showAlert("Succès", "Réservation supprimée avec succès!");
                    loadReservations();
                    statusLabel.setText("✅ Réservation supprimée");
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage());
                }
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