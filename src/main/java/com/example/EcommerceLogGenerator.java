package com.example;

import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EcommerceLogGenerator {
    private static final Logger logger = LogManager.getLogger(EcommerceLogGenerator.class);
    private static final Random random = new Random();

    private static final String[] REQUESTS = {"GET", "POST", "PUT", "DELETE"};
    private static final String[] ENDPOINTS = {"/usr", "/usr/admin", "/usr/admin/developer", "/usr/login", "/usr/register", "/cart", "/checkout"};
    private static final String[] STATUS_CODES = {"303", "404", "500", "403", "502", "304", "200"};
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0",
        "Mozilla/5.0 (Android 10; Mobile; rv:84.0) Gecko/84.0 Firefox/84.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 10; ONEPLUS A6000) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4380.0 Safari/537.36 Edg/89.0.759.0"
    };
    private static final String[] ACTIONS = {
        "User login", "Product search", "Add to cart", "Checkout", 
        "Payment processing", "Payment failed", "Order placed", 
        "Product viewed", "User logout", "Refund initiated", "Delivery confirmed", 
        "Database connection error", "Session expired", "Server timeout", "Item out of stock"
    };

    public static void main(String[] args) {
        try (FileWriter fileWriter = new FileWriter("logfiles.log", true)) { 
            while (true) {
                try {
                    String logEntry = EcommerceLogGenerator.generateLogEntry();
                    fileWriter.write(logEntry);
                    fileWriter.flush(); 
                    Thread.sleep(2000);
                } catch (IOException e) {
                    logger.error("Error writing log entry", e);
                } catch (InterruptedException e) {
                    logger.error("Thread sleep interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("Unexpected error", e);
                }
            }
        } catch (IOException e) {
            logger.error("Error in logging process", e);
        }
    }

    public static String generateLogEntry() {
        String ipAddress = generateRandomIp();
        String timestamp = generateRandomHttpDate("01/Jan/2023:12:00:00", "01/Jan/2025:12:00:00");
        String requestType = REQUESTS[random.nextInt(REQUESTS.length)];
        String endpoint = ENDPOINTS[random.nextInt(ENDPOINTS.length)];
        String statusCode = STATUS_CODES[random.nextInt(STATUS_CODES.length)];
        String responseTime = String.valueOf((int) (random.nextGaussian() * 50 + 5000));
        String referrer = "-";
        String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
        String action = ACTIONS[random.nextInt(ACTIONS.length)];  
        String userId = String.valueOf(random.nextInt(5000) + 1);
        String logType = getLogType(action);

        return String.format("%s - - [%s] \"%s %s HTTP/1.0\" %s %s \"%s\" \"%s\" %s \"%s\" [%s]%n",
                ipAddress, timestamp, requestType, endpoint, statusCode, responseTime, referrer, userAgent, userId, action, logType);
    }

    public static void logWithLevel(String logEntry) {
        String[] parts = logEntry.split("\"");
        if (parts.length > 9) {
            String action = parts[9];
            if (action.contains("login") || action.contains("search") || action.contains("viewed") || action.contains("placed")) {
                logger.info(logEntry);
            } else if (action.contains("failed") || action.contains("timeout") || action.contains("expired") || action.contains("out of stock")) {
                logger.error(logEntry);
            } else if (action.contains("Refund") || action.contains("Delivery confirmed") || action.contains("Database connection error")) {
                logger.warn(logEntry); 
            }
        } else {
            logger.error("Log entry does not contain the expected number of parts: " + logEntry);
        }
    }

    public static String generateRandomIp() {
        return random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256);
    }

    public static String generateRandomHttpDate(String startDateStr, String endDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
    
        try {
            ZonedDateTime startDate = ZonedDateTime.parse(startDateStr + " +0000", DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH));
            ZonedDateTime endDate = ZonedDateTime.parse(endDateStr + " +0000", DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH));
            long startMillis = startDate.toInstant().toEpochMilli();
            long endMillis = endDate.toInstant().toEpochMilli();
            long randomMillis = startMillis + (long) (random.nextDouble() * (endMillis - startMillis));
            ZonedDateTime randomDate = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(randomMillis), ZoneOffset.UTC);
            return randomDate.format(formatter);
    
        } catch (DateTimeParseException e) {
            logger.error("Error parsing date", e);
            return ZonedDateTime.now(ZoneOffset.UTC).format(formatter);
        }
    }

    public static String getLogType(String action) {
        if (action.contains("login") || action.contains("search") || action.contains("viewed") || action.contains("placed")) {
            return "INFO";
        } else if (action.contains("failed") || action.contains("timeout") || action.contains("expired") || action.contains("out of stock")) {
            return "ERROR";
        } else if (action.contains("Refund") || action.contains("Delivery confirmed") || action.contains("Database connection error")) {
            return "WARN";
        }
        return "INFO";
    }
}