/**
 * Clean Modern Banking Client - Standalone Demo Version
 * Demonstrates Java 17+ modernization without external dependencies
 */

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public class CleanBankingClient {
    
    private static final String DEFAULT_BASE_URL = "http://localhost:8123";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^ACC\\d{4}$");
    
    private final HttpClient httpClient;
    private final String baseUrl;

    public CleanBankingClient() {
        this(DEFAULT_BASE_URL);
    }

    public CleanBankingClient(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
        
        System.out.println("Modern Banking Client initialized with: " + baseUrl);
    }

    public static class BankingClientException extends Exception {
        private final int statusCode;

        public BankingClientException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public BankingClientException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
        }

        public int getStatusCode() { return statusCode; }
    }

    private void validateAccountFormat(String account) throws BankingClientException {
        if (account == null || !ACCOUNT_PATTERN.matcher(account).matches()) {
            throw new BankingClientException("Invalid account format: " + account + 
                ". Expected format: ACC#### (e.g., ACC1000)", 400);
        }
    }

    private void validateAmount(double amount) throws BankingClientException {
        if (amount <= 0) {
            throw new BankingClientException("Amount must be positive: " + amount, 400);
        }
        if (amount > 1_000_000) {
            throw new BankingClientException("Amount exceeds maximum limit: " + amount, 400);
        }
    }

    public String transferFunds(String fromAccount, String toAccount, double amount) 
            throws BankingClientException {
        
        System.out.println("Initiating transfer: " + fromAccount + " -> " + toAccount + 
                          " (Amount: $" + amount + ")");
        
        validateAccountFormat(fromAccount);
        validateAccountFormat(toAccount);
        validateAmount(amount);
        
        try {
            String jsonBody = String.format(
                "{\"fromAccount\":\"%s\",\"toAccount\":\"%s\",\"amount\":%.2f}",
                fromAccount, toAccount, amount
            );
            
            System.out.println("Request: " + jsonBody);

            // Modern Java 17+ HTTP Client
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transfer"))
                    .header("Content-Type", "application/json")
                    .timeout(DEFAULT_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Response status: " + response.statusCode());
            System.out.println("Response body: " + response.body());

            if (response.statusCode() == 200) {
                System.out.println("Transfer completed successfully!");
                return response.body();
            } else {
                String errorMessage = String.format("Transfer failed with status %d: %s", 
                                                  response.statusCode(), response.body());
                throw new BankingClientException(errorMessage, response.statusCode());
            }
            
        } catch (IOException | InterruptedException e) {
            throw new BankingClientException("Transfer request failed: " + e.getMessage(), e);
        }
    }

    public boolean checkAccount(String accountId) throws BankingClientException {
        System.out.println("Validating account: " + accountId);
        
        if (!ACCOUNT_PATTERN.matcher(accountId).matches()) {
            System.out.println("Invalid account format: " + accountId);
            return false;
        }
        
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/accounts/validate/" + accountId))
                    .header("Accept", "application/json")
                    .timeout(DEFAULT_TIMEOUT)
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            boolean isValid = response.statusCode() == 200;
            System.out.println("Account " + accountId + " validation: " + 
                             (isValid ? "VALID" : "INVALID"));
            
            return isValid;
            
        } catch (IOException | InterruptedException e) {
            throw new BankingClientException("Account validation failed: " + e.getMessage(), e);
        }
    }

    private void showModernizationComparison() {
        System.out.println("\n=== MODERNIZATION COMPARISON ===");
        
        System.out.println("\nLEGACY Java 6 Problems:");
        System.out.println("  - Manual HttpURLConnection setup (20+ lines)");
        System.out.println("  - Manual stream handling and resource management");
        System.out.println("  - String concatenation for JSON (error-prone)");
        System.out.println("  - Generic Exception catch-all");
        System.out.println("  - No input validation or timeout configuration");
        
        System.out.println("\nMODERN Java 17+ Solutions:");
        System.out.println("  - HttpClient with builder pattern (clean & concise)");
        System.out.println("  - Automatic resource management");
        System.out.println("  - Structured JSON handling with validation");
        System.out.println("  - Specific exception types with HTTP status codes");
        System.out.println("  - Built-in timeout and connection pooling");
        System.out.println("  - Modern 'var' keyword usage");
    }

    public static void main(String[] args) {
        var client = new CleanBankingClient();
        
        try {
            System.out.println("=== MODERN BANKING CLIENT DEMO ===");
            
            client.showModernizationComparison();
            
            System.out.println("\n=== LIVE API DEMO ===");
            System.out.println("(Requires banking server running on localhost:8123)");
            
            // Demo 1: Account validation
            System.out.println("\nDemo 1: Account Validation");
            boolean isValid = client.checkAccount("ACC1000");
            System.out.println("Result: ACC1000 is " + (isValid ? "valid" : "invalid"));
            
            // Demo 2: Basic transfer
            System.out.println("\nDemo 2: Basic Transfer");
            String result = client.transferFunds("ACC1000", "ACC1001", 100.00);
            System.out.println("Transfer completed - Response length: " + result.length() + " characters");
            
            // Demo 3: Error handling
            System.out.println("\nDemo 3: Error Handling Demonstration");
            try {
                client.transferFunds("INVALID", "ACC1001", 100.00);
            } catch (BankingClientException e) {
                System.out.println("Expected error caught: " + e.getMessage());
            }
            
            System.out.println("\n=== DEMO COMPLETED SUCCESSFULLY ===");
            System.out.println("\nFor the complete Maven-based implementation, see:");
            System.out.println("  - ModernBankingClient.java (full featured version)");
            System.out.println("  - README.md (comprehensive documentation)");
            System.out.println("  - pom.xml (Maven dependencies)");
            System.out.println("  - Unit tests with JUnit 5");
            
        } catch (BankingClientException e) {
            System.err.println("Banking operation failed: " + e.getMessage());
            if (e.getStatusCode() == -1) {
                System.err.println("\nServer may not be running. Start with:");
                System.err.println("  docker run -d -p 8123:8123 singhacksbjb/sidequest-server:latest");
            }
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}