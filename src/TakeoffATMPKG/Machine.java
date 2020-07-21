package TakeoffATMPKG;

import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.time.LocalDateTime;
import java.util.Locale;
import java.text.NumberFormat;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;



/*
Date: July 20, 2020
Programmer: Krishan Agarwal
Issues:
Java Version: Java 8
Assumptions: The bank account database is entered into the system manually, not read from a file or otherwise
             accessed externally.
Notes:
Program Description: A program that allows a user to interact with an ATM until the END command is sent.
*/

/*
Class: Machine
Description: Contains main method, which includes methods used to interact with the ATM
*/


public class Machine {
    // The total number of sample accounts provided in the sample data
    final static int NUMBER_OF_ACCOUNTS = 4;
    // Total cash value stored with the machine. ATMs do not dispense coins, so data type must be an integer
    // Initial value set to $10,000, per the specification
    static int totalCash = 10000;
    // Session time, two minutes per the specification
    final static int SESSION_TIME = 2;
    // The TimerTask object (below) measures time in milliseconds and the session time is in minutes
    final static int MILLISECONDS_PER_MINUTE = 60000;
    // Formats currency to United States Dollar, so ####.#### will become $#,###.##
    // Applies to all transaction messages sent to the user
    static NumberFormat usd = NumberFormat.getCurrencyInstance(Locale.US);
    // Applies to all transactions sent to the transaction history
    // Formats currency from ####.#### to ####.##
    static DecimalFormat decimalFormat = new DecimalFormat("#0.00");


    private static Account[] customers = new Account[NUMBER_OF_ACCOUNTS];

    /*
       When a user has gained the necessary authorization, the index position of that account within the array is
       stored to improve time performance for other interactions, such as WITHDRAW, DEPOSIT, etc. Variable is
       assigned a negative value when account access expires via timeout or logout.
    */
    static int indexOfAccountAccessed = -1;

