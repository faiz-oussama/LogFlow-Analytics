package com.example;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;

public class LogAnalyticsController {
    @FXML private PieChart logLevelChart;
    @FXML private BarChart<String, Number> httpMethodsChart;
    @FXML private LineChart<String, Number> timeSeriesChart;
    @FXML private BarChart<String, Number> statusCodesChart;
    @FXML private TableView<EndpointStats> endpointsTable;
    @FXML private TableColumn<EndpointStats, String> endpointColumn;
    @FXML private TableColumn<EndpointStats, Integer> hitsColumn;
    @FXML private TableColumn<EndpointStats, Double> avgResponseTimeColumn;
    @FXML private TableColumn<EndpointStats, String> errorRateColumn;
    @FXML private ComboBox<String> timeRangeComboBox;
    @FXML private Label totalLogsLabel;
    @FXML private Label errorRateLabel;
    @FXML private Label avgResponseTimeLabel;
    @FXML private Label logsLabel;
    @FXML private Label dashboardLabel;
    @FXML private Label timeRangeLabel;
    @FXML private Label filterLabel;
    @FXML private Label chatbotLabel;

    private Map<String, Integer> logLevelCounts = new HashMap<>();
    private Map<String, Integer> httpMethodCounts = new HashMap<>();
    private Map<String, Integer> statusCodeCounts = new HashMap<>();
    private Map<String, EndpointStats> endpointStats = new HashMap<>();
    private LinkedList<LogEntry> recentLogs = new LinkedList<>();
    private static final int MAX_TIME_SERIES_POINTS = 20;

    @FXML
    public void initialize() {
        setupTimeRangeComboBox();
        setupTableColumns();
        initializeCharts();
        setupNavigation();
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
    private void setupTimeRangeComboBox() {
        timeRangeComboBox.setItems(FXCollections.observableArrayList(
            "Last 15 minutes", "Last hour", "Last 24 hours", "Last 7 days"
        ));
        timeRangeComboBox.setValue("Last hour");
        timeRangeComboBox.setOnAction(e -> refreshCharts());
    }

    private void setupTableColumns() {
        endpointColumn.setCellValueFactory(data -> data.getValue().endpointProperty());
        hitsColumn.setCellValueFactory(data -> data.getValue().hitsProperty().asObject());
        avgResponseTimeColumn.setCellValueFactory(data -> data.getValue().avgResponseTimeProperty().asObject());
        errorRateColumn.setCellValueFactory(data -> data.getValue().errorRateProperty());
    }

    private void initializeCharts() {
        // Initialize empty charts
        updateLogLevelChart();
        updateHttpMethodsChart();
        updateStatusCodesChart();
        updateTimeSeriesChart();
    }

    public void processNewLog(LogEntry logEntry) {
        Platform.runLater(() -> {
            updateCounters(logEntry);
            updateEndpointStats(logEntry);
            updateCharts();
            updateSummaryLabels();
        });
    }

    private void updateCounters(LogEntry logEntry) {
        // Update log level counts
        logLevelCounts.merge(logEntry.getLogLevel(), 1, Integer::sum);
        
        // Update HTTP method counts
        httpMethodCounts.merge(logEntry.getHttpMethod(), 1, Integer::sum);
        
        // Update status code counts
        String statusCode = String.valueOf(logEntry.getStatusCode());
        statusCodeCounts.merge(statusCode, 1, Integer::sum);

        // Maintain recent logs for time series
        recentLogs.add(logEntry);
        if (recentLogs.size() > MAX_TIME_SERIES_POINTS) {
            recentLogs.removeFirst();
        }
    }

    private void updateEndpointStats(LogEntry logEntry) {
        String endpoint = logEntry.getRequest(); // Using request as endpoint
        EndpointStats stats = endpointStats.computeIfAbsent(endpoint, 
            k -> new EndpointStats(endpoint));
        
        stats.incrementHits();
        stats.updateResponseTime(Long.parseLong(logEntry.getResponseSize())); // Using response size as response time
        if (logEntry.getStatusCode() >= 400) {
            stats.incrementErrors();
        }

        // Update table
        ObservableList<EndpointStats> tableData = FXCollections.observableArrayList(
            endpointStats.values()
        );
        tableData.sort((a, b) -> b.getHits() - a.getHits());
        endpointsTable.setItems(tableData);
    }

    private void updateCharts() {
        updateLogLevelChart();
        updateHttpMethodsChart();
        updateStatusCodesChart();
        updateTimeSeriesChart();
    }

    private void updateLogLevelChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        logLevelCounts.forEach((level, count) -> 
            pieChartData.add(new PieChart.Data(level, count))
        );
        logLevelChart.setData(pieChartData);
    }

    private void updateHttpMethodsChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        httpMethodCounts.forEach((method, count) -> 
            series.getData().add(new XYChart.Data<>(method, count))
        );
        httpMethodsChart.getData().clear();
        httpMethodsChart.getData().add(series);
    }

    private void updateStatusCodesChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        statusCodeCounts.forEach((code, count) -> 
            series.getData().add(new XYChart.Data<>(code, count))
        );
        statusCodesChart.getData().clear();
        statusCodesChart.getData().add(series);
    }

    private void updateTimeSeriesChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        Map<String, Integer> timePoints = new LinkedHashMap<>();
        recentLogs.forEach(log -> {
            String timeKey = log.getTimestamp().substring(11, 19); // Extract time from timestamp
            timePoints.merge(timeKey, 1, Integer::sum);
        });

        timePoints.forEach((time, count) -> 
            series.getData().add(new XYChart.Data<>(time, count))
        );

        timeSeriesChart.getData().clear();
        timeSeriesChart.getData().add(series);
    }

    private void updateSummaryLabels() {
        int totalLogs = logLevelCounts.values().stream().mapToInt(Integer::intValue).sum();
        totalLogsLabel.setText(String.valueOf(totalLogs));

        int totalErrors = statusCodeCounts.entrySet().stream()
            .filter(e -> e.getKey().startsWith("4") || e.getKey().startsWith("5"))
            .mapToInt(Map.Entry::getValue)
            .sum();
        double errorRate = totalLogs > 0 ? (double) totalErrors / totalLogs * 100 : 0;
        errorRateLabel.setText(String.format("%.1f%%", errorRate));

        double avgResponseTime = endpointStats.values().stream()
            .mapToDouble(EndpointStats::getAvgResponseTime)
            .average()
            .orElse(0.0);
        avgResponseTimeLabel.setText(String.format("%.0fms", avgResponseTime));
    }

    private void refreshCharts() {
        // Implement time-based filtering based on selected time range
        String selectedRange = timeRangeComboBox.getValue();
        // Reset counters and recalculate based on time range
        // This would filter logs based on the selected time range
    }
}