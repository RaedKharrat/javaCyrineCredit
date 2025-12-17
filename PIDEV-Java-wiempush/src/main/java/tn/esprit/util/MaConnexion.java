package tn.esprit.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MaConnexion {
    // Updated for your syrinecredit database
    final String URL = "jdbc:mysql://localhost:3306/syrinecredit";
    final String USR = "root";
    final String PWD = ""; // Your password here if you have one

    //att
    private Connection cnx;
    private static MaConnexion instance;

    //Constructor - Private for Singleton pattern
    private MaConnexion(){
        try {
            cnx = DriverManager.getConnection(URL, USR, PWD);
            System.out.println("✅ Connexion établie avec succès à la base 'syrinecredit'");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de connexion à la base de données:");
            e.printStackTrace();
            throw new RuntimeException("Impossible de se connecter à la base de données", e);
        }
    }

    public Connection getCnx() {
        return cnx;
    }

    public static MaConnexion getInstance() {
        if(instance == null)
            instance = new MaConnexion();

        return instance;
    }


}