    public static void main(String[] args) {
        final int ACCOUNT_ID_LENGTH = 10;
        final int PIN_LENGTH = 4;
        final int MAX_CASH_IN_ATM = totalCash;
        Scanner keyboard = null;
        String input = null;
        keyboard = new Scanner(System.in);

        //Fills static array declared above
        customers[0] = new Account(2859459814L, "7386", 10.24);
        customers[1] = new Account(1434597300L, "4557", 90000.55);
        customers[2] = new Account(7089382418L, "0075", 0.00);
        customers[3] = new Account(2001377812L, "5950", 60.00);

        while (true) {
            //Sample data didn't show accounts with leading zeros, so I assume that AccountID can be stored as a long
            int commaIndex;
            int inputLength;
            long inputAccountNumber = 0L;
            String inputPIN;




            System.out.println("Please enter your account's identification number, a comma, and then your PIN\n" +
                    "Example: 1234567890,1234\nOr type END to end this program");

            // Validate user input.
            input = keyboard.nextLine();
            input = input.trim();
            // Instead of calling the length() method repeatedly throughout validation, store the length value as an int
            inputLength = input.length();

            try {
                // If the user enters in an empty string, inform them and repeat the loop
                if (inputLength < 1) {
                    System.out.println("Input error: " + input);
                    System.out.println("Authorization required.\n\n");
                    continue;
                }

                // Check for the END command. If entered, print a message and terminate the program
                if (input.equalsIgnoreCase("end")){
                    end();
                }

                // If there is no comma, or of there is nothing after the comma, inform the user and repeat the loop
                commaIndex = input.indexOf(",");
                if (commaIndex == -1 || commaIndex == inputLength - 1) {
                    System.out.println("Incorrect input: " + input);
                    System.out.println("Authorization required.\n\n");
                    continue;
                }

                /*
                   If the account number is not in the correct format, throw an exception and restart the while loop
                   Verify that the portion of the input representing the Account ID is the correct length, which is 10
                   digits. Since the String is an zero-based array, the comma will be in the 11th position, which is
                   index 10.
                 */
                if (commaIndex != ACCOUNT_ID_LENGTH) {
                    throw new Exception("This Account ID is not the correct length, it should be 10 digits");
                }

                /*
                   Verify that the Account ID is a number. If the input value is not a number, it will throw a
                   NumberFormatException, which is caught below
                */
                inputAccountNumber = Long.parseLong(input.substring(0, commaIndex));

                // Assign the inputPIN string to the portion of the input from after the comma to the end of the input
                inputPIN = input.substring(commaIndex + 1, inputLength);

                // Delete all non-numeric characters
                inputPIN = inputPIN.replaceAll("[^0-9]", "");

                // Check to see if what remains is the correct length for a PIN, as defined above
                if (inputPIN.length() != PIN_LENGTH){
                    throw new Exception("This PIN is not the correct length, it should be 4 digits");
                }
            }

            // Handle exception if the input is NOT a number. This also takes care of commands, such as deposit,
            // withdraw, etc. for users who have not accessed any account yet
            catch (NumberFormatException e) {
                System.out.println("Invalid input: " + input + ".");
                System.out.println("Authorization required.\n\n");
                continue;
            }

            // Handle a generic exception, such as when the length of the input does not match that of the account ID
            catch (Exception e){
                System.out.println(e.getMessage() + "\n\n");
                System.out.println("Authorization required.\n\n");
                continue;
            }

            // Send information provided by the user to the authorize method. Print the message that is returned
            System.out.println(authorize(inputAccountNumber, inputPIN));

            /* If the user is unauthorized, start the loop over again to prompt them for their Account ID and PIN. The
               value of indexOfAccountAccessed is set to the array index of an account only if the user is authorized
               to access that account. An array index can never be negative, so if indexOfAccountAccessed is negative,
               the user must not have access to an account.
            */
            if (indexOfAccountAccessed == -1){
                continue;
            }

            // This is the internal while loop. The user will only be able to access commands such as withdraw, deposit,
            // etc. while in this loop. The commands will become inaccessible to the user and they will break this loop
            // if their session time expires, they elect to log out, or they terminate the program
            while(indexOfAccountAccessed != -1){
                // The digit entered by the user to indicate their next action
                int selectionValue;

                System.out.println("Please type the number for the next action you would like to take:\n" +
                        "[1] Withdraw Money\n" +
                        "[2] Deposit Money\n" +
                        "[3] View Account Balance\n" +
                        "[4] View Transaction History\n" +
                        "[5] Log Out\n" +
                        "[6] Terminate Program\n");

                // Validate entry by first determining if they entered anything, and then verifying that what they
                // entered is single digit
                try {
                    if (input.length() == 0){
                        throw new Exception("Please enter one digit only. Example: 4\n\n");
                    }
                    input = keyboard.nextLine();
                    selectionValue = Integer.parseInt(input);
                    if (selectionValue < 1 || selectionValue > 6){
                        throw new Exception("Please select a value between one and six by entering just that digit\n\n");
                    }
                }
                catch (NumberFormatException e){
                    System.out.println("That doesn't appear to be a number. Please enter one digit only\n\n");
                    continue;
                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                    continue;
                }

                // Take the digit they entered and act on the user's choice. Each case corresponds to menu above
                switch (selectionValue){
                    // Withdraw Money
                    case 1:
                        int numberOfTwenties;
                        System.out.println("Please enter the number of $20 bills you would like to withdraw. " +
                                "For example, '4' will give you $80\n");
                        try{
                            numberOfTwenties = Integer.parseInt(keyboard.nextLine());

                            if (numberOfTwenties < 0){
                                throw new Exception("You cannot withdraw a negative amount. Consider depositing\n\n");
                            }
                        }
                        catch (NumberFormatException e){
                            System.out.println("That doesn't appear to be a valid number\n\n");
                            break;
                        }

                        catch (Exception e){
                            System.out.println(e.getMessage());
                            break;
                        }

                        // Withdrawal amount must be less than or equal to the total possible amount stored in the ATM
                        if (numberOfTwenties <= (MAX_CASH_IN_ATM / 20)){
                            System.out.println(withdraw(numberOfTwenties));
                        } else {
                            System.out.println("That is more money than the ATM can hold\n\n");
                        }
                        break;

                    // Deposit Money
                    case 2:
                        double depositValue;
                        System.out.println("Please enter the amount you would like to deposit\n\n");
                        try {
                            depositValue = Double.parseDouble(keyboard.nextLine());

                            if (depositValue < 0){
                                throw new Exception("You cannot deposit a negative amount. Consider withdrawing\n\n");
                            }

                            // Ensures number is two decimal places. Must be cast from float to double
                            depositValue = (double) Math.round(depositValue * 100) / 100;
                        }
                        catch (NumberFormatException e){
                            System.out.println("That doesn't appear to be a valid number\n\n");
                            break;
                        }
                        catch (Exception e){
                            System.out.println(e.getMessage());
                            break;
                        }

                        System.out.println(deposit(depositValue));
                        break;

                    // View Account Balance
                    case 3:
                        System.out.println(balance());
                        break;

                    // View Transaction History
                    case 4:
                        history();
                        break;

                    // Log Out
                    case 5:
                        logout();
                        break;

                    // Terminate Program
                    case 6:
                        end();
                        break;

                    default:
                        break;
                }
            }

        }
    }










