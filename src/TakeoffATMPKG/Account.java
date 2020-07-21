package TakeoffATMPKG;

import java.util.LinkedList;
import java.util.Timer;

/*
Class: Machine
Description: Contains all information related to an individual customer's account, such as their account ID, PIN,
             balance, transaction history, and the session timer object, which controls how long the user has access to
             their account after authenticating themselves.
*/

public class Account {
    // Account balances contain dollars and cents, so the double value must be used.
    private double accountBalance;
    // Account ID is a number that exceeds the limit of an int data type, so a long must be used. None of the sample
    // data showed accounts with leading zeros, so this data type assumes that the accountID must start with a non-zero
    private long accountID;
    // Personal Identification Numbers are numeric, but leading zeros should not be discarded, so a String must be used
    private String pin;
    // The timer object is declared in each account and initialized when the account is authorized. This object is used
    // to delay the point at which users lose access.
    public Timer sessionTimer;

    /*
    Data Structure: transactionHistory
    Description: Stores all of the transaction history. When a user accesses their transaction history they want every
                 transaction. A linked list will let the machine traverse through each node, so it may print the data
                 stored within, until the end of the list has been reached.
    */
    private LinkedList<String> transactionHistory;

    /*
    Method: Account()
    Description: A constructor that takes in all of the information from the array of customer accounts
    Input:
        newAccountID - A unique value that represents the a bank account number
        newPIN - A confidential personal identification number
        startingBalance - The initial balance of the account
    Output: Creates an object of type Account and creates a linked list of the transaction history
    Returns: No return type (N/A for a constructor)
    */
    public Account (long newAccountID, String newPIN, double startingBalance){
        this.accountID = newAccountID;
        this.pin = newPIN;
        this.accountBalance = startingBalance;
        this.transactionHistory = new LinkedList<String>();
    }

    /*
    Method: getAccountID()
    Description: Retrieves the private accountID property
    Input: None
    Output: None
    Returns: The account ID as a long
    */
    public long getAccountID(){
        return accountID;
    }
    /*
    Method: getPin()
    Description: Retrieves the private pin property
    Input: None
    Output: None
    Returns: The account pin as a String
    */
    public String getPin() {
        return pin;
    }

    /*
    Method: getAccountBalance()
    Description: Retrieves the private accountBalance property
    Input: None
    Output: None
    Returns: The account balance as a double
    */
    public double getAccountBalance() {
        return accountBalance;
    }

    /*
    Method: setAccountBalance()
    Description: Mutator method that changes the value of the account object's private accountBalance property
    Input: A double value representing the new account balance
    Output: The accountBalance property of that account object is changed
    Returns: Void
    */
    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    /*
    Method: getTransactionHistory()
    Description: Retrieves the private transaction history data structure that is connected to each account
    Input: None
    Output: None
    Returns: The transaction history as a linked list of String objects
    */
    public LinkedList<String> getTransactionHistory() {
        return transactionHistory;
    }

    /*
    Method: updateTransactionHistory()
    Description: Mutator method that formats the provided information and stores it in a newly created node, which is
                 inserted at the head of the linked list representing that account object's transaction history.
    Input: transactionDateAndTime - A String represented the properly formatted date and time generated at the instant
                                    of the transaction.
           amount - A positive (deposit) or negative (withdrawal) amount as a double, since deposits can be checks that
                    include fractions of dollars
           newBalance - A newly calculated balance amount as a double, since the account balance value may include
           fractions of a dollar.
    Output: The transactionHistory linked list is prepended with the new data
    Returns: Void
    */
    public void updateTransactionHistory(String transactionDateAndTime, String amount, String newBalance) {
        String newData = transactionDateAndTime + " " + amount + " " + newBalance;
        this.transactionHistory.addFirst(newData);
    }
}
