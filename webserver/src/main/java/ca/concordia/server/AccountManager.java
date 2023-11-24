package ca.concordia.server;

import java.util.*;

public class AccountManager {

    public static final Map<Integer, Account> accounts = new HashMap<>();

    static {
        // Initialize accounts from a file or any other source if needed
        accounts.put(123, new Account(4000, 123));
        accounts.put(321, new Account(5000, 321));
        accounts.put(432, new Account(2000, 432));
    }

    public static synchronized void transferFunds(String fromAccountId, int amount, String toAccountId, int toAmount) {
        Account fromAccount = accounts.get(Integer.parseInt(fromAccountId));
        Account toAccount = accounts.get(Integer.parseInt(toAccountId));

        if (fromAccount != null && toAccount != null) {
            // Ensure sufficient balance before transferring funds
            if (fromAccount.getBalance() >= amount) {
                fromAccount.withdraw(amount);
                toAccount.deposit(toAmount);
            }
        }
    }
}
