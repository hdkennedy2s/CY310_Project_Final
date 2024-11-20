package org.main;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

public class EnterBudget {

    //Initiate Variables
    private static double monthlyIncome;
    private static int paychecksPerMonth;
    private static final Map<String, Double> budgetCategories = new HashMap<>();
    private static final String DB_PATH = Paths.get(System.getProperty("user.home"), ".sqlite", "db", "cy310.db").toString();

    public static void enterBudget(int userId) {
        Scanner scanner = new Scanner(System.in);

        //Enter monthly income
        System.out.println("Enter your total monthly income: ");
        monthlyIncome = getPositiveDouble(scanner);

        //Enter how often paychecks are received
        System.out.println("How many paychecks do you receive per month? ");
        paychecksPerMonth = getPositiveInt(scanner);

        //Enter budget categories and percentages
        System.out.println("Enter budget categories and their percentages (they must total 100%):");
        inputBudgetCategories(scanner);

        //Save the budget to the database
        saveBudgetToDatabase(userId);

        //Display the budget summary
        displayBudgetSummary();
    }

    //Validate user input
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

    private static int getPositiveInt(Scanner scanner) {
        while (true) {
            try {
                int value = Integer.parseInt(scanner.nextLine());
                if (value > 0) {
                    return value;
                } else {
                    System.out.println("Please enter a positive integer.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter an integer.");
            }
        }
    }

    private static void inputBudgetCategories(Scanner scanner) {
        double totalPercentage = 0;

        while (true) {
            System.out.println("Enter a category name (or type 'done' to finish): ");
            String category = scanner.nextLine();

            if (category.equalsIgnoreCase("done")) {
                if (totalPercentage == 100) {
                    break;
                } else {
                    System.out.printf("Your percentages total %.2f%%. They must total 100%%.%n", totalPercentage);
                    continue;
                }
            }

            System.out.println("Enter the percentage for " + category + ": ");
            double percentage = getPositiveDouble(scanner);

            if (totalPercentage + percentage > 100) {
                System.out.printf("Adding %.2f%% exceeds 100%%. Try again.%n", percentage);
            } else {
                budgetCategories.put(category, percentage);
                totalPercentage += percentage;
                System.out.printf("Added %s: %.2f%% (Total: %.2f%%)%n", category, percentage, totalPercentage);
            }
        }
    }

    private static void saveBudgetToDatabase(int userId) {
        String deleteSQL = "DELETE FROM budgets WHERE user_id = ?";
        String insertSQL = "INSERT INTO budgets (user_id, category, percentage, income, paychecks) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSQL);
             PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

            // Clear old budget for the user
            deleteStmt.setInt(1, userId);
            deleteStmt.executeUpdate();

            // Insert new budget data
            for (Map.Entry<String, Double> entry : budgetCategories.entrySet()) {
                insertStmt.setInt(1, userId);
                insertStmt.setString(2, entry.getKey());
                insertStmt.setDouble(3, entry.getValue());
                insertStmt.setDouble(4, monthlyIncome);
                insertStmt.setInt(5, paychecksPerMonth);
                insertStmt.executeUpdate();
            }

            System.out.println("Budget saved successfully.");
        } catch (SQLException e) {
            System.out.println("Error saving budget: " + e.getMessage()); //Error message display
        }
    }

    public static void displayBudgetSummary() {
        System.out.println("\n---- Budget Summary ----");
        System.out.printf("Monthly Income: $%.2f%n", monthlyIncome);
        System.out.printf("Paychecks Per Month: %d%n", paychecksPerMonth);
        System.out.println("\nCategory Allocations:");

        double paycheckAmount = monthlyIncome / paychecksPerMonth;

        for (Map.Entry<String, Double> entry : budgetCategories.entrySet()) {
            String category = entry.getKey();
            double percentage = entry.getValue();
            double monthlyAllocation = (monthlyIncome * percentage) / 100;
            double perPaycheckAllocation = monthlyAllocation / paychecksPerMonth;

            System.out.printf("%s: %.2f%% (Monthly: $%.2f, Per Paycheck: $%.2f)%n",
                    category, percentage, monthlyAllocation, perPaycheckAllocation);
        }

    }
}