    /*
    Method: authorize()
    Description: Received user inputted information, which has been formatted into an Account ID and PIN. Searches the
                 customer account data structure to verify that there is an account with that Account ID and that said
                 account has the same PIN that the user entered.
    Input:
        inputAccountID - The account ID number that the user entered
        inputPIN - The PIN that was entered by the user
    Output: Starts the accountAccessibleTimer countdown timer
    Returns: A String containing a message, which is defined in the the specification document.
    */
    public static String authorize(long inputAccountID, String inputPIN){
        // Output value if the account authorization is successful
        String authorized = (inputAccountID + " successfully authorized.\n\n");
        // Output value if the account authorization is unsuccessful
        String unauthorized = ("Authorization failed.\n\n");

        // Linear search of account objects stored in customers array
        for(int i = 0; i < customers.length; i++) {
            // Check to see if the element with an Account ID that matches the input Account ID is present
            if (customers[i].getAccountID() == inputAccountID) {

                // Check to see if the account with the matching Account ID has a PIN that matches the input PIN
                if (customers[i].getPin().equals(inputPIN)) {
                    // The Account ID and its corresponding PIN match the input Account ID and input PIN
                    indexOfAccountAccessed = i;
                    // Two minute session timer started
                    accountAccessTimer();
                    return authorized;
                }
            }
        }
        // No Account ID matches the input Account ID and/or no PIN matches the input PIN
        return unauthorized;
    }

    /*
    Method: withdraw()
    Description: The user can withdraw money from the ATM. If the ATM has enough money, the user will receive it and
                 both the user's account balance and the ATM's total cash will be updated. The user will receive a
                 message telling them what happened.
    Input:
        numberOfTwenties - The number of $20 bills that the user would like to withdraw
    Output: Both the user's account balance, transaction history, and the ATM's total cash may be changed
    Returns: String - Message to user
    */
    public static String withdraw(int numberOfTwenties){
        double balance = customers[indexOfAccountAccessed].getAccountBalance();

        // If the account is already overdrawn
        if (balance <= 0) {
            return "Your account is overdrawn! You may not make withdrawals at this time.\n\n";
        }

        // The withdrawal value is the actual amount of money being removed from the account, while the value is the
        // argument representing the number of $20 bills
        double withdrawalValue = numberOfTwenties * 20;

        // The machine has enough money. Allow the user to withdraw the money and update their account balance
        if(totalCash > withdrawalValue){
            // Update the value of balance variable
            balance -= withdrawalValue;

            // Update the value of the account balance
            customers[indexOfAccountAccessed].setAccountBalance(balance);

            // Create a formatted timestamp
            String timePattern = "yyyy-MM-dd HH:mm:ss";
            DateTimeFormatter timeColonFormatter = DateTimeFormatter.ofPattern(timePattern);
            LocalDateTime localDate = LocalDateTime.now();
            String timeStamp = (timeColonFormatter.format(localDate));

            // Update the amount of money stored in the ATM
            totalCash -= withdrawalValue;

            // Log the transaction. Format the withdrawal value so it is negative
            customers[indexOfAccountAccessed].updateTransactionHistory(timeStamp,
                    decimalFormat.format(withdrawalValue - (2 * withdrawalValue)), decimalFormat.format(balance));

            // New two minute session timer started
            renewAccessTimer();
        }
        // If the machine has cash, but not enough to dispense. This ATM can only dispense $20 bills, so it must have
        // at least one $20 available for withdrawal
        else if (totalCash >= 20){
            return "Unable to dispense full amount requested at this time.\n\n";
        }
        // If there is no money in the machine
        else {
            return "Unable to process your withdrawal at this time.\n\n";
        }

        // If the account the user is accessing NOT overdrawn. The balance has been updated to reflect the withdrawal
        if (balance >= 0){
            return "Amount dispensed: " + usd.format(withdrawalValue) + "\n\n" +
                    "Current balance: " + usd.format(balance) + "\n\n";
        }
        // If the account the user is accessing has less zero dollars after the withdrawal
        else {
            // Five dollar fee
            balance -= 5;
            customers[indexOfAccountAccessed].setAccountBalance(balance);
            return "Amount dispensed: " + usd.format(withdrawalValue) + "\n\n" +
                    "You have been charged an overdraft fee of $5. Current balance: -" +
                    usd.format(Math.abs(balance)) + "\n\n";
        }
    }

    /*
    Method: deposit()
    Description: The user can deposit money from the ATM. The total cash stored within the ATM will not be updated
                 because the deposited money has to be verified by the bank.* The user's account balance will be updated
                 and they will receive a message informing them about what happened.

                 *If the deposit is a check, it cannot be dispensed to other customers. If the bills are old or damaged
                 they must be taken out of circulation.
    Input:
        value - The amount of money that they want to add to their account. This is a double because they might be
                depositing a check that has a partial dollar amount
    Output: The user's account balance and transaction history may be changed
    Returns: String - Message to user
    */
    public static String deposit(double value){
        double balance = customers[indexOfAccountAccessed].getAccountBalance();
        // Update the value of balance variable
        balance += value;

        // Update the value of the account balance
        customers[indexOfAccountAccessed].setAccountBalance(balance);

        // Create a formatted timestamp
        String timePattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter timeColonFormatter = DateTimeFormatter.ofPattern(timePattern);
        LocalDateTime localDate = LocalDateTime.now();
        String timeStamp = (timeColonFormatter.format(localDate));

        // Log the transaction
        customers[indexOfAccountAccessed].updateTransactionHistory(timeStamp, decimalFormat.format(value),
                decimalFormat.format(balance));

        // New two minute session timer started
        renewAccessTimer();

        if (balance >= 0) {
            return "Current balance: " + usd.format(balance) + "\n\n";
        }
        return "Current balance: -" + usd.format(Math.abs(balance)) + "\n\n";
    }

