package ca.concordia.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebServer {

    private ExecutorService executorService;

    public WebServer(int poolSize) {
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }
    private static Map<Integer, Account> accountMap = new HashMap<>();
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected...");

            // Use a worker thread to handle the connection
            executorService.submit(() -> handleConnection(clientSocket));
        }
    }

    private void handleConnection(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            String request = in.readLine();
            if (request != null) {
                if (request.startsWith("GET")) {
                    handleGetRequest(out);
                } else if (request.startsWith("POST")) {
                    handlePostRequest(in, out);
                }
            }

            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(OutputStream out) throws IOException {
        System.out.println("Handling GET request");
        String response = "HTTP/1.1 200 OK\r\n\r\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<title>Concordia Transfers</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>Welcome to Concordia Transfers</h1>\n" +
                "<p>Select the account and amount to transfer</p>\n" +
                "\n" +
                "<form action=\"/submit\" method=\"post\">\n" +
                "        <label for=\"account\">Account:</label>\n" +
                "        <input type=\"text\" id=\"account\" name=\"account\"><br><br>\n" +
                "\n" +
                "        <label for=\"value\">Value:</label>\n" +
                "        <input type=\"text\" id=\"value\" name=\"value\"><br><br>\n" +
                "\n" +
                "        <label for=\"toAccount\">To Account:</label>\n" +
                "        <input type=\"text\" id=\"toAccount\" name=\"toAccount\"><br><br>\n" +
                "\n" +
                "        <input type=\"submit\" value=\"Submit\">\n" +
                "    </form>\n" +
                "</body>\n" +
                "</html>\n";
        out.write(response.getBytes());
        out.flush();
    }



    private void handlePostRequest(BufferedReader in, OutputStream out) throws IOException {
        System.out.println("Handling post request");
        StringBuilder requestBody = new StringBuilder();
        int contentLength = 0;
        String line;

        // Read headers to get content length
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length")) {
                contentLength = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
            }
        }

        // Read the request body based on content length
        for (int i = 0; i < contentLength; i++) {
            requestBody.append((char) in.read());
        }

        System.out.println(requestBody.toString());
        // Parse the request body as URL-encoded parameters
        String[] params = requestBody.toString().split("&");
        String account = null, value = null, toAccount = null, toValue = null;

        for (String param : params) {
            String[] parts = param.split("=");
            if (parts.length == 2) {
                String key = URLDecoder.decode(parts[0], "UTF-8");
                String val = URLDecoder.decode(parts[1], "UTF-8");

                switch (key) {
                    case "account":
                        account = val;
                        break;
                    case "value":
                        value = val;
                        break;
                    case "toAccount":
                        toAccount = val;
                        break;
                }
            }
        }

        String responseContent = "<html><body><h1>Thank you for using Concordia Transfers</h1>";
        // Check if both accounts exist
        if (account != null && toAccount != null && value != null) {
            boolean sourceAccountExists = doesAccountExist(Integer.parseInt(account));
            boolean targetAccountExists = doesAccountExist(Integer.parseInt(toAccount));
            if (!sourceAccountExists) {
                responseContent += "<h2>Source Account does not exist</h2>";
                System.out.println("Account does not exist");
            } else if (!targetAccountExists) {
                responseContent += "<h2>Destination Account does not exist</h2>";
                System.out.println("Destination Account does not exist");
            } else {
                Account source = accountMap.get(Integer.parseInt(account));
                Account destination = accountMap.get(Integer.parseInt(toAccount));
                int transferValue = Integer.parseInt(value);
                if (transferValue < 0) {
                    responseContent += "<h2>Cannot transfer negative sum</h2>";
                    System.out.println("Cannot transfer negative sum");
                } else if (transferValue > source.getBalance()) {
                    responseContent += "<h2>No sufficient funds in Source Account</h2>";
                } else {
                    source.withdraw(transferValue);
                    destination.deposit(transferValue);
                    printMap();

                    responseContent +=
                            "<h2>Received Form Inputs:</h2>" +
                                    "<p>Source Account: " + account + "</p>" +
                                    "<p>Value: " + value + "</p>" +
                                    "<p>Destination Account: " + toAccount + "</p>" +
                                    "<p>Source Account New Balance: " + source.getBalance() + "</p>" +
                                    "<p>Destination Account New Balance: " + destination.getBalance() + "</p>";
                }
            }
        }

        // Create the response
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: " + responseContent.length() + "\r\n" +
                "Content-Type: text/html\r\n\r\n" +
                responseContent + "</body></html>";

        out.write(response.getBytes());
        out.flush();
    }

    public static Map<Integer, Account> readFile(String filePath) {
        Map<Integer, Account> accountMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    // Skip the first line (header)
                    firstLine = false;
                    continue;
                }

                // Split the line into account ID and balance
                String[] parts = line.split(",");
                int accountId = Integer.parseInt(parts[0].trim());
                int balance = Integer.parseInt(parts[1].trim());

                // Create an Account object and put it into the HashMap
                Account account = new Account(balance, accountId);
                accountMap.put(accountId, account);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }

        return accountMap;
    }

    // check if account exists
    public boolean doesAccountExist(int accountId) {
        return accountMap.containsKey(accountId);
    }

    public static void printMap() {
        for (Map.Entry<Integer, Account> entry : accountMap.entrySet()) {
            int accountId = entry.getKey();
            Account account = entry.getValue();
            int balance = account.getBalance();

            System.out.println("Account ID: " + accountId + ", Balance: " + balance);
        }
    }


    public static void main(String[] args) {

        // Setup data structure using file input
        String filePath = "webserver/src/main/resources/details.txt";
        accountMap = readFile(filePath);

        printMap();

        WebServer server = new WebServer(10);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }





}
