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
    @FXML private BubbleChart<Number, Number> responseSizeChart;
    @FXML private StackedAreaChart<String, Number> actionDistributionChart;
    @FXML private GridPane methodStatusHeatmap;
    @FXML private PolarChart userAgentChart;

    private Map<String, Integer> logLevelCounts = new HashMap<>();
    private Map<String, Integer> httpMethodCounts = new HashMap<>();
    private Map<String, Integer> statusCodeCounts = new HashMap<>();
    private Map<String, EndpointStats> endpointStats = new HashMap<>();
    private LinkedList<LogEntry> recentLogs = new LinkedList<>();
    private static final int MAX_TIME_SERIES_POINTS = 20;
    private Map<String, Map<String, Integer>> methodStatusMatrix = new HashMap<>();
    private Map<String, Integer> actionCounts = new HashMap<>();
    private Map<String, Integer> userAgentCounts = new HashMap<>();

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
        // Initialize new charts
        setupResponseSizeBubbleChart();
        setupActionDistributionChart();
        setupMethodStatusHeatmap();
        setupUserAgentRadarChart();
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
        // Update new charts
        setupResponseSizeBubbleChart();
        setupActionDistributionChart();
        setupMethodStatusHeatmap();
        setupUserAgentRadarChart();
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

    private void setupResponseSizeBubbleChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Response Size (KB)");
        yAxis.setLabel("Status Code");
        
        responseSizeChart.setTitle("Response Size vs Status Code Distribution");
        responseSizeChart.getStyleClass().add("modern-bubble-chart");
        
        ObservableList<BubbleChart.Series<Number, Number>> bubbleData = FXCollections.observableArrayList();
        Map<String, List<Double>> responseData = new HashMap<>();
        
        for (LogEntry log : recentLogs) {
            double responseSize = Double.parseDouble(log.getResponseSize()) / 1024.0; // Convert to KB
            int statusCode = Integer.parseInt(log.getStatusCode());
            responseData.computeIfAbsent(log.getStatusCode(), k -> new ArrayList<>()).add(responseSize);
        }
        
        for (Map.Entry<String, List<Double>> entry : responseData.entrySet()) {
            BubbleChart.Series<Number, Number> series = new BubbleChart.Series<>();
            series.setName("Status " + entry.getKey());
            
            double avgSize = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double frequency = entry.getValue().size();
            
            series.getData().add(new BubbleChart.Data<>(avgSize, Integer.parseInt(entry.getKey()), frequency / 5));
            bubbleData.add(series);
        }
        
        responseSizeChart.setData(bubbleData);
    }

    private void setupActionDistributionChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Number of Actions");
        
        actionDistributionChart.setTitle("Action Distribution Over Time");
        actionDistributionChart.getStyleClass().add("modern-area-chart");
        
        Map<String, Map<String, Integer>> timeActionCounts = new TreeMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        
        for (LogEntry log : recentLogs) {
            String timeKey = log.getTimestamp().format(timeFormatter);
            String action = log.getAction();
            
            timeActionCounts.computeIfAbsent(timeKey, k -> new HashMap<>())
                           .merge(action, 1, Integer::sum);
        }
        
        Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Integer>> timeEntry : timeActionCounts.entrySet()) {
            for (Map.Entry<String, Integer> actionEntry : timeEntry.getValue().entrySet()) {
                seriesMap.computeIfAbsent(actionEntry.getKey(), k -> {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName(k);
                    return series;
                }).getData().add(new XYChart.Data<>(timeEntry.getKey(), actionEntry.getValue()));
            }
        }
        
        actionDistributionChart.getData().addAll(seriesMap.values());
    }

    private void setupMethodStatusHeatmap() {
        methodStatusHeatmap.getChildren().clear();
        methodStatusHeatmap.setStyle("-fx-background-color: white; -fx-padding: 10;");
        
        // Create headers
        int row = 0;
        int col = 0;
        
        Set<String> methods = new TreeSet<>();
        Set<String> statuses = new TreeSet<>();
        
        for (LogEntry log : recentLogs) {
            methods.add(log.getHttpMethod());
            statuses.add(log.getStatusCode());
        }
        
        // Add column headers (HTTP Methods)
        for (String method : methods) {
            Label label = new Label(method);
            label.getStyleClass().add("heatmap-header");
            methodStatusHeatmap.add(label, ++col, 0);
        }
        
        // Add row headers (Status Codes)
        row = 0;
        for (String status : statuses) {
            Label label = new Label(status);
            label.getStyleClass().add("heatmap-header");
            methodStatusHeatmap.add(label, 0, ++row);
            
            // Calculate and add heatmap cells
            col = 0;
            for (String method : methods) {
                int count = methodStatusMatrix
                    .getOrDefault(method, new HashMap<>())
                    .getOrDefault(status, 0);
                
                StackPane cell = createHeatmapCell(count, 
                    methods.stream().mapToInt(m -> 
                        methodStatusMatrix
                            .getOrDefault(m, new HashMap<>())
                            .getOrDefault(status, 0))
                    .max()
                    .orElse(1));
                
                methodStatusHeatmap.add(cell, ++col, row);
            }
        }
    }

    private StackPane createHeatmapCell(int value, int maxValue) {
        StackPane cell = new StackPane();
        double opacity = Math.min(1.0, value / (double) maxValue);
        
        cell.setStyle(String.format(
            "-fx-background-color: rgba(33, 150, 243, %.2f); " +
            "-fx-min-width: 50; -fx-min-height: 50; " +
            "-fx-border-color: white; -fx-border-width: 1;",
            opacity));
        
        Label label = new Label(String.valueOf(value));
        label.setStyle("-fx-text-fill: " + (opacity > 0.5 ? "white" : "#333") + ";");
        
        cell.getChildren().add(label);
        return cell;
    }

    private void setupUserAgentRadarChart() {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        
        for (Map.Entry<String, Integer> entry : userAgentCounts.entrySet()) {
            String browser = extractBrowserInfo(entry.getKey());
            pieData.add(new PieChart.Data(browser, entry.getValue()));
        }
        
        userAgentChart.setData(pieData);
        userAgentChart.setTitle("User Agent Distribution");
        userAgentChart.getStyleClass().add("modern-polar-chart");
    }

    private String extractBrowserInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) return "IE";
        return "Other";
    }

    @Override
    public void updateCharts(List<LogEntry> logs) {
        // Update new charts
        recentLogs = new LinkedList<>(logs);
        
        // Update method-status matrix
        methodStatusMatrix.clear();
        for (LogEntry log : logs) {
            methodStatusMatrix
                .computeIfAbsent(log.getHttpMethod(), k -> new HashMap<>())
                .merge(log.getStatusCode(), 1, Integer::sum);
        }
        
        // Update action counts
        actionCounts.clear();
        for (LogEntry log : logs) {
            actionCounts.merge(log.getAction(), 1, Integer::sum);
        }
        
        // Update user agent counts
        userAgentCounts.clear();
        for (LogEntry log : logs) {
            userAgentCounts.merge(log.getUserAgent(), 1, Integer::sum);
        }
        
        Platform.runLater(() -> {
            setupResponseSizeBubbleChart();
            setupActionDistributionChart();
            setupMethodStatusHeatmap();
            setupUserAgentRadarChart();
        });
    }
}