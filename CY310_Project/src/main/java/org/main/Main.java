package org.main;

import static org.main.Login.connect;
import static org.main.UtilizeBudget.utilizeBudget;
import static org.main.ViewBudget.viewBudget;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        connect(); //Connect to SQLite database
        mainMenu(); //Initialize menu
    }

    private static void mainMenu() {
        Scanner scanner = new Scanner(System.in);
        int userId = Login.login();

        while (true) {
            System.out.println("-----------------------------------");
            System.out.println("Please select an option: ");
            System.out.println("""
                1. Budget a paycheck
                2. Enter/Edit Budget Information
                3. View Budget Information
                4. Exit""");

            String mainMenuSelection = scanner.nextLine();

            switch (mainMenuSelection) {
                case "1": //Budget a paycheck
                    utilizeBudget(userId);
                    break;

                case "2": //Enter/Edit Budget Information
                    EnterBudget.enterBudget(userId);
                    break;

                case "3": //View Budget Information
                    viewBudget(userId);
                    break;

                case "4": //Exit
                    System.out.println("Goodbye.");
                    System.exit(0);
                    break;

                default: //Retry
                    System.out.println("Please enter a valid option.");
            }
        }
    }
}
