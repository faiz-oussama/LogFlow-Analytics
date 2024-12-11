package com.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.shape.Circle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URL;
import java.util.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import javafx.application.Platform;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.animation.FadeTransition;

public class ChatbotController implements Initializable {
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatHistory;
    @FXML private TextField userInput;
    @FXML private Button sendButton;
    @FXML private FlowPane suggestedQuestionsPane;
    @FXML private HBox typingIndicator;
    @FXML private Label typingLabel;
    @FXML private Circle dot1;
    @FXML private Circle dot2;
    @FXML private Circle dot3;
    @FXML private Label dashboardLabel;
    @FXML private Label logsLabel;
    @FXML private Label filterLabel;
    @FXML private Label chatbotLabel;

    private ObjectMapper objectMapper;
    private List<Map<String, String>> logEntries;
    private static final String[] SUGGESTED_QUESTIONS = {
        "What are the most common errors?",
        "Show me errors from the last hour",
        "What's the average response size?",
        "Which endpoints have the most errors?",
        "Show me failed requests",
        "What are the most common actions?",
        "Show me requests by HTTP method",
        "Show request rate analysis",
        "Show performance trends",
        "Analyze error patterns"
    };

    private Timeline typingAnimation;
    private VBox welcomeContainer;
    private WebScrapingService webScrapingService;
    private boolean waitingForWebSearchConfirmation = false;
    private String pendingSearchQuery = null;

    private static final Set<String> TECHNICAL_KEYWORDS = new HashSet<>(Arrays.asList(
        "error", "problem", "exception", "fail", "bug", "issue", "crash", "stuck",
        "not working", "help", "how to", "how do i", "can't", "cannot", "doesn't work",
        "broken", "debug", "fix", "solve", "solution", 
        // Adding more technical keywords
        "implement", "configure", "setup", "install", "deploy", "integrate",
        "optimize", "performance", "security", "best practice", "example",
        "tutorial", "guide", "documentation", "reference", "syntax"
    ));

    private static final Set<String> DIRECT_SEARCH_KEYWORDS = new HashSet<>(Arrays.asList(
        "search", "find", "lookup", "google", "stackoverflow", "stack overflow",
        "docs", "documentation", "example", "tutorial", "guide", "reference"
    ));

    private static final String BASE_STYLES = """
            body {
                font-family: 'Segoe UI', system-ui, -apple-system;
                margin: 12px 16px;
                color: #2d3436;
                line-height: 1.5;
                font-size: 14px;
            }
            pre {
                background-color: rgba(0, 0, 0, 0.04);
                padding: 12px;
                border-radius: 6px;
                font-family: 'Consolas', 'Monaco', monospace;
                font-size: 13px;
                overflow-x: auto;
                white-space: pre-wrap;
            }
            code {
                font-family: 'Consolas', 'Monaco', monospace;
                background-color: rgba(0, 0, 0, 0.04);
                padding: 2px 4px;
                border-radius: 4px;
            }
            a {
                color: #0366d6;
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }
            ul, ol {
                margin: 8px 0;
                padding-left: 24px;
            }
            li {
                margin: 4px 0;
            }
            blockquote {
                margin: 8px 0;
                padding-left: 12px;
                border-left: 4px solid #dfe2e5;
                color: #6a737d;
            }
            .timestamp {
                font-size: 11px;
                color: #636e72;
                margin-top: 4px;
            }
            """;

    private static final String USER_STYLES = BASE_STYLES + """
            body {
                color: white;
            }
            a {
                color: #9fccff;
            }
            """;

    public ChatbotController() {
        this.webScrapingService = new WebScrapingService();
    }

    private boolean shouldOfferWebSearch(String message) {
        message = message.toLowerCase();
        
        // Check for technical keywords
        for (String keyword : TECHNICAL_KEYWORDS) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        
        // Check if it's a question
        if (message.contains("?") || message.startsWith("what") || message.startsWith("how") 
            || message.startsWith("why") || message.startsWith("can") || message.startsWith("could")) {
            return true;
        }
        
        return false;
    }

