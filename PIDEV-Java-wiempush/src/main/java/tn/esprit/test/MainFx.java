package tn.esprit.test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFx extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the Login screen instead of AjouterInfoMedicaux
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 600); // Set size for Login
            primaryStage.setTitle("Travel Agency - Connexion");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

            System.out.println("🚀 Application started with Login screen!");

        } catch (IOException e) {
            System.err.println("❌ Error loading Login.fxml:");
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        // Test database connection first
        System.out.println("=== Travel Agency JavaFX Application ===\n");

        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("✅ MySQL Driver loaded successfully");

            // Launch JavaFX application
            launch(args);

        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERROR: MySQL Driver not found!");
            System.err.println("Please add mysql-connector-java-8.0.33.jar to your project.");
            System.err.println("\nDownload from: https://dev.mysql.com/downloads/connector/j/");
            e.printStackTrace();
        }
    }
}