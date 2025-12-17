package tn.esprit.entities;

import tn.esprit.enums.ReservationStatus;
import java.time.LocalDateTime;

public class Reservation {
    private int id;
    private String reservationCode;
    private double totalPrice;
    private LocalDateTime createdAt;
    private ReservationStatus status;

    // Relationships
    private Demande demande;
    private User client;
    private User agent;

    // Constructors
    public Reservation() {
        this.createdAt = LocalDateTime.now();
        this.status = ReservationStatus.PENDING;
        this.reservationCode = generateReservationCode();
    }

    public Reservation(Demande demande, User client, User agent, double totalPrice) {
        this();
        this.demande = demande;
        this.client = client;
        this.agent = agent;
        this.totalPrice = totalPrice;

        // Set inverse relationships
        if (demande != null) {
            demande.setReservation(this);
        }
    }

    private String generateReservationCode() {
        // Generate R-XXXXXX code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder("R-");
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReservationCode() { return reservationCode; }
    public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public Demande getDemande() { return demande; }
    public void setDemande(Demande demande) {
        this.demande = demande;
        if (demande != null && demande.getReservation() != this) {
            demande.setReservation(this);
        }
    }

    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }

    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }

    @Override
    public String toString() {
        return reservationCode != null ? reservationCode : "Reservation #" + id;
    }

    // Helper methods
    public static String getStatusLabel(ReservationStatus status) {
        switch (status) {
            case PENDING: return "En attente";
            case CONFIRMED: return "Confirmée";
            case CANCELLED: return "Annulée";
            case COMPLETED: return "Terminée";
            default: return status.toString();
        }
    }

    public String getStatusLabel() {
        return getStatusLabel(this.status);
    }
}