    private String formatSearchQuery(String message) {
        // Remove common words that don't help with search
        String[] wordsToRemove = {"a", "an", "the", "in", "on", "at", "to", "for", "of", "with", "by"};
        String query = message.toLowerCase();
        for (String word : wordsToRemove) {
            query = query.replaceAll("\\b" + word + "\\b", "");
        }
        
        // Add technical context if not present
        if (!query.contains("java") && !query.contains("error")) {
            query += " java error solution";
        }
        
        return query.trim().replaceAll("\\s+", " ");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        objectMapper = new ObjectMapper();
        logEntries = new ArrayList<>();
        loadLogs();
        setupSuggestedQuestions();
        setupNavigation();
        chatHistory.heightProperty().addListener((obs, old, val) -> 
            chatScrollPane.setVvalue(1.0));
            
        userInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleSendMessage();
            }
        });
        
        typingIndicator.setVisible(false);
        typingLabel.setText("...");
        initializeTypingAnimation();
        showWelcomeMessage();
    }
    private void setupNavigation() {
        dashboardLabel.setOnMouseClicked(event -> {
            try {  
                MainApplication.setRoot("logAnalytics");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        logsLabel.setOnMouseClicked(event -> {
            try {
                MainApplication.setRoot("logVis");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        filterLabel.setOnMouseClicked(event -> {
            try {
                MainApplication.setRoot("filter");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        chatbotLabel.setOnMouseClicked(event -> {
            try {
                MainApplication.setRoot("chatbot");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadLogs() {
        try {
            File logsFile = new File("src/main/resources/logs.json");
            JsonNode rootNode = objectMapper.readTree(logsFile);
            logEntries = new ArrayList<>();
            
            if (rootNode.isArray()) {
                for (JsonNode node : rootNode) {
                    Map<String, String> logEntry = new HashMap<>();
                    logEntry.put("client_ip", node.path("client_ip").asText());
                    logEntry.put("timestamp", node.path("timestamp").asText());
                    logEntry.put("http_method", node.path("http_method").asText());
                    logEntry.put("request", node.path("request").asText());
                    logEntry.put("status_code", node.path("status_code").asText());
                    logEntry.put("response_size", node.path("response_size").asText());
                    logEntry.put("user_id", node.path("user_id").asText());
                    logEntry.put("action", node.path("action").asText());
                    logEntry.put("log_level", node.path("log_level").asText());
                    logEntry.put("message", node.path("message").asText());
                    logEntries.add(logEntry);
                }
            } else {
                Map<String, String> logEntry = new HashMap<>();
                logEntry.put("client_ip", rootNode.path("client_ip").asText());
                logEntry.put("timestamp", rootNode.path("timestamp").asText());
                logEntry.put("http_method", rootNode.path("http_method").asText());
                logEntry.put("request", rootNode.path("request").asText());
                logEntry.put("status_code", rootNode.path("status_code").asText());
                logEntry.put("response_size", rootNode.path("response_size").asText());
                logEntry.put("user_id", rootNode.path("user_id").asText());
                logEntry.put("action", rootNode.path("action").asText());
                logEntry.put("log_level", rootNode.path("log_level").asText());
                logEntry.put("message", rootNode.path("message").asText());
                logEntries.add(logEntry);
            }            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Failed to load logs: " + e.getMessage());
        }
    }

    private String processUserQuery(String query) {
        query = query.toLowerCase();
        
        // First check if it's a log analysis query
        if (query.contains("common errors") || query.contains("frequent errors")) {
            return analyzeCommonErrors();
        } else if (query.contains("response size") || query.contains("average size")) {
            return analyzeResponseSizes();
        } else if (query.contains("failed requests") || query.contains("failures")) {
            return analyzeFailedRequests();
        } else if (query.contains("endpoints")) {
            return analyzeEndpoints();
        } else if (query.contains("http method") || query.contains("request method")) {
            return analyzeHttpMethods();
        } else if (query.contains("actions") || query.contains("common actions")) {
            return analyzeCommonActions();
        } else if (query.contains("request rate") || query.contains("traffic")) {
            return analyzeRequestRate();
        } else if (query.contains("performance") || query.contains("trends")) {
            return analyzePerformanceTrends();
        } else if (query.contains("error pattern")) {
            return analyzeErrorPatterns();
        } else if (shouldOfferWebSearch(query)) {
            // Perform web search
            return handleUserQuery(query);
        } else {
            return "I'm not sure how to help with that query. Try asking about:\n" +
                   "- Common errors\n" +
                   "- Response sizes\n" +
                   "- Failed requests\n" +
                   "- Endpoints analysis\n" +
                   "- HTTP methods\n" +
                   "- Common actions\n" +
                   "- Request rate analysis\n" +
                   "- Performance trends\n" +
                   "- Error patterns\n" +
                   "You can also ask me any technical questions, and I'll search for solutions!";
        }
    }

    private String summarizeQuery(String query) {
        if (query.contains("how")) {
            return "how to " + query.substring(query.indexOf("how") + 3).trim();
        } else if (query.contains("what")) {
            return "what " + query.substring(query.indexOf("what") + 4).trim();
        } else if (query.contains("why")) {
            return "why " + query.substring(query.indexOf("why") + 3).trim();
        } else if (query.contains("error") || query.contains("problem") || query.contains("issue")) {
            return "the " + (query.contains("error") ? "error" : query.contains("problem") ? "problem" : "issue") + " you're experiencing";
        } else {
            return "your question";
        }
    }

    private String analyzeCommonErrors() {
        Map<String, Integer> errorCounts = new HashMap<>();
        for (Map<String, String> log : logEntries) {
            if ("ERROR".equals(log.get("log_level"))) {
                String action = log.get("action");
                errorCounts.merge(action, 1, Integer::sum);
            }
        }
        
        if (errorCounts.isEmpty()) {
            return "No errors found in the logs.";
        }

        StringBuilder response = new StringBuilder("Common errors in the logs:\n\n");
        errorCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .forEach(entry -> response.append(String.format("- %s: %d occurrences\n", 
                                            entry.getKey(), entry.getValue())));
        
        return response.toString();
    }

    private String analyzeResponseSizes() {
        List<Long> sizes = logEntries.stream()
            .map(log -> Long.parseLong(log.get("response_size")))
            .collect(Collectors.toList());
        
        double average = sizes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long max = sizes.stream().mapToLong(Long::longValue).max().orElse(0);
        long min = sizes.stream().mapToLong(Long::longValue).min().orElse(0);
        
        return String.format("Response Size Analysis:\n\n" +
                           "Average size: %.2f bytes\n" +
                           "Largest response: %d bytes\n" +
                           "Smallest response: %d bytes\n",
                           average, max, min);
    }

    private String analyzeFailedRequests() {
        List<Map<String, String>> failedRequests = logEntries.stream()
            .filter(log -> {
                String statusCode = log.get("status_code");
                return statusCode.startsWith("4") || statusCode.startsWith("5");
            })
            .collect(Collectors.toList());
        
        if (failedRequests.isEmpty()) {
            return "No failed requests found in the logs.";
        }

        Map<String, Integer> statusCodeCount = new HashMap<>();
        for (Map<String, String> request : failedRequests) {
            String statusCode = request.get("status_code");
            statusCodeCount.merge(statusCode, 1, Integer::sum);
        }

        StringBuilder response = new StringBuilder("Failed Requests Analysis:\n\n");
        response.append(String.format("Total failed requests: %d\n\n", failedRequests.size()));
        response.append("Breakdown by status code:\n");
        statusCodeCount.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> response.append(String.format("- %s: %d requests\n", 
                                            entry.getKey(), entry.getValue())));
        
        return response.toString();
    }

    private String analyzeEndpoints() {
        Map<String, Integer> endpointCounts = new HashMap<>();
        Map<String, Integer> endpointErrors = new HashMap<>();
        
        for (Map<String, String> log : logEntries) {
            String endpoint = log.get("request");
            endpointCounts.merge(endpoint, 1, Integer::sum);
            
            if ("ERROR".equals(log.get("log_level"))) {
                endpointErrors.merge(endpoint, 1, Integer::sum);
            }
        }

        StringBuilder response = new StringBuilder("Endpoint Analysis:\n\n");
        response.append("Most accessed endpoints:\n");
        endpointCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .forEach(entry -> response.append(String.format("- %s: %d requests\n", 
                                            entry.getKey(), entry.getValue())));
        
        response.append("\nEndpoints with most errors:\n");
        endpointErrors.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(5)
            .forEach(entry -> response.append(String.format("- %s: %d errors\n", 
                                            entry.getKey(), entry.getValue())));
        
        return response.toString();
    }

    private String analyzeHttpMethods() {
        Map<String, Integer> methodCounts = new HashMap<>();
        for (Map<String, String> log : logEntries) {
            String method = log.get("http_method");
            methodCounts.merge(method, 1, Integer::sum);
        }

        StringBuilder response = new StringBuilder("HTTP Methods Analysis:\n\n");
        methodCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> response.append(String.format("- %s: %d requests\n", 
                                            entry.getKey(), entry.getValue())));
        
        return response.toString();
    }

    private String analyzeCommonActions() {
        Map<String, Integer> actionCounts = new HashMap<>();
        for (Map<String, String> log : logEntries) {
            String action = log.get("action");
            if (action != null && !action.isEmpty()) {
                actionCounts.merge(action, 1, Integer::sum);
            }
        }

        if (actionCounts.isEmpty()) {
            return "No actions found in the logs.";
        }

        StringBuilder response = new StringBuilder("Common Actions Analysis:\n\n");
        actionCounts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(10)
            .forEach(entry -> response.append(String.format("- %s: %d occurrences\n", 
                                            entry.getKey(), entry.getValue())));
        
        return response.toString();
    }

    @FXML
    private void handleSendMessage() {
        if (welcomeContainer != null) {
            chatHistory.getChildren().remove(welcomeContainer);
            welcomeContainer = null;
        }
        String message = userInput.getText().trim();
        if (message.isEmpty()) return;
        
        addUserMessage(message);
        userInput.clear();
        showTypingIndicator(true);
        
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                // Initial dot
                typingLabel.setText(".");
            }),
            new KeyFrame(Duration.seconds(0.5), e -> {
                // Two dots
                typingLabel.setText("..");
            }),
            new KeyFrame(Duration.seconds(1.0), e -> {
                // Three dots
                typingLabel.setText("...");
            }),
            new KeyFrame(Duration.seconds(2.0), e -> {
                // Process and show response
                String response = processUserQuery(message);
                showTypingIndicator(false);
                addBotMessage(response);
            })
        );
        timeline.play();
    }
    
    private void showTypingIndicator(boolean show) {
        typingIndicator.setVisible(show);
        if (show) {
            typingIndicator.setManaged(true);
            chatScrollPane.setVvalue(1.0);
            typingAnimation.play();
        } else {
            typingIndicator.setManaged(false);
            typingAnimation.stop();
        }
    }

    private void addUserMessage(String message) {
        HBox messageContainer = new HBox();
        messageContainer.getStyleClass().addAll("user-message-container");
        messageContainer.setAlignment(Pos.CENTER_RIGHT);  

        VBox messageBox = new VBox(5);
        messageBox.getStyleClass().add("message-bubble");
        messageBox.setMaxWidth(500);  

        Text messageText = new Text(message);
        messageText.getStyleClass().add("message-text");
        messageText.setWrappingWidth(480);  

        Text timeText = new Text(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeText.getStyleClass().add("timestamp");

        messageBox.getChildren().addAll(messageText, timeText);
        messageContainer.getChildren().add(messageBox);
        chatHistory.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        HBox messageContainer = new HBox();
        messageContainer.getStyleClass().addAll("bot-message-container");
        messageContainer.setAlignment(Pos.CENTER_LEFT);

        VBox messageBox = new VBox(5);
        messageBox.getStyleClass().add("message-bubble");

        Text messageText = new Text(message);
        messageText.getStyleClass().add("message-text");
        messageText.setWrappingWidth(500);  

        Text timeText = new Text(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeText.getStyleClass().add("timestamp");

        messageBox.getChildren().addAll(messageText, timeText);
        messageContainer.getChildren().add(messageBox);
        chatHistory.getChildren().add(messageContainer);
        scrollToBottom();
    }

    private void setupSuggestedQuestions() {
        for (String question : SUGGESTED_QUESTIONS) {
            Button suggestionBtn = new Button(question);
            suggestionBtn.getStyleClass().add("suggestion-button");
            suggestionBtn.setOnAction(e -> {
                userInput.setText(question);
                handleSendMessage();
            });
            suggestedQuestionsPane.getChildren().add(suggestionBtn);
        }
    }

    private void showErrorMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String analyzeRequestRate() {
        Map<String, Integer> hourlyRequests = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
        
        for (Map<String, String> log : logEntries) {
            String timestamp = log.get("timestamp");
            try {
                LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
                String hour = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"));
                hourlyRequests.merge(hour, 1, Integer::sum);
            } catch (DateTimeParseException e) {
                continue;
            }
        }

        StringBuilder response = new StringBuilder("Request Rate Analysis:\n\n");
        
        if (hourlyRequests.isEmpty()) {
            return "No request rate data found in the logs.";
        }

        response.append("Hourly request distribution:\n");
        hourlyRequests.forEach((hour, count) -> 
            response.append(String.format("- %s: %d requests\n", hour, count)));

        double avgRequestsPerHour = hourlyRequests.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);

        response.append(String.format("\nAverage requests per hour: %.2f", avgRequestsPerHour));
        return response.toString();
    }

    private String analyzePerformanceTrends() {
        Map<String, List<Integer>> endpointResponseTimes = new HashMap<>();
        
        for (Map<String, String> log : logEntries) {
            String endpoint = log.get("request");
            String responseTimeStr = log.get("response_time");
            
            if (endpoint != null && responseTimeStr != null) {
                try {
                    int responseTime = Integer.parseInt(responseTimeStr);
                    endpointResponseTimes
                        .computeIfAbsent(endpoint, k -> new ArrayList<>())
                        .add(responseTime);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }

        StringBuilder response = new StringBuilder("Performance Analysis:\n\n");
        response.append("Average response times by endpoint:\n");
        
        endpointResponseTimes.entrySet().stream()
            .sorted((e1, e2) -> {
                double avg1 = e1.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                double avg2 = e2.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
                return Double.compare(avg2, avg1);
            })
            .limit(10)
            .forEach(entry -> {
                double avgTime = entry.getValue().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);
                response.append(String.format("- %s: %.2fms\n", entry.getKey(), avgTime));
            });

        return response.toString();
    }

    private String analyzeErrorPatterns() {
        List<ErrorSequence> errorSequences = new ArrayList<>();
        ErrorSequence currentSequence = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");

        for (Map<String, String> log : logEntries) {
            if ("ERROR".equals(log.get("log_level"))) {
                LocalDateTime timestamp = LocalDateTime.parse(log.get("timestamp"), formatter);
                String endpoint = log.get("request");
                String errorType = log.get("action");

                if (currentSequence == null) {
                    currentSequence = new ErrorSequence(timestamp);
                }
                
                currentSequence.addError(errorType, endpoint);
            } else if (currentSequence != null) {
                errorSequences.add(currentSequence);
                currentSequence = null;
            }
        }

        if (currentSequence != null) {
            errorSequences.add(currentSequence);
        }

        StringBuilder response = new StringBuilder("Error Pattern Analysis:\n\n");
        
        if (errorSequences.isEmpty()) {
            return "No error patterns detected in the logs.";
        }

        ErrorSequence longestSequence = errorSequences.stream()
            .max(Comparator.comparingInt(ErrorSequence::getSize))
            .orElse(null);

        if (longestSequence != null) {
            response.append(String.format("Longest error sequence: %d consecutive errors\n", 
                longestSequence.getSize()));
            response.append("Most common errors in sequence:\n");
            longestSequence.getMostCommonErrors().forEach((error, count) -> 
                response.append(String.format("- %s: %d times\n", error, count)));
        }

        double avgSequenceLength = errorSequences.stream()
            .mapToInt(ErrorSequence::getSize)
            .average()
            .orElse(0.0);

        response.append(String.format("\nAverage error sequence length: %.2f errors", 
            avgSequenceLength));

        return response.toString();
    }

    private void initializeTypingAnimation() {
        typingAnimation = new Timeline(
            new KeyFrame(Duration.seconds(0.0), e -> {
                dot1.setOpacity(1.0);
                dot2.setOpacity(0.3);
                dot3.setOpacity(0.3);
            }),
            new KeyFrame(Duration.seconds(0.3), e -> {
                dot1.setOpacity(0.3);
                dot2.setOpacity(1.0);
                dot3.setOpacity(0.3);
            }),
            new KeyFrame(Duration.seconds(0.6), e -> {
                dot1.setOpacity(0.3);
                dot2.setOpacity(0.3);
                dot3.setOpacity(1.0);
            })
        );
        typingAnimation.setCycleCount(Timeline.INDEFINITE);
    }

    private void showWelcomeMessage() {
        Label welcomeLabel = new Label("Welcome to the Chatbot!");
        welcomeLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: #333; -fx-font-weight: bold;");

        Label promptLabel = new Label("Please type or ask something to get started.");
        promptLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555;");

        welcomeContainer = new VBox(welcomeLabel, promptLabel);
        welcomeContainer.setAlignment(Pos.CENTER);
        welcomeContainer.setSpacing(10);
        welcomeContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-padding: 20; -fx-border-radius: 10; -fx-background-radius: 10;");
        
        chatHistory.getChildren().add(welcomeContainer);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2), welcomeContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private void handleUserMessage(String message) {
        if (waitingForWebSearchConfirmation) {
            handleWebSearchConfirmation(message.toLowerCase());
            return;
        }

        // Add user message to chat
        addUserMessage(message);

        // Show typing indicator
        showTypingIndicator(true);

        // Check for direct search keywords first
        boolean isDirectSearch = false;
        String lowercaseMessage = message.toLowerCase();
        for (String keyword : DIRECT_SEARCH_KEYWORDS) {
            if (lowercaseMessage.contains(keyword)) {
                isDirectSearch = true;
                break;
            }
        }

        // Process the message and get response
        String response = processUserQuery(message);
        
        // If it's a direct search or technical query, perform search immediately
        if (isDirectSearch || (shouldOfferWebSearch(message) && lowercaseMessage.contains("?"))) {
            showTypingIndicator(true);
            Thread searchThread = new Thread(() -> {
                try {
                    List<String> solutions = webScrapingService.searchSolutions(formatSearchQuery(message));
                    Platform.runLater(() -> {
                        hideTypingIndicator();
                        if (!solutions.isEmpty()) {
                            StringBuilder formattedResponse = new StringBuilder("Here are some relevant solutions I found:\n\n");
                            for (int i = 0; i < solutions.size(); i++) {
                                formattedResponse.append(String.format("%d. %s\n", i + 1, solutions.get(i)));
                            }
                            addBotMessage(formattedResponse.toString());
                        } else {
                            addBotMessage("I couldn't find any relevant solutions online. Please try rephrasing your question.");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        hideTypingIndicator();
                        addBotMessage("Sorry, I encountered an error while searching for solutions: " + e.getMessage());
                    });
                }
            });
            searchThread.setDaemon(true);
            searchThread.start();
        }
        // For other technical queries, ask for confirmation
        else if (shouldOfferWebSearch(message)) {
            pendingSearchQuery = formatSearchQuery(message);
            waitingForWebSearchConfirmation = true;
            
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                hideTypingIndicator();
                addBotMessage(response + "\n\nWould you like me to search for solutions online? (Yes/No)");
            }));
            timeline.play();
        } else {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                hideTypingIndicator();
                addBotMessage(response);
            }));
            timeline.play();
        }
    }

    private void handleWebSearchConfirmation(String response) {
        waitingForWebSearchConfirmation = false;
        if (response.startsWith("y")) {
            showTypingIndicator(true);
            Thread searchThread = new Thread(() -> {
                try {
                    List<String> solutions = webScrapingService.searchSolutions(pendingSearchQuery);
                    Platform.runLater(() -> {
                        hideTypingIndicator();
                        if (!solutions.isEmpty()) {
                            StringBuilder formattedResponse = new StringBuilder("Here are some relevant solutions I found:\n\n");
                            for (int i = 0; i < solutions.size(); i++) {
                                formattedResponse.append(String.format("%d. %s\n", i + 1, solutions.get(i)));
                            }
                            addBotMessage(formattedResponse.toString());
                        } else {
                            addBotMessage("I couldn't find any relevant solutions online. Please try rephrasing your question.");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        hideTypingIndicator();
                        addBotMessage("Sorry, I encountered an error while searching for solutions: " + e.getMessage());
                    });
                }
            });
            searchThread.setDaemon(true);
            searchThread.start();
        } else {
            addBotMessage("Okay, let me know if you'd like to search the web later.");
        }
        pendingSearchQuery = null;
    }

    private String handleUserQuery(String userQuery) {
        // Fetch search results from WebScrapingService
        List<String> searchResults = webScrapingService.searchSolutions(userQuery);

        // Format the results into a user-friendly message
        StringBuilder response = new StringBuilder("Here are some solutions I found for your query:\n\n");
        for (String result : searchResults) {
            response.append(result).append("\n\n");
        }

        // If no results were found, add a fallback message
        if (searchResults.isEmpty()) {
            response.append("Sorry, I couldn't find any solutions for your query.");
        }

        // Return the formatted response
        return response.toString();
    }

    private void hideTypingIndicator() {
        Platform.runLater(() -> {
            typingIndicator.setVisible(false);
            if (typingAnimation != null) {
                typingAnimation.stop();
            }
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    private static class ErrorSequence {
        private final LocalDateTime startTime;
        private final Map<String, Integer> errorCounts = new HashMap<>();
        private final Map<String, Integer> endpointCounts = new HashMap<>();
        private int size = 0;

        public ErrorSequence(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public void addError(String errorType, String endpoint) {
            errorCounts.merge(errorType, 1, Integer::sum);
            endpointCounts.merge(endpoint, 1, Integer::sum);
            size++;
        }

        public int getSize() {
            return size;
        }

        public Map<String, Integer> getMostCommonErrors() {
            return errorCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
    }
}
