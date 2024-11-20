package org.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.file.Paths;

public class ViewBudget {

    //Specify budget path
    private static final String DB_PATH = Paths.get(System.getProperty("user.home"), ".sqlite", "db", "cy310.db").toString();

    public static void viewBudget(int userId) {
        //Access budget table for specified user
        String querySQL = "SELECT category, percentage, income, paychecks FROM budgets WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement(querySQL)) {

            pstmt.setInt(1, userId); //View budget of current user
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n---- Current Budget ----");
                    System.out.printf("User ID: %d%n", userId);
                    double totalIncome = rs.getDouble("income");
                    int paychecks = rs.getInt("paychecks");
                    System.out.printf("Monthly Income: $%.2f%n", totalIncome);
                    System.out.printf("Paychecks Per Month: %d%n", paychecks);

                    System.out.println("\nCategory Allocations:");
                    do {
                        String category = rs.getString("category");
                        double percentage = rs.getDouble("percentage");
                        double allocation = (totalIncome * percentage) / 100;

                        System.out.printf("%s: %.2f%% - $%.2f%n", category, percentage, allocation);
                    } while (rs.next());

                } else {
                    System.out.println("No budget found for this user.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching budget: " + e.getMessage());
        }
    }
}
