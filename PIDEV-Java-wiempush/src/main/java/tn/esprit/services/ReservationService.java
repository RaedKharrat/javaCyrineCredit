package tn.esprit.services;

import tn.esprit.entities.Reservation;
import tn.esprit.entities.Demande;
import tn.esprit.entities.User;
import tn.esprit.enums.ReservationStatus;
import tn.esprit.interfaces.IService;
import tn.esprit.util.MaConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationService implements IService<Reservation> {

    @Override
    public void add(Reservation reservation) {
        String query = "INSERT INTO reservation (reservation_code, total_price, status, demande_id, client_id, agent_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reservation.getReservationCode());
            ps.setDouble(2, reservation.getTotalPrice());
            ps.setString(3, reservation.getStatus().toString());
            ps.setInt(4, reservation.getDemande().getId());
            ps.setInt(5, reservation.getClient().getId());
            ps.setInt(6, reservation.getAgent().getId());

            ps.executeUpdate();

            // Get the generated ID
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                reservation.setId(generatedKeys.getInt(1));
            }

            System.out.println("✅ Reservation added successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Reservation reservation) {
        String query = "UPDATE reservation SET total_price = ?, status = ? WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setDouble(1, reservation.getTotalPrice());
            ps.setString(2, reservation.getStatus().toString());
            ps.setInt(3, reservation.getId());

            ps.executeUpdate();
            System.out.println("✅ Reservation updated successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Reservation reservation) {
        String query = "DELETE FROM reservation WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, reservation.getId());
            ps.executeUpdate();
            System.out.println("✅ Reservation deleted successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Reservation> getAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "ORDER BY r.created_at DESC";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    @Override
    public Reservation getOne(int id) {
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "WHERE r.id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToReservation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // SPECIAL METHODS FOR THE PLATFORM

    // 1. Get reservations for a specific client
    public List<Reservation> getReservationsByClient(int clientId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "WHERE r.client_id = ? " +
                "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    // 2. Get reservations handled by a specific agent
    public List<Reservation> getReservationsByAgent(int agentId) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "WHERE r.agent_id = ? " +
                "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    // 3. Get reservation by demande ID (OneToOne relationship)
    public Reservation getByDemandeId(int demandeId) {
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "WHERE r.demande_id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, demandeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToReservation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. Get reservations by status
    public List<Reservation> getReservationsByStatus(ReservationStatus status) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "WHERE r.status = ? " +
                "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, status.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    // 5. Update reservation status
    public boolean updateStatus(int reservationId, ReservationStatus status) {
        String query = "UPDATE reservation SET status = ? WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, status.toString());
            ps.setInt(2, reservationId);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. Search reservations by code or destination
    public List<Reservation> searchReservations(String keyword) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT r.*, " +
                "d.title as demande_title, d.destination, " +
                "u1.username as client_username, " +
                "u2.username as agent_username " +
                "FROM reservation r " +
                "JOIN demande d ON r.demande_id = d.id " +
                "JOIN user u1 ON r.client_id = u1.id " +
                "JOIN user u2 ON r.agent_id = u2.id " +
                "WHERE r.reservation_code LIKE ? OR d.destination LIKE ? " +
                "ORDER BY r.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }
        return reservations;
    }

    // 7. Count statistics
    public int countByStatus(ReservationStatus status) {
        String query = "SELECT COUNT(*) FROM reservation WHERE status = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, status.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // 8. Get total revenue (sum of confirmed/completed reservations)
    public double getTotalRevenue() {
        String query = "SELECT SUM(total_price) FROM reservation WHERE status IN ('CONFIRMED', 'COMPLETED')";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // 9. Check if demande already has a reservation
    public boolean demandeHasReservation(int demandeId) {
        String query = "SELECT COUNT(*) FROM reservation WHERE demande_id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, demandeId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 10. Generate unique reservation code
    public String generateUniqueReservationCode() {
        String code;
        do {
            code = "R-" + System.currentTimeMillis() % 1000000; // Simple unique code
        } while (reservationCodeExists(code));
        return code;
    }

    private boolean reservationCodeExists(String code) {
        String query = "SELECT COUNT(*) FROM reservation WHERE reservation_code = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Helper method to map ResultSet to Reservation object
    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("id"));
        reservation.setReservationCode(rs.getString("reservation_code"));
        reservation.setTotalPrice(rs.getDouble("total_price"));
        reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        reservation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Create demande object
        Demande demande = new Demande();
        demande.setId(rs.getInt("demande_id"));
        demande.setTitle(rs.getString("demande_title"));
        demande.setDestination(rs.getString("destination"));
        reservation.setDemande(demande);

        // Create client object
        User client = new User();
        client.setId(rs.getInt("client_id"));
        client.setUsername(rs.getString("client_username"));
        reservation.setClient(client);

        // Create agent object
        User agent = new User();
        agent.setId(rs.getInt("agent_id"));
        agent.setUsername(rs.getString("agent_username"));
        reservation.setAgent(agent);

        return reservation;
    }
}