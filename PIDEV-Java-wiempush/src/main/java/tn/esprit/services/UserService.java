package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.enums.UserRole;
import tn.esprit.interfaces.IService;
import tn.esprit.util.MaConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    @Override
    public void add(User user) {
        // We'll use AuthService for registration
        System.out.println("Use AuthService.register() instead");
    }

    @Override
    public void update(User user) {
        String query = "UPDATE user SET username = ?, email = ?, role = ? WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().toString());
            ps.setInt(4, user.getId());

            ps.executeUpdate();
            System.out.println("User updated successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(User user) {
        String query = "DELETE FROM user WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            System.out.println("User deleted successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user ORDER BY created_at DESC";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(UserRole.valueOf(rs.getString("role")));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                users.add(user);
            }
        }
        return users;
    }

    @Override
    public User getOne(int id) {
        String query = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(UserRole.valueOf(rs.getString("role")));
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Special methods for our platform

    public List<User> getAgents() throws SQLException {
        List<User> agents = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role = 'AGENT' ORDER BY username";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User agent = new User();
                agent.setId(rs.getInt("id"));
                agent.setUsername(rs.getString("username"));
                agent.setEmail(rs.getString("email"));
                agent.setRole(UserRole.AGENT);
                agent.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                agents.add(agent);
            }
        }
        return agents;
    }

    public List<User> getClients() throws SQLException {
        List<User> clients = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role = 'CLIENT' ORDER BY username";

        try (Statement stmt = MaConnexion.getInstance().getCnx().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User client = new User();
                client.setId(rs.getInt("id"));
                client.setUsername(rs.getString("username"));
                client.setEmail(rs.getString("email"));
                client.setRole(UserRole.CLIENT);
                client.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                clients.add(client);
            }
        }
        return clients;
    }

    // Count statistics
    public int countByRole(UserRole role) {
        String query = "SELECT COUNT(*) FROM user WHERE role = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, role.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}