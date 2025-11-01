/**
 * Modern Banking Client - Java 17+ Implementation
 * 
 * This is a modernized version of the legacy Java 6 banking client that demonstrates:
 * - Java 17+ HTTP Client API (replacing HttpURLConnection)
 * - Proper JSON handling with Jackson
 * - Professional logging with SLF4J
 * - Modern exception handling and error recovery
 * - Configuration management
 * - Builder pattern implementation
 * - Input validation and security best practices
 * 
 * @author Banking Client Modernization Challenge
 * @version 2.0
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class ModernBankingClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ModernBankingClient.class);
    
    // Configuration
    private static final String DEFAULT_BASE_URL = "http://localhost:8123";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^ACC\\d{4}$");
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private String jwtToken;

    /**
     * Constructor with default configuration
     */
    public ModernBankingClient() {
        this(DEFAULT_BASE_URL);
    }

    /**
     * Constructor with custom base URL
     * @param baseUrl The base URL of the banking API
     */
    public ModernBankingClient(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "Base URL cannot be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        
        logger.info("Modern Banking Client initialized with base URL: {}", baseUrl);
    }

    /**
     * Transfer Request DTO - Immutable data transfer object
     */
    public static class TransferRequest {
        @JsonProperty("fromAccount")
        private final String fromAccount;
        
        @JsonProperty("toAccount") 
        private final String toAccount;
        
        @JsonProperty("amount")
        private final double amount;

        public TransferRequest(String fromAccount, String toAccount, double amount) {
            this.fromAccount = validateAccount(fromAccount);
            this.toAccount = validateAccount(toAccount);
            this.amount = validateAmount(amount);
        }

        // Getters
        public String getFromAccount() { return fromAccount; }
        public String getToAccount() { return toAccount; }
        public double getAmount() { return amount; }

        private static String validateAccount(String account) {
            if (account == null || !ACCOUNT_PATTERN.matcher(account).matches()) {
                throw new IllegalArgumentException("Invalid account format: " + account + 
                    ". Expected format: ACC#### (e.g., ACC1000)");
            }
            return account;
        }

        private static double validateAmount(double amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be positive: " + amount);
            }
            if (amount > 1_000_000) {
                throw new IllegalArgumentException("Amount exceeds maximum limit: " + amount);
            }
            return amount;
        }
    }

    /**
     * Transfer Response DTO - Immutable response object
     */
    public static class TransferResponse {
        @JsonProperty("transactionId")
        private String transactionId;
        
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("fromAccount")
        private String fromAccount;
        
        @JsonProperty("toAccount")
        private String toAccount;
        
        @JsonProperty("amount")
        private double amount;

        // Default constructor for Jackson
        public TransferResponse() {}

        // Getters
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getFromAccount() { return fromAccount; }
        public String getToAccount() { return toAccount; }
        public double getAmount() { return amount; }

        public boolean isSuccessful() {
            return "SUCCESS".equalsIgnoreCase(status);
        }

        @Override
        public String toString() {
            return String.format("TransferResponse{transactionId='%s', status='%s', message='%s', " +
                    "fromAccount='%s', toAccount='%s', amount=%.2f}", 
                    transactionId, status, message, fromAccount, toAccount, amount);
        }
    }

    /**
     * JWT Token Response for authentication
     */
    public static class TokenResponse {
        @JsonProperty("token")
        private String token;
        
        @JsonProperty("expiresIn")
        private long expiresIn;

        // Default constructor for Jackson
        public TokenResponse() {}

        public String getToken() { return token; }
        public long getExpiresIn() { return expiresIn; }
    }

    /**
     * Custom exception for banking operations
     */
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

    /**
     * Authenticate and obtain JWT token
     * @return JWT token for authenticated requests
     * @throws BankingClientException if authentication fails
     */
    public String authenticate() throws BankingClientException {
        logger.info("Attempting to authenticate with banking API");
        
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/authToken"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                var tokenResponse = objectMapper.readValue(response.body(), TokenResponse.class);
                this.jwtToken = tokenResponse.getToken();
                logger.info("Successfully authenticated. Token expires in {} seconds", tokenResponse.getExpiresIn());
                return this.jwtToken;
            } else {
                throw new BankingClientException("Authentication failed", response.statusCode());
            }
            
        } catch (JsonProcessingException e) {
            throw new BankingClientException("Failed to parse authentication response", e);
        } catch (IOException | InterruptedException e) {
            throw new BankingClientException("Authentication request failed", e);
        }
    }

    /**
     * Transfer funds between accounts
     * @param fromAccount Source account ID
     * @param toAccount Destination account ID  
     * @param amount Amount to transfer
     * @return Transfer response with transaction details
     * @throws BankingClientException if transfer fails
     */
    public TransferResponse transferFunds(String fromAccount, String toAccount, double amount) 
            throws BankingClientException {
        
        logger.info("Initiating transfer: {} -> {} (Amount: ${})", fromAccount, toAccount, amount);
        
        var transferRequest = new TransferRequest(fromAccount, toAccount, amount);
        
        try {
            // Convert request to JSON
            String jsonBody = objectMapper.writeValueAsString(transferRequest);
            logger.debug("Transfer request JSON: {}", jsonBody);

            // Build HTTP request
            var requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transfer"))
                    .header("Content-Type", "application/json")
                    .timeout(DEFAULT_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            // Add JWT token if available
            if (jwtToken != null) {
                requestBuilder.header("Authorization", "Bearer " + jwtToken);
                logger.debug("Added JWT authentication header");
            }

            var request = requestBuilder.build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            logger.debug("Transfer response status: {}", response.statusCode());
            logger.debug("Transfer response body: {}", response.body());

            if (response.statusCode() == 200) {
                var transferResponse = objectMapper.readValue(response.body(), TransferResponse.class);
                
                if (transferResponse.isSuccessful()) {
                    logger.info("Transfer completed successfully. Transaction ID: {}", 
                              transferResponse.getTransactionId());
                } else {
                    logger.warn("Transfer failed: {}", transferResponse.getMessage());
                }
                
                return transferResponse;
                
            } else {
                String errorMessage = String.format("Transfer failed with status %d: %s", 
                                                  response.statusCode(), response.body());
                logger.error(errorMessage);
                throw new BankingClientException(errorMessage, response.statusCode());
            }
            
        } catch (JsonProcessingException e) {
            throw new BankingClientException("Failed to process JSON data", e);
        } catch (IOException | InterruptedException e) {
            throw new BankingClientException("Transfer request failed", e);
        }
    }

    /**
     * Validate account existence and status
     * @param accountId Account ID to validate
     * @return true if account is valid
     * @throws BankingClientException if validation fails
     */
    public boolean validateAccount(String accountId) throws BankingClientException {
        logger.info("Validating account: {}", accountId);
        
        if (!ACCOUNT_PATTERN.matcher(accountId).matches()) {
            logger.warn("Invalid account format: {}", accountId);
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
            logger.info("Account {} validation result: {}", accountId, isValid ? "VALID" : "INVALID");
            
            return isValid;
            
        } catch (IOException | InterruptedException e) {
            throw new BankingClientException("Account validation failed", e);
        }
    }

    /**
     * Builder pattern for creating transfer requests with validation
     */
    public static class TransferBuilder {
        private String fromAccount;
        private String toAccount; 
        private double amount;
        private ModernBankingClient client;

        public TransferBuilder(ModernBankingClient client) {
            this.client = client;
        }

        public TransferBuilder from(String fromAccount) {
            this.fromAccount = fromAccount;
            return this;
        }

        public TransferBuilder to(String toAccount) {
            this.toAccount = toAccount;
            return this;
        }

        public TransferBuilder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public TransferResponse execute() throws BankingClientException {
            return client.transferFunds(fromAccount, toAccount, amount);
        }

        public TransferResponse executeWithValidation() throws BankingClientException {
            // Validate accounts before transfer
            if (!client.validateAccount(fromAccount)) {
                throw new BankingClientException("Source account is invalid: " + fromAccount, 400);
            }
            if (!client.validateAccount(toAccount)) {
                throw new BankingClientException("Destination account is invalid: " + toAccount, 400);
            }
            
            return execute();
        }
    }

    /**
     * Create a new transfer builder for fluent API usage
     */
    public TransferBuilder transfer() {
        return new TransferBuilder(this);
    }

    /**
     * Close the HTTP client and cleanup resources
     */
    public void close() {
        // HttpClient doesn't require explicit closing in Java 11+
        logger.info("Banking client closed");
    }

    /**
     * Main method - Usage examples and demonstration
     */
    public static void main(String[] args) {
        var client = new ModernBankingClient();
        
        try {
            logger.info("=== Modern Banking Client Demo ===");
            
            // Example 1: Basic transfer (legacy compatibility)
            logger.info("--- Example 1: Basic Transfer ---");
            var response1 = client.transferFunds("ACC1000", "ACC1001", 100.00);
            System.out.println("Transfer Result: " + response1);
            
            // Example 2: Transfer with authentication
            logger.info("--- Example 2: Authenticated Transfer ---");
            client.authenticate();
            var response2 = client.transferFunds("ACC1000", "ACC1001", 250.00);
            System.out.println("Authenticated Transfer: " + response2);
            
            // Example 3: Fluent API with validation
            logger.info("--- Example 3: Fluent API with Validation ---");
            var response3 = client.transfer()
                    .from("ACC1000")
                    .to("ACC1001") 
                    .amount(50.00)
                    .executeWithValidation();
            System.out.println("Validated Transfer: " + response3);
            
            // Example 4: Account validation
            logger.info("--- Example 4: Account Validation ---");
            boolean isValid = client.validateAccount("ACC1000");
            System.out.println("Account ACC1000 is valid: " + isValid);
            
        } catch (BankingClientException e) {
            logger.error("Banking operation failed: {} (Status: {})", e.getMessage(), e.getStatusCode());
            System.err.println("Error: " + e.getMessage());
        } finally {
            client.close();
        }
    }
}