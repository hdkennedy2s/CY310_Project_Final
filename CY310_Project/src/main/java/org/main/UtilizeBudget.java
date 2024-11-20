package org.main;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class UtilizeBudget {

    //Specify database path
    private static final String DB_PATH = Paths.get(System.getProperty("user.home"), ".sqlite", "db", "cy310.db").toString();

    public static void utilizeBudget(int userId) {
        // Connect to the database and check if a budget exists for the user
        Map<String, Double> budgetCategories = getBudgetForUser(userId);

        //Ensure user has created a budget
        if (budgetCategories.isEmpty()) {
            System.out.println("No budget created.");
            return;
        }

        // Prompt the user for an amount to allocate across the budget
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter a positive amount to allocate across your budget categories:");
        double amount = getPositiveDouble(scanner);

        // Display the allocation of the entered amount across the budget categories
        displayBudgetAllocation(amount, budgetCategories);
    }

    private static Map<String, Double> getBudgetForUser(int userId) {
        Map<String, Double> budgetCategories = new HashMap<>();

        String sql = "SELECT category, percentage FROM budgets WHERE user_id = ?";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    double percentage = rs.getDouble("percentage");
                    budgetCategories.put(category, percentage);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching budget: " + e.getMessage());
        }

        return budgetCategories;
    }

    //Input Validation
    private static double getPositiveDouble(Scanner scanner) {
        while (true) {
            try {
                double value = Double.parseDouble(scanner.nextLine());
                if (value > 0) {
                    return value;
                } else {
                    System.out.println("Please enter a positive value.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    //Display user's budget allocation
    private static void displayBudgetAllocation(double amount, Map<String, Double> budgetCategories) {
        System.out.println("\n---- Budget Allocation ----");
        System.out.printf("Total Amount: $%.2f%n", amount);
        System.out.println("\nCategory Allocations:");

        for (Map.Entry<String, Double> entry : budgetCategories.entrySet()) {
            String category = entry.getKey();
            double percentage = entry.getValue();
            double allocation = (amount * percentage) / 100;

            System.out.printf("%s: %.2f%% - $%.2f%n", category, percentage, allocation);
        }

    }
}
