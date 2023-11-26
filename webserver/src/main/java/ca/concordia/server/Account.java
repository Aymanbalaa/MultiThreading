package ca.concordia.server;

import java.util.concurrent.Semaphore;

public class Account {
    private int balance;
    private int id;
    private final Semaphore semaphore = new Semaphore(1);

    public Account(int balance, int id) {
        this.balance = balance;
        this.id = id;
    }

    public int getBalance() {
        return balance;
    }

    public void withdraw(int amount) {
        try {
            semaphore.acquire();
            balance -= amount;
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle the exception according to your application's needs
        } finally {
            semaphore.release();
        }
    }

    public void deposit(int amount) {
        try {
            semaphore.acquire();
            balance += amount;
        } catch (InterruptedException e) {
            e.printStackTrace(); // Handle the exception according to your application's needs
        } finally {
            semaphore.release();
        }
    }
}
