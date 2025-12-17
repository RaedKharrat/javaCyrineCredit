package tn.esprit.entities;

import tn.esprit.enums.UserRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String username;
    private String email;
    private String password;
    private UserRole role;
    private LocalDateTime createdAt;

    // Relationships (as lists)
    private List<Demande> demandes = new ArrayList<>();
    private List<Demande> treatedDemandes = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String email, String password, UserRole role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters (I'll show you how to generate them)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Demande> getDemandes() { return demandes; }
    public void setDemandes(List<Demande> demandes) { this.demandes = demandes; }

    public List<Demande> getTreatedDemandes() { return treatedDemandes; }
    public void setTreatedDemandes(List<Demande> treatedDemandes) { this.treatedDemandes = treatedDemandes; }

    public List<Reservation> getReservations() { return reservations; }
    public void setReservations(List<Reservation> reservations) { this.reservations = reservations; }

    @Override
    public String toString() {
        return username != null ? username : "User #" + id;
    }
}