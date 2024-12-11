package com.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.stage.FileChooser;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FilterController implements Initializable {
    @FXML private ComboBox<String> timePresetCombo;
    @FXML private DatePicker startDate;
    @FXML private DatePicker endDate;
    @FXML private TextField startTime;
    @FXML private TextField endTime;
    
    @FXML private CheckBox errorCheck;
    @FXML private CheckBox warnCheck;
    @FXML private CheckBox infoCheck;
    @FXML private CheckBox debugCheck;
    @FXML private CheckBox traceCheck;
    
    @FXML private ComboBox<String> sourceCombo;
    @FXML private ComboBox<String> componentCombo;
    
    @FXML private VBox advancedFiltersContainer;
    @FXML private Button resetBtn;
    @FXML private Button applyBtn;
    @FXML private Button exportCsvBtn;
    
    @FXML private Label dashboardLabel;
    @FXML private Label logsLabel;
    @FXML private Label filterLabel;
    @FXML private Label chatbotLabel;

    @FXML private TableView<LogEntry> filteredLogsTable;
    private ObservableList<LogEntry> allLogs = FXCollections.observableArrayList();
    private ObservableList<LogEntry> filteredLogs = FXCollections.observableArrayList();
    private boolean filterActive = false;
    
    @FXML private TableColumn<LogEntry, String> columnIP;
    @FXML private TableColumn<LogEntry, String> columnTimestamp;
    @FXML private TableColumn<LogEntry, String> columnRequestType;
    @FXML private TableColumn<LogEntry, String> columnEndpoint;
    @FXML private TableColumn<LogEntry, String> columnStatusCode;
    @FXML private TableColumn<LogEntry, String> columnResponseTime;
    @FXML private TableColumn<LogEntry, String> columnLogLevel;

    private List<HBox> advancedFilterRows = new ArrayList<>();
    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private volatile boolean isRunning = true;
    private List<AdvancedFilter> advancedFilters = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTimeControls();
        setupLogLevelControls();
        setupSourceControls();
        setupAdvancedFilters();
        setupButtons();
        setupTable();
        setupNavigation();
        setupExportButton();
        
        // Start Kafka consumer immediately in the background
        setupKafkaConsumer();
    }
    
    private void setupTimeControls() {
        timePresetCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                switch (newVal) {
                    case "Last 15 minutes":
                        setTimeRange(15);
                        break;
                    case "Last hour":
                        setTimeRange(60);
                        break;
                    case "Last 24 hours":
                        setTimeRange(24 * 60);
                        break;
                    case "Last 7 days":
                        setTimeRange(7 * 24 * 60);
                        break;
                    case "Custom range":
                        enableCustomTimeRange(true);
                        break;
                }
            }
        });
        
        // Set default time format
        startTime.setPromptText("HH:mm");
        endTime.setPromptText("HH:mm");
    }
    
    private void setTimeRange(int minutes) {
        LocalDate now = LocalDate.now();
        LocalTime currentTime = LocalTime.now();
        
        endDate.setValue(now);
        endTime.setText(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        
        if (minutes < 24 * 60) {
            startDate.setValue(now);
            startTime.setText(currentTime.minusMinutes(minutes).format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            startDate.setValue(now.minusDays(minutes / (24 * 60)));
            startTime.setText(currentTime.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
    
    private void setupLogLevelControls() {
        // Set default selections
        infoCheck.setSelected(true);
        errorCheck.setSelected(true);
        warnCheck.setSelected(true);
    }
    
    private void setupSourceControls() {
        sourceCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateComponentCombo(newVal);
            }
        });
    }
    
    private void updateComponentCombo(String source) {
        ObservableList<String> components = FXCollections.observableArrayList();
        switch (source) {
            case "Application":
                components.addAll("Frontend", "Backend", "Database", "Cache");
                break;
            case "System":
                components.addAll("OS", "Hardware", "Services", "Drivers");
                break;
            case "Security":
                components.addAll("Authentication", "Authorization", "Firewall", "Encryption");
                break;
            case "Network":
                components.addAll("HTTP", "TCP/IP", "DNS", "Load Balancer");
                break;
        }
        componentCombo.setItems(components);
    }
    
    private void setupAdvancedFilters() {
        // Add initial filter row
        addFilterRow();
    }
    
    private void addFilterRow() {
        HBox filterRow = (HBox) advancedFiltersContainer.getChildren().get(0);
        Button addButton = (Button) filterRow.getChildren().get(filterRow.getChildren().size() - 1);
        
        addButton.setOnAction(e -> {
            HBox newRow = createFilterRow();
            advancedFiltersContainer.getChildren().add(newRow);
            advancedFilterRows.add(newRow);
        });
    }
    
    private HBox createFilterRow() {
        HBox row = new HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("advanced-filter-row");
        
        // Add components similar to the first row
        ComboBox<String> fieldCombo = new ComboBox<>();
        fieldCombo.setPromptText("Field");
        fieldCombo.setItems(FXCollections.observableArrayList(
            "Message", "User ID", "IP Address", "Session ID"
        ));
        
        ComboBox<String> operatorCombo = new ComboBox<>();
        operatorCombo.setPromptText("Operator");
        operatorCombo.setItems(FXCollections.observableArrayList(
            "Contains", "Equals", "Starts with", "Ends with", "Regex"
        ));
        
        TextField valueField = new TextField();
        valueField.setPromptText("Value");
        
        Button removeButton = new Button("-");
        removeButton.getStyleClass().add("remove-filter-btn");
        removeButton.setOnAction(e -> advancedFiltersContainer.getChildren().remove(row));
        
        row.getChildren().addAll(fieldCombo, operatorCombo, valueField, removeButton);
        return row;
    }
    
    private void setupButtons() {
        resetBtn.setOnAction(event -> resetFilters());
        applyBtn.setOnAction(event -> applyFilters());
    }
    
    private void resetFilters() {
        // Reset UI controls
        timePresetCombo.setValue(null);
        startDate.setValue(null);
        endDate.setValue(null);
        startTime.setText("");
        endTime.setText("");
        errorCheck.setSelected(true);
        warnCheck.setSelected(true);
        infoCheck.setSelected(true);
        debugCheck.setSelected(true);
        traceCheck.setSelected(true);
        sourceCombo.setValue(null);
        componentCombo.setValue(null);
        
        // Clear advanced filters
        advancedFiltersContainer.getChildren().clear();
        addFilterRow();

        // Simply disable filtering and show all logs
        filterActive = false;
        filteredLogsTable.setItems(allLogs);
    }
    private void setupExportButton() {
        exportCsvBtn.setOnAction(event -> exportToCSV());
    }

    private void exportToCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save CSV File");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );
        fileChooser.setInitialFileName("log_export_" + 
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

        File file = fileChooser.showSaveDialog(exportCsvBtn.getScene().getWindow());
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Write CSV header
                writer.write("Timestamp,IP,Request Type,Endpoint,Status Code,Response Time,Log Level\n");
                
                // Write data
                ObservableList<LogEntry> logsToExport = filterActive ? filteredLogs : allLogs;
                for (LogEntry log : logsToExport) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                        log.getTimestamp(),
                        log.getIpAddress(),
                        log.getRequestType(),
                        log.getEndpoint(),
                        log.getStatusCode(),
                        log.getResponseTime(),
                        log.getLogLevel()
                    ));
                }
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText(null);
                alert.setContentText("Logs have been successfully exported to CSV!");
                alert.showAndWait();
                
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText(null);
                alert.setContentText("An error occurred while exporting the logs: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void setupTable() {
        // Set up cell value factories for each column
        columnIP.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        columnTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        columnRequestType.setCellValueFactory(new PropertyValueFactory<>("requestType"));
        columnEndpoint.setCellValueFactory(new PropertyValueFactory<>("endpoint"));
        columnStatusCode.setCellValueFactory(new PropertyValueFactory<>("statusCode"));
        columnResponseTime.setCellValueFactory(new PropertyValueFactory<>("responseTime"));
        columnLogLevel.setCellValueFactory(new PropertyValueFactory<>("logLevel"));

        // Initialize the table with the observable list
        filteredLogsTable.setItems(allLogs);
    }
    
    private void setupKafkaConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "filter-consumer-group");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("max.poll.records", "500");
        props.put("fetch.min.bytes", "1");
        props.put("fetch.max.wait.ms", "500");
        props.put("session.timeout.ms", "30000");
        props.put("heartbeat.interval.ms", "3000");
        props.put("max.partition.fetch.bytes", "1048576");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("logs-topic"));
        
        // Start the consumer thread
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::consumeLogMessages);
        
        System.out.println("Filter: Kafka consumer initialized with config:");
        props.forEach((key, value) -> System.out.println(key + " = " + value));
    }

    private void consumeLogMessages() {
        System.out.println("Filter: Starting log consumption...");
        while (isRunning) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                if (!records.isEmpty()) {
                    List<LogEntry> newLogs = new ArrayList<>();
                    for (ConsumerRecord<String, String> record : records) {
                        LogEntry logEntry = parseLogEntry(record.value());
                        if (logEntry != null) {
                            newLogs.add(logEntry);
                        }
                    }
                    
                    if (!newLogs.isEmpty()) {
                        Platform.runLater(() -> {
                            allLogs.addAll(newLogs);
                            if (!filterActive) {
                                filteredLogsTable.setItems(allLogs);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Filter: Error consuming messages: " + e.getMessage());
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println("Filter: Log consumption stopped.");
    }
    
    private LogEntry parseLogEntry(String logMessage) {
        try {
            System.out.println("Parsing log message: " + logMessage);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(logMessage);
            
            String ipAddress = jsonNode.get("client_ip").asText();
            String timestamp = jsonNode.get("timestamp").asText();
            String requestType = jsonNode.get("http_method").asText();
            String endpoint = jsonNode.get("request").asText();
            String statusCode = jsonNode.get("status_code").asText();
            String responseTime = jsonNode.get("response_size").asText() + "ms";
            String logLevel = jsonNode.get("log_level").asText();
            String source = deriveSource(endpoint);
            String component = deriveComponent(endpoint);
            String message = jsonNode.get("message").asText();
            String userId = jsonNode.get("user_id").asText();
            String sessionId = "N/A";  // Not provided in JSON

            LogEntry entry = new LogEntry(ipAddress, timestamp, requestType, endpoint, statusCode, 
                              responseTime, logLevel, source, component, message, userId, sessionId);
            System.out.println("Successfully created log entry: " + entry);
            return entry;
        } catch (Exception e) {
            System.err.println("Error parsing log message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String deriveSource(String endpoint) {
        if (endpoint.startsWith("/api")) return "Application";
        if (endpoint.startsWith("/usr")) return "User Service";
        if (endpoint.startsWith("/cart")) return "Shopping Cart";
        if (endpoint.startsWith("/checkout")) return "Checkout Service";
        return "System";
    }

    private String deriveComponent(String endpoint) {
        if (endpoint.contains("/api/users")) return "Frontend";
        if (endpoint.contains("/api/orders")) return "Backend";
        if (endpoint.contains("/api/products")) return "Backend";
        if (endpoint.contains("/api/admin")) return "Admin";
        if (endpoint.contains("/api/auth")) return "Authentication";
        return "Other";
    }
    
    private void setupNavigation() {
        dashboardLabel.setOnMouseClicked(event -> {
            try {
                stop();  // Clean up resources before navigation
                MainApplication.setRoot("logAnalytics");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        logsLabel.setOnMouseClicked(event -> {
            try {
                stop();  // Clean up resources before navigation
                MainApplication.setRoot("logVis");
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

    public void stop() {
        System.out.println("Stopping Kafka consumer and cleaning up resources...");
        isRunning = false;
        if (consumer != null) {
            try {
                consumer.close(Duration.ofSeconds(5));  // Give it 5 seconds to close gracefully
            } catch (Exception e) {
                System.err.println("Error closing consumer: " + e.getMessage());
            }
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
        System.out.println("Resources cleaned up successfully");
    }

    @FXML
    private void applyFilters() {
        System.out.println("\n=== Applying Filters ===");
        System.out.println("Start Date: " + startDate.getValue());
        System.out.println("End Date: " + endDate.getValue());
        System.out.println("Current logs size: " + allLogs.size());

        // Mark filter as active
        filterActive = true;

        List<LogEntry> filteredData = allLogs.stream()
            .filter(entry -> {
                // Apply time range filter
                if (startDate.getValue() != null || endDate.getValue() != null) {
                    try {
                        LocalDateTime entryTime = parseTimestamp(entry.getTimestamp());
                        if (entryTime == null) {
                            System.out.println("Skipping entry - couldn't parse timestamp: " + entry.getTimestamp());
                            return false;
                        }
                        
                        // Set default times if not specified
                        LocalDateTime filterStart = startDate.getValue() != null ? 
                            LocalDateTime.of(startDate.getValue(), LocalTime.MIN) :
                            LocalDateTime.MIN;
                            
                        LocalDateTime filterEnd = endDate.getValue() != null ? 
                            LocalDateTime.of(endDate.getValue(), LocalTime.MAX) :
                            LocalDateTime.MAX;
                        
                        if (entryTime.isBefore(filterStart) || entryTime.isAfter(filterEnd)) {
                            return false;
                        }
                    } catch (Exception e) {
                        System.err.println("Error in date filtering: " + e.getMessage());
                        return false;
                    }
                }

                // Apply log level filters
                String level = entry.getLogLevel().toLowerCase();
                if ((!errorCheck.isSelected() && level.equals("error")) ||
                    (!warnCheck.isSelected() && level.equals("warn")) ||
                    (!infoCheck.isSelected() && level.equals("info")) ||
                    (!debugCheck.isSelected() && level.equals("debug")) ||
                    (!traceCheck.isSelected() && level.equals("trace"))) {
                    return false;
                }

                // Apply source filter
                if (sourceCombo.getValue() != null && !sourceCombo.getValue().isEmpty()) {
                    if (!entry.getSource().equals(sourceCombo.getValue())) {
                        return false;
                    }
                }

                // Apply component filter
                if (componentCombo.getValue() != null && !componentCombo.getValue().isEmpty()) {
                    if (!entry.getComponent().equals(componentCombo.getValue())) {
                        return false;
                    }
                }

                return true;
            })
            .collect(Collectors.toList());

        System.out.println("Filtered data size: " + filteredData.size());

        // Update the filtered logs
        Platform.runLater(() -> {
            filteredLogs.setAll(filteredData);
            filteredLogsTable.setItems(filteredLogs);
        });
    }

    @FXML
    private void clearFilters() {
        // Reset all filters
        startDate.setValue(null);
        endDate.setValue(null);
        errorCheck.setSelected(true);
        warnCheck.setSelected(true);
        infoCheck.setSelected(true);
        debugCheck.setSelected(true);
        traceCheck.setSelected(true);
        sourceCombo.setValue(null);
        componentCombo.setValue(null);
        
        // Clear filter active flag
        filterActive = false;
        
        // Show all logs
        Platform.runLater(() -> {
            filteredLogsTable.setItems(allLogs);
        });
    }
    
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            System.out.println("Raw timestamp: " + timestamp);
            
            // Extract the actual timestamp from the JSON-like format
            if (timestamp.contains("\"timestamp\":")) {
                timestamp = timestamp.split("\"timestamp\":\"")[1].split("\"")[0];
            }
            
            System.out.println("Extracted timestamp: " + timestamp);
            
            // Remove any surrounding brackets and extra spaces
            timestamp = timestamp.trim().replaceAll("[\\[\\]]", "");
            
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
                return LocalDateTime.parse(timestamp, formatter);
            } catch (Exception e) {
                System.out.println("Parse error: " + e.getMessage());
                // If timezone parsing fails, try without timezone
                String dateTimePart = timestamp.split(" \\+")[0];
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss", Locale.ENGLISH);
                return LocalDateTime.parse(dateTimePart, formatter);
            }
        } catch (Exception e) {
            System.err.println("Error parsing timestamp: " + timestamp + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            return LocalTime.MIN;
        }
    }
    
    private String getFieldValue(LogEntry entry, String field) {
        switch (field) {
            case "Message": return entry.getMessage();
            case "User ID": return entry.getUserId();
            case "IP Address": return entry.getIpAddress();
            case "Session ID": return entry.getSessionId();
            default: return "";
        }
    }
    
    private boolean matchesFilter(String value, String operator, String filterValue) {
        if (value == null) return false;
        
        switch (operator) {
            case "Contains":
                return value.toLowerCase().contains(filterValue.toLowerCase());
            case "Equals":
                return value.equalsIgnoreCase(filterValue);
            case "Starts with":
                return value.toLowerCase().startsWith(filterValue.toLowerCase());
            case "Ends with":
                return value.toLowerCase().endsWith(filterValue.toLowerCase());
            case "Regex":
                try {
                    return value.matches(filterValue);
                } catch (Exception e) {
                    return false;
                }
            default:
                return false;
        }
    }

    public void enableCustomTimeRange(boolean enable) {
        startDate.setDisable(!enable);
        endDate.setDisable(!enable);
        startTime.setDisable(!enable);
        endTime.setDisable(!enable);
    }

    // Inner class for advanced filters
    public static class AdvancedFilter {
        private String field;
        private String operator;
        private String value;
        
        public AdvancedFilter(String field, String operator, String value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }
        
        @Override
        public String toString() {
            return field + " " + operator + " " + value;
        }
    }
    
    public static class LogEntry {
        private String ipAddress;
        private String timestamp;
        private String requestType;
        private String endpoint;
        private String statusCode;
        private String responseTime;
        private String logLevel;
        private String source;
        private String component;
        private String message;
        private String userId;
        private String sessionId;
        
        public LogEntry(String ipAddress, String timestamp, String requestType, String endpoint, String statusCode, String responseTime, String logLevel, String source, String component, String message, String userId, String sessionId) {
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
            this.requestType = requestType;
            this.endpoint = endpoint;
            this.statusCode = statusCode;
            this.responseTime = responseTime;
            this.logLevel = logLevel;
            this.source = source;
            this.component = component;
            this.message = message;
            this.userId = userId;
            this.sessionId = sessionId;
        }
        
        public String getIpAddress() { return ipAddress; }
        public String getTimestamp() { return timestamp; }
        public String getRequestType() { return requestType; }
        public String getEndpoint() { return endpoint; }
        public String getStatusCode() { return statusCode; }
        public String getResponseTime() { return responseTime; }
        public String getLogLevel() { return logLevel; }
        public String getSource() { return source; }
        public String getComponent() { return component; }
        public String getMessage() { return message; }
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
    }
}