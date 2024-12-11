package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogEntry {
    @JsonProperty("_id")
    private String id;

    @JsonProperty("client_ip")
    private String clientIp;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("http_method")
    private String httpMethod;

    @JsonProperty("request")
    private String request;

    @JsonProperty("status_code")
    private int statusCode;

    @JsonProperty("response_size")
    private String responseSize;

    @JsonProperty("referrer")
    private String referrer;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("log_level")
    private String logLevel;

    @JsonProperty("http_version")
    private String httpVersion;

    @JsonProperty("message")
    private String message;

    @JsonProperty("ecs")
    private Ecs ecs;

    // Getters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequest() {
        return request;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseSize() {
        return responseSize;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public String getMessage() {
        return message;
    }

    public Ecs getEcs() {
        return ecs;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id='" + id + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", request='" + request + '\'' +
                ", statusCode='" + statusCode + '\'' +
                ", responseSize='" + responseSize + '\'' +
                ", referrer='" + referrer + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", logLevel='" + logLevel + '\'' +
                ", httpVersion='" + httpVersion + '\'' +
                ", message='" + message + '\'' +
                ", ecs=" + ecs +
                '}';
    }

    public static class Ecs {
        @JsonProperty("version")
        private String version;

        // Getters
        public String getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "Ecs{" +
                    "version='" + version + '\'' +
                    '}';
        }
    }
}