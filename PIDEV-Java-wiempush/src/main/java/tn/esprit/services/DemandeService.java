package tn.esprit.services;

import tn.esprit.entities.Demande;
import tn.esprit.entities.User;
import tn.esprit.enums.DemandeStatus;
import tn.esprit.interfaces.IService;
import tn.esprit.util.MaConnexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DemandeService implements IService<Demande> {

    @Override
    public void add(Demande demande) {
        String query = "INSERT INTO demande (title, description, destination, budget, start_date, end_date, status, created_at, client_id, assigned_agent_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, demande.getTitle());
            ps.setString(2, demande.getDescription());
            ps.setString(3, demande.getDestination());
            ps.setDouble(4, demande.getBudget());
            ps.setDate(5, Date.valueOf(demande.getStartDate()));
            ps.setDate(6, Date.valueOf(demande.getEndDate()));
            ps.setString(7, demande.getStatus().toString());
            ps.setTimestamp(8, Timestamp.valueOf(demande.getCreatedAt())); // ADD THIS
            ps.setInt(9, demande.getClient().getId());

            if (demande.getAssignedAgent() != null) {
                ps.setInt(10, demande.getAssignedAgent().getId());
            } else {
                ps.setNull(10, Types.INTEGER);
            }

            ps.executeUpdate();

            // Get the generated ID
            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                demande.setId(generatedKeys.getInt(1));
            }

            System.out.println("✅ Demande added successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Demande demande) {
        String query = "UPDATE demande SET title = ?, description = ?, destination = ?, budget = ?, " +
                "start_date = ?, end_date = ?, status = ?, assigned_agent_id = ? WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, demande.getTitle());
            ps.setString(2, demande.getDescription());
            ps.setString(3, demande.getDestination());
            ps.setDouble(4, demande.getBudget());
            ps.setDate(5, Date.valueOf(demande.getStartDate()));
            ps.setDate(6, Date.valueOf(demande.getEndDate()));
            ps.setString(7, demande.getStatus().toString());

            if (demande.getAssignedAgent() != null) {
                ps.setInt(8, demande.getAssignedAgent().getId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.setInt(9, demande.getId());

            ps.executeUpdate();
            System.out.println("✅ Demande updated successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Demande demande) {
        String query = "DELETE FROM demande WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, demande.getId());
            ps.executeUpdate();
            System.out.println("✅ Demande deleted successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Demande> getAll() throws SQLException {
        List<Demande> demandes = new ArrayList<>();
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "ORDER BY d.created_at DESC";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Demande demande = mapResultSetToDemande(rs);
                demandes.add(demande);
            }
        }
        return demandes;
    }

    @Override
    public Demande getOne(int id) {
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "WHERE d.id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToDemande(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // SPECIAL METHODS FOR THE PLATFORM

    // 1. Get demandes for a specific client
    public List<Demande> getDemandesByClient(int clientId) throws SQLException {
        List<Demande> demandes = new ArrayList<>();
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "WHERE d.client_id = ? " +
                "ORDER BY d.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        }
        return demandes;
    }

    // 2. Get demandes assigned to a specific agent
    public List<Demande> getDemandesByAgent(int agentId) throws SQLException {
        List<Demande> demandes = new ArrayList<>();
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "WHERE d.assigned_agent_id = ? " +
                "ORDER BY d.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        }
        return demandes;
    }

    // 3. Get pending demandes (for agents to see new requests)
    public List<Demande> getPendingDemandes() throws SQLException {
        List<Demande> demandes = new ArrayList<>();
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "WHERE d.status = 'PENDING' " +
                "ORDER BY d.created_at DESC";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        }
        return demandes;
    }

    // 4. Assign demande to agent (when agent accepts)
    public boolean assignToAgent(int demandeId, int agentId) {
        String query = "UPDATE demande SET assigned_agent_id = ?, status = 'IN_PROGRESS' WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, agentId);
            ps.setInt(2, demandeId);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. Update status (confirm, reject, validate)
    public boolean updateStatus(int demandeId, DemandeStatus status) {
        String query = "UPDATE demande SET status = ? WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, status.toString());
            ps.setInt(2, demandeId);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 6. Get demandes by status
    public List<Demande> getDemandesByStatus(DemandeStatus status) throws SQLException {
        List<Demande> demandes = new ArrayList<>();
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "WHERE d.status = ? " +
                "ORDER BY d.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, status.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        }
        return demandes;
    }

    // 7. Search demandes by title or destination
    public List<Demande> searchDemandes(String keyword) throws SQLException {
        List<Demande> demandes = new ArrayList<>();
        String query = "SELECT d.*, u1.username as client_username, u2.username as agent_username " +
                "FROM demande d " +
                "LEFT JOIN user u1 ON d.client_id = u1.id " +
                "LEFT JOIN user u2 ON d.assigned_agent_id = u2.id " +
                "WHERE d.title LIKE ? OR d.destination LIKE ? " +
                "ORDER BY d.created_at DESC";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                demandes.add(mapResultSetToDemande(rs));
            }
        }
        return demandes;
    }

    // 8. Count statistics
    public int countByStatus(DemandeStatus status) {
        String query = "SELECT COUNT(*) FROM demande WHERE status = ?";

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

    // 9. Get total number of demandes
    public int getTotalDemandes() {
        String query = "SELECT COUNT(*) FROM demande";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Helper method to map ResultSet to Demande object
    private Demande mapResultSetToDemande(ResultSet rs) throws SQLException {
        Demande demande = new Demande();
        demande.setId(rs.getInt("id"));
        demande.setTitle(rs.getString("title"));
        demande.setDescription(rs.getString("description"));
        demande.setDestination(rs.getString("destination"));
        demande.setBudget(rs.getDouble("budget"));
        demande.setStartDate(rs.getDate("start_date").toLocalDate());
        demande.setEndDate(rs.getDate("end_date").toLocalDate());
        demande.setStatus(DemandeStatus.valueOf(rs.getString("status")));
        demande.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        // Create client object
        User client = new User();
        client.setId(rs.getInt("client_id"));
        client.setUsername(rs.getString("client_username"));
        demande.setClient(client);

        // Create agent object if exists
        int agentId = rs.getInt("assigned_agent_id");
        if (!rs.wasNull()) {
            User agent = new User();
            agent.setId(agentId);
            agent.setUsername(rs.getString("agent_username"));
            demande.setAssignedAgent(agent);
        }

        return demande;
    }
}