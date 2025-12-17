package tn.esprit.entities;

import tn.esprit.enums.DemandeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Demande {
    private int id;
    private String title;
    private String description;
    private String destination;
    private double budget;
    private LocalDate startDate;
    private LocalDate endDate;
    private DemandeStatus status;
    private LocalDateTime createdAt;

    // Relationships
    private User client;
    private User assignedAgent;
    private Reservation reservation;

    // Constructors
    public Demande() {
        this.createdAt = LocalDateTime.now();
        this.status = DemandeStatus.PENDING;
    }

    public Demande(String title, String description, String destination,
                   double budget, LocalDate startDate, LocalDate endDate, User client) {
        this();
        this.title = title;
        this.description = description;
        this.destination = destination;
        this.budget = budget;
        this.startDate = startDate;
        this.endDate = endDate;
        this.client = client;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public DemandeStatus getStatus() { return status; }
    public void setStatus(DemandeStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }

    public User getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(User assignedAgent) { this.assignedAgent = assignedAgent; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
        // Set inverse side
        if (reservation != null && reservation.getDemande() != this) {
            reservation.setDemande(this);
        }
    }

    @Override
    public String toString() {
        return title != null ? title : "Demande #" + id;
    }
}