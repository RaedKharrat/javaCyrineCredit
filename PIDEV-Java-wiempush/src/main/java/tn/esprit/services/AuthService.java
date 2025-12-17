package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.enums.UserRole;
import tn.esprit.util.MaConnexion;
import java.sql.*;
import java.util.Base64;

public class AuthService {

    // Register a new user
    public boolean register(User user) {
        String query = "INSERT INTO user (username, email, password, role) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());

            // In production, use BCrypt! This is just for demo
            String hashedPassword = Base64.getEncoder().encodeToString(user.getPassword().getBytes());
            ps.setString(3, hashedPassword);

            ps.setString(4, user.getRole().toString());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Login user
    public User login(String username, String password) {
        String query = "SELECT * FROM user WHERE username = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Get stored password
                String storedPassword = rs.getString("password");

                // Decode and compare (in production, use BCrypt.checkpw)
                String encodedInput = Base64.getEncoder().encodeToString(password.getBytes());

                if (storedPassword.equals(encodedInput)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setRole(UserRole.valueOf(rs.getString("role")));
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Check if username exists
    public boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM user WHERE username = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Check if email exists
    public boolean emailExists(String email) {
        String query = "SELECT COUNT(*) FROM user WHERE email = ?";

        try (PreparedStatement ps = MaConnexion.getInstance().getCnx().prepareStatement(query)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}