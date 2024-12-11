package com.example;

import javafx.beans.property.*;

public class EndpointStats {
    private final StringProperty endpoint;
    private final StringProperty method;
    private final IntegerProperty hits;
    private final DoubleProperty avgResponseTime;
    private final StringProperty errorRate;
    private final StringProperty status;
    private int totalResponseTime;
    private int errorCount;

    public EndpointStats(String endpoint) {
        this.endpoint = new SimpleStringProperty(endpoint);
        this.method = new SimpleStringProperty("");
        this.hits = new SimpleIntegerProperty(0);
        this.avgResponseTime = new SimpleDoubleProperty(0.0);
        this.errorRate = new SimpleStringProperty("0%");
        this.status = new SimpleStringProperty("SUCCESS");
        this.totalResponseTime = 0;
        this.errorCount = 0;
    }

    public void incrementHits() {
        hits.set(hits.get() + 1);
        updateErrorRate();
    }

    public void incrementErrors() {
        errorCount++;
        updateErrorRate();
    }

    public void updateResponseTime(long responseTime) {
        totalResponseTime += responseTime;
        avgResponseTime.set((double) totalResponseTime / hits.get());
    }

    private void updateErrorRate() {
        double rate = (double) errorCount / hits.get() * 100;
        errorRate.set(String.format("%.1f%%", rate));
    }

    public void setMethod(String method) {
        this.method.set(method);
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    // Getters
    public String getEndpoint() { return endpoint.get(); }
    public String getMethod() { return method.get(); }
    public int getHits() { return hits.get(); }
    public double getAvgResponseTime() { return avgResponseTime.get(); }
    public String getErrorRate() { return errorRate.get(); }
    public String getStatus() { return status.get(); }

    // Property getters
    public StringProperty endpointProperty() { return endpoint; }
    public StringProperty methodProperty() { return method; }
    public IntegerProperty hitsProperty() { return hits; }
    public DoubleProperty avgResponseTimeProperty() { return avgResponseTime; }
    public StringProperty errorRateProperty() { return errorRate; }
    public StringProperty statusProperty() { return status; }
}
