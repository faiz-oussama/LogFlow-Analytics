package com.example;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.time.format.DateTimeFormatter;
import javafx.application.Platform;
import javafx.scene.layout.*;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Section;

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
    @FXML private StackedBarChart<String, Number> actionDistributionChart;
    @FXML private GridPane methodStatusHeatmap;
    @FXML private Pane polarChartContainer;
    private Gauge polarGauge;

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
        setupPolarChart();
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
        setupPolarChart();
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
        // Clear existing data
        logLevelCounts.clear();
        httpMethodCounts.clear();
        statusCodeCounts.clear();
        endpointStats.clear();
        
        // Get current time
        long currentTime = System.currentTimeMillis();
        long filterTime;
        
        // Calculate filter time based on selected range
        switch (timeRangeComboBox.getValue()) {
            case "Last 15 minutes":
                filterTime = currentTime - (15 * 60 * 1000);
                break;
            case "Last hour":
                filterTime = currentTime - (60 * 60 * 1000);
                break;
            case "Last 24 hours":
                filterTime = currentTime - (24 * 60 * 60 * 1000);
                break;
            case "Last 7 days":
                filterTime = currentTime - (7 * 24 * 60 * 60 * 1000);
                break;
            default:
                filterTime = currentTime - (60 * 60 * 1000); // Default to last hour
        }

        // Filter and process logs
        List<LogEntry> filteredLogs = recentLogs.stream()
            .filter(log -> {
                try {
                    long logTime = Long.parseLong(log.getTimestamp());
                    return logTime >= filterTime;
                } catch (NumberFormatException e) {
                    return false;
                }
            })
            .collect(java.util.stream.Collectors.toList());

        // Process filtered logs
        filteredLogs.forEach(this::processNewLog);
        
        // Update all charts
        updateCharts();
        updateSummaryLabels();
    }

    private void setupResponseSizeBubbleChart() {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        recentLogs.forEach(log -> {
            try {
                double responseSize = Double.parseDouble(log.getResponseSize()) / 1024.0; // Convert to KB
                series.getData().add(new XYChart.Data<>(
                    responseSize,
                    (double) log.getStatusCode(),
                    Math.min(responseSize * 2, 20) // Bubble size proportional to response size
                ));
            } catch (NumberFormatException e) {
                // Skip invalid entries
            }
        });
        responseSizeChart.getData().clear();
        responseSizeChart.getData().add(series);
    }

    private void setupActionDistributionChart() {
        actionDistributionChart.getData().clear();
        Map<String, Map<String, Integer>> timeSeriesData = new TreeMap<>();
        
        recentLogs.forEach(log -> {
            String timeKey = log.getTimestamp().substring(11, 16); // HH:mm format
            String action = log.getHttpMethod();
            
            timeSeriesData.computeIfAbsent(timeKey, k -> new HashMap<>())
                         .merge(action, 1, Integer::sum);
        });

        Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();
        
        timeSeriesData.forEach((time, actions) -> {
            actions.forEach((action, count) -> {
                XYChart.Series<String, Number> series = seriesMap.computeIfAbsent(action,
                    k -> {
                        XYChart.Series<String, Number> s = new XYChart.Series<>();
                        s.setName(action);
                        return s;
                    });
                series.getData().add(new XYChart.Data<>(time, count));
            });
        });
        
        actionDistributionChart.getData().addAll(seriesMap.values());
    }

    private void setupMethodStatusHeatmap() {
        methodStatusHeatmap.getChildren().clear();
        methodStatusHeatmap.getColumnConstraints().clear();
        methodStatusHeatmap.getRowConstraints().clear();

        // Calculate method-status matrix
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        int maxValue = 0;

        for (LogEntry log : recentLogs) {
            String method = log.getHttpMethod();
            String statusGroup = String.valueOf(log.getStatusCode()).substring(0, 1) + "xx";
            
            matrix.computeIfAbsent(method, k -> new HashMap<>())
                 .merge(statusGroup, 1, Integer::sum);
            
            maxValue = Math.max(maxValue,
                matrix.get(method).getOrDefault(statusGroup, 0));
        }

        // Create headers
        int row = 0;
        int col = 0;
        
        // Add method headers
        for (String method : matrix.keySet()) {
            methodStatusHeatmap.add(new Label(method), ++col, 0);
        }
        
        // Add status code headers
        String[] statusGroups = {"2xx", "3xx", "4xx", "5xx"};
        for (String status : statusGroups) {
            methodStatusHeatmap.add(new Label(status), 0, ++row);
        }

        // Add heatmap cells
        for (String method : matrix.keySet()) {
            Map<String, Integer> statusCounts = matrix.get(method);
            row = 0;
            for (String status : statusGroups) {
                int value = statusCounts.getOrDefault(status, 0);
                methodStatusHeatmap.add(
                    createHeatmapCell(value, maxValue),
                    col, ++row
                );
            }
            col++;
        }
    }

    private Pane createHeatmapCell(int value, int maxValue) {
        Pane cell = new Pane();
        cell.setPrefSize(40, 40);
        
        double intensity = maxValue > 0 ? (double) value / maxValue : 0;
        String color = String.format("rgb(0, %.0f, 0)", intensity * 255);
        
        cell.setStyle("-fx-background-color: " + color + ";");
        
        Label label = new Label(String.valueOf(value));
        label.setStyle("-fx-text-fill: white;");
        cell.getChildren().add(label);
        
        return cell;
    }

    private void setupPolarChart() {
        if (polarGauge == null) {
            polarGauge = GaugeBuilder.create()
                .skinType(Gauge.SkinType.GAUGE)
                .prefSize(200, 200)
                .sections(
                    new Section(0, 33.3, "Low", javafx.scene.paint.Color.GREEN),
                    new Section(33.3, 66.6, "Medium", javafx.scene.paint.Color.YELLOW),
                    new Section(66.6, 100, "High", javafx.scene.paint.Color.RED)
                )
                .build();
            polarChartContainer.getChildren().add(polarGauge);
        }

        // Calculate success rate
        long totalRequests = recentLogs.size();
        long successfulRequests = recentLogs.stream()
            .filter(log -> log.getStatusCode() >= 200 && log.getStatusCode() < 300)
            .count();

        double successRate = totalRequests > 0 
            ? (double) successfulRequests / totalRequests * 100 
            : 0;

        polarGauge.setValue(successRate);
    }

    public void updateCharts(List<LogEntry> logs) {
        // Update new charts
        recentLogs = new LinkedList<>(logs);
        
        // Update method-status matrix
        methodStatusMatrix.clear();
        for (LogEntry log : logs) {
            methodStatusMatrix
                .computeIfAbsent(log.getHttpMethod(), k -> new HashMap<>())
                .merge(String.valueOf(log.getStatusCode()), 1, Integer::sum);
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
            setupPolarChart();
        });
    }
}