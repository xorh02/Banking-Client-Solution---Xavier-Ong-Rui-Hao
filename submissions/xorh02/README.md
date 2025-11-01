# Banking Client

Modernized Java banking client for the challenge.

## Features Implemented

- Core transfer functionality with /transfer endpoint
- Upgraded from Java 6 to Java 17+ (modern HTTP Client, var keyword)
- Input validation and proper error handling
- JWT authentication support
- Professional logging with SLF4J
- Builder pattern for fluent API
- Comprehensive unit tests with JUnit 5
- Maven project structure

## Getting Started

You'll need Java 17 or newer. Make sure the banking server is running first.

Starting the server:
With Docker:
docker run -d -p 8123:8123 singhacksbjb/sidequest-server:latest

Or if you have the jar file:
java -jar core-banking-api.jar

Check if it's working:
curl http://localhost:8123/accounts/validate/ACC1000

## Running the Client

Simple version (just compile and run):
javac CleanBankingClient.java
java CleanBankingClient

If you want the full version with all features, use Maven:
mvn clean compile
mvn exec:java -Dexec.mainClass="ModernBankingClient"

## How to Use

Transfer money between accounts:
var client = new CleanBankingClient();
String result = client.transferFunds("ACC1000", "ACC1001", 100.00);

Check if an account exists:
boolean isValid = client.checkAccount("ACC1000");

## What's Included

The main files are:
- CleanBankingClient.java - simple version that works without Maven
- ModernBankingClient.java - full version with JWT auth and all the extras
- pom.xml - Maven setup if you want to use the full version
- Unit tests in src/test/

The code upgrades the old Java 6 style to modern Java 17+ with proper error handling, input validation, and the new HTTP client instead of the old HttpURLConnection mess.
