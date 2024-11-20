package org.main;

import java.io.File;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Scanner;

public class Login {
    static {
        try {
            // Explicitly load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    private static final String DB_PATH = Paths.get(System.getProperty("user.home"), ".sqlite", "db", "cy310.db").toString();

    public static void connect() {
        // Check if the database exists, otherwise create it
        File dbFile = new File(DB_PATH);
        if (!dbFile.exists()) {
            // Create the necessary directories
            new File(dbFile.getParent()).mkdirs();
            System.out.println("Database not found. Creating new database...");
            createNewDatabase();
            createTable();
        } else {
            System.out.println("Database found.");
        }
    }

    private static void createNewDatabase() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + Login.DB_PATH)) {
            if (conn != null) {
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void createTable() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
             id INTEGER PRIMARY KEY AUTOINCREMENT,
             username TEXT NOT NULL UNIQUE,
             password TEXT NOT NULL,
             salt TEXT NOT NULL
            );""";

        String createBudgetsTable = """
            CREATE TABLE IF NOT EXISTS budgets (
             id INTEGER PRIMARY KEY AUTOINCREMENT,
             user_id INTEGER NOT NULL,
             category TEXT NOT NULL,
             percentage REAL NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
             income REAL NOT NULL,
             paychecks INTEGER NOT NULL,
             FOREIGN KEY (user_id) REFERENCES users (id)
            );""";

        String addIncomeColumn = "ALTER TABLE budgets ADD COLUMN income REAL NOT NULL DEFAULT 0";  // To add income column if missing

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             Statement stmt = conn.createStatement()) {

            // Create users table
            stmt.execute(createUsersTable);

            // Create budgets table if it doesn't exist
            stmt.execute(createBudgetsTable);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    private static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hash = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static void addUser(String username, String password) {
        String salt = generateSalt();

        try {
            String hashedPassword = hashPassword(password, salt);

            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.setString(3, salt);
                pstmt.executeUpdate();
                System.out.println("User added successfully.");
            } catch (SQLException e) {
                System.out.println("Error inserting user: " + e.getMessage());
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error hashing password: " + e.getMessage());
        }
    }

    public static Integer verifyUser(String username, String inputPassword) {
        String sql = "SELECT id, password, salt FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    String storedHash = rs.getString("password");
                    String storedSalt = rs.getString("salt");

                    String inputHash = hashPassword(inputPassword, storedSalt);

                    if (storedHash.equals(inputHash)) {
                        return userId;  // Login successful, return user ID
                    }
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            System.out.println("Error verifying user: " + e.getMessage());
        }
        return null;  // Login failed
    }

    public static Integer login() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("-----------------------------------");
            System.out.println("Please select an option:");
            System.out.println("""
                    1. Login as existing user
                    2. Create User
                    0. Exit
                    """);

            String loginMenuSelection = scanner.nextLine();

            switch (loginMenuSelection) {
                case "1":
                    System.out.println("Enter username: ");
                    String username = scanner.nextLine();
                    System.out.println("Enter password: ");
                    String inputPassword = scanner.nextLine();

                    Integer userId = verifyUser(username, inputPassword);
                    if (userId != null) {
                        System.out.println("Login successful.");
                        return userId;  // Return user ID after successful login
                    } else {
                        System.out.println("Login failed. Try again.");
                    }
                    break;

                case "2":
                    System.out.println("Enter username: ");
                    username = scanner.nextLine();
                    System.out.println("Enter password: ");
                    inputPassword = scanner.nextLine();

                    addUser(username, inputPassword);
                    break;

                case "0":
                    System.out.println("Goodbye.");
                    System.exit(0);

                default:
                    System.out.println("Please enter a valid option.");
                    break;
            }
        }
    }
}