    /*
    Method: balance()
    Description: Returns a message (defined in the specification)
    Input: None
    Output: None
    Returns: A message (defined in the specification) containing the user's account balance
    */
    public static String balance(){
        // New two minute session timer started
        renewAccessTimer();

        double balance = customers[indexOfAccountAccessed].getAccountBalance();

        if (balance >= 0){
            return "Current balance: " + usd.format(balance) + "\n\n";
        }
        return "Current balance: -" + usd.format(Math.abs(balance)) + "\n\n";
    }

    /*
    Method: history()
    Description: Outputs a message containing the of the transaction history in the
                 correct format (defined in the specification;
    Input: None
    Output: A message containing the user's transaction history <date> <time> <amount> <balance after transaction> for
            example "2020-02-04 13:04:22 -20.00 140.67" in reverse chronological order, if a history exists.
    Returns: None
    */
    public static void history(){
        if (customers[indexOfAccountAccessed].getTransactionHistory().isEmpty()){
            System.out.println("No history found\n\n");
        } else {
            for (Iterator i = customers[indexOfAccountAccessed].getTransactionHistory().iterator(); i.hasNext();) {
                System.out.println(i.next());
            }
            System.out.print("\n\n");
        }
    }

    /*
    Method: logout()
    Description: The user loses access to their account
    Input: None
    Output: The reference variable for the session timer stored in the account object is set to null, making it elible
            for garbage collection.
            A message (defined in the specification) is displayed for the user to see
            The indexOfAccountAccessed is reset to its initial negative value.
    Returns: None
    */
    public static void logout(){
        if (indexOfAccountAccessed == -1){
            System.out.println("No account is currently authorized.");
        }
        customers[indexOfAccountAccessed].sessionTimer = null;
        System.out.println("Account " + customers[indexOfAccountAccessed].getAccountID() +" logged out.\n\n");
        indexOfAccountAccessed = -1;
    }

    /*
    Method: end()
    Description: The user terminates the program.
    Input: None
    Output: A message indicating that the program is shutting down and a system exit code.
    Returns: None
    */
    public static void end(){
        System.out.println("\nThank you for using our service. Goodbye");
        System.exit(0);
    }

    /*
    Method: accountAccessTimer()
    Description: The timer object stored within the account object, which is currently accessed, is set to 120,000
                 milliseconds (two minutes) and the countdown is started. At the end of two minutes, the value of
                 indexOfAccountAccessed is set to a negative number, which prevents the user from accessing their account
    Input: None
    Output: A message indicating that the session time has expired and the indexOfAccountAccessed is set to -1
    Returns: None
    */
    public static void accountAccessTimer(){
        customers[indexOfAccountAccessed].sessionTimer = new Timer();

        TimerTask endAccess = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Your session time has expired\n\n");
                indexOfAccountAccessed = -1;
            }
        };

        // Session time is defined above as 2 (minutes) and there are 60,000 milliseconds in a minute. This method takes
        // its delay argument in units of milliseconds.
        customers[indexOfAccountAccessed].sessionTimer.schedule(endAccess, (SESSION_TIME * MILLISECONDS_PER_MINUTE));
    }

    /*
    Method: renewAccessTimer()
    Description: A new timer object is created and the reference variable for the old timer object is set to refer to
                 the new timer object. Since there is nothing referencing the old timer object, it is eligible for
                 garbage collection. The new timer object gives the user two minutes of access. A new timer object
                 must be created because the countdown timer of the old object cannot be updated or reset.
    Input: None
    Output: A message indicating that the program is shutting down and a system exit code.
    Returns: None
    */
    public static void renewAccessTimer(){
        Timer newSession = new Timer();
        TimerTask endAccess = new TimerTask() {
            @Override
            public void run() {
                if (indexOfAccountAccessed >= 0) {
                    System.out.println("Your session time has expired\n\n");
                    indexOfAccountAccessed = -1;
                }
            }
        };

        newSession.schedule(endAccess, (SESSION_TIME * MILLISECONDS_PER_MINUTE));
        customers[indexOfAccountAccessed].sessionTimer = newSession;
        newSession = null;
    }
}