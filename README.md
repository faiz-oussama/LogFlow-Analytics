# Real-Time Log Streaming and Analytics Platform

A sophisticated Java-based application that combines the power of the ELK Stack (Elasticsearch, Logstash, Filebeat) with Apache Kafka for comprehensive log management, real-time streaming, visualization, and analysis.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [Components](#components)
- [Data Flow](#data-flow)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Overview

This project implements an enterprise-grade log management and analytics platform that leverages both the ELK Stack and Apache Kafka to provide a complete solution for log processing, storage, and visualization. The system uses Filebeat to collect logs, Logstash for processing and enrichment, Elasticsearch for storage and search, and Kafka for reliable real-time streaming.

### Key Capabilities
- Automated log collection using Filebeat
- Advanced log processing and enrichment via Logstash
- Scalable storage and search with Elasticsearch
- Real-time streaming through Apache Kafka
- Dynamic filtering and search functionality
- Interactive data visualization
- Customizable analytics dashboard
- High-performance data processing
- Reliable log shipping and processing

## Features

### 1. Log Collection and Processing
- Automated log collection with Filebeat
- Log parsing and enrichment through Logstash
- Full-text search capabilities via Elasticsearch
- Real-time log ingestion using Apache Kafka
- Configurable consumer groups for scalable processing
- Automatic reconnection handling
- Buffered message processing

### 2. Filtering System
- Multi-criteria filtering:
  - Time range (custom and preset options)
  - Log levels (ERROR, WARN, INFO, DEBUG, TRACE)
  - Source and component filtering
  - Advanced custom filters
- Real-time filter application
- Filter persistence

### 3. User Interface
- Modern JavaFX-based UI
- Responsive design
- Three main views:
  - Log Visualization
  - Filter Interface
  - Analytics Dashboard
- Interactive table with sortable columns
- Custom styling and themes

## Technology Stack

### Backend
- Java 11+
- Apache Kafka 3.x
- Jackson JSON Parser
- Maven for dependency management
- Elasticsearch 7.x
- Logstash 7.x
- Filebeat 7.x
- Elasticsearch Java API Client

### Frontend
- JavaFX 17
- FXML for UI layout
- CSS for styling

### Development Tools
- IDE: Any Java IDE (Eclipse, IntelliJ IDEA, VS Code)
- Git for version control
- Maven for build automation

## System Architecture

### Components
1. **ELK Stack Integration**
   - **Filebeat**
     - Monitors and ships log files
     - Configurable input paths
     - Handles log rotation and backpressure
     - Ensures reliable log shipping

   - **Logstash**
     - Receives logs from Filebeat
     - Applies filters and transformations
     - Parses log formats
     - Enriches data with additional fields
     - Routes processed logs to Elasticsearch

   - **Elasticsearch**
     - Stores and indexes log data
     - Provides full-text search capabilities
     - Handles data retention policies
     - Enables complex queries and aggregations

   - **ElasticsearchClient**
     - Java-based Elasticsearch integration
     - Retrieves logs using Elasticsearch Java API
     - Implements query builders
     - Forwards retrieved logs to Kafka topics

2. **Log Producer**
   - Generates sample log data
   - Implements Kafka producer
   - Configurable message generation rate

3. **Kafka Infrastructure**
   - Topic: logs-topic
   - Partitioning for scalability
   - Message persistence
   - Consumer group management

4. **Log Consumer**
   - Real-time message consumption
   - JSON parsing and validation
   - Object mapping to LogEntry class

5. **UI Controllers**
   - FilterController: Manages log filtering
   - LogVisController: Handles log visualization
   - LogAnalyticsController: Processes analytics

## Prerequisites

- Java Development Kit (JDK) 11 or higher
- Apache Kafka 3.x
- Elasticsearch 7.x
- Logstash 7.x
- Filebeat 7.x
- Maven 3.6+
- Minimum 8GB RAM (recommended for ELK stack)
- 10GB free disk space

## Installation

1. Clone the repository:
```bash
git clone https://github.com/faizoussama/Logs-streaming-kafka.git
cd Logs-streaming-kafka
```

2. Install dependencies:
```bash
mvn clean install
```

3. Start ELK Stack:
```bash
# Start Elasticsearch
sudo systemctl start elasticsearch

# Start Logstash
sudo systemctl start logstash

# Start Filebeat
sudo systemctl start filebeat
```

4. Start Kafka:
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka Server
bin/kafka-server-start.sh config/server.properties
```

5. Run the application:
```bash
mvn javafx:run
```

## Configuration

### Filebeat Configuration
```yaml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /path/to/your/logs/*.log
  fields:
    log_type: application

output.logstash:
  hosts: ["localhost:5044"]
```

### Logstash Configuration
```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  grok {
    match => { "message" => "%{COMBINEDAPACHELOG}" }
  }
  date {
    match => [ "timestamp", "dd/MMM/yyyy:HH:mm:ss Z" ]
    target => "@timestamp"
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "logs-%{+YYYY.MM.dd}"
  }
}
```

### Elasticsearch Configuration
```yaml
cluster.name: log-cluster
node.name: log-node-1
path.data: /var/lib/elasticsearch
path.logs: /var/log/elasticsearch
network.host: localhost
http.port: 9200
```

### Kafka Configuration
```properties
bootstrap.servers=localhost:9092
group.id=filter-consumer-group
key.deserializer=StringDeserializer
value.deserializer=StringDeserializer
auto.offset.reset=earliest
enable.auto.commit=true
```

### Application Properties
- Log retention period: 7 days
- Maximum message size: 1MB
- Consumer poll timeout: 100ms
- UI refresh rate: 1 second

## Usage

### Starting the Application
1. Launch the application using Maven or your IDE
2. The main window will open with three tabs:
   - Logs View
   - Filter
   - Analytics

### Using the Filter Interface
1. Select time range using presets or custom range
2. Choose log levels to display
3. Select source and component filters
4. Add advanced filters if needed
5. Click Apply to filter logs
6. Use Reset to clear all filters

### Log Format
```json
{
  "_id": "unique_id",
  "client_ip": "xxx.xxx.xxx.xxx",
  "timestamp": "dd/MMM/yyyy:HH:mm:ss Z",
  "http_method": "GET/POST/PUT/DELETE",
  "request": "/api/endpoint",
  "status_code": 200,
  "response_size": "1234",
  "log_level": "INFO",
  "message": "Log message content"
}
```

## Components

### 1. MainApplication.java
- Application entry point
- Scene management
- Navigation handling

### 2. FilterController.java
- Log filtering logic
- Kafka consumer management
- UI event handling

### 3. LogEntry.java
- Data model for log entries
- JSON mapping annotations
- Getter/setter methods

### 4. KafkaLogProducer.java
- Log generation
- Kafka producer implementation
- Message serialization

## Data Flow

1. **Log Collection**
   - Applications generate logs
   - Filebeat monitors and collects log files
   - Ships logs to Logstash reliably

2. **Log Processing**
   - Logstash receives logs from Filebeat
   - Applies filters and transformations
   - Enriches data with additional context
   - Forwards processed logs to Elasticsearch

3. **Storage and Indexing**
   - Elasticsearch stores and indexes logs
   - Maintains searchable indices
   - Handles data retention and lifecycle

4. **Data Retrieval**
   - ElasticsearchClient queries logs
   - Uses Elasticsearch Java API
   - Implements efficient search queries
   - Forwards retrieved logs to Kafka

5. **Data Streaming**
   - Kafka manages message queue
   - FilterController consumes messages
   - Real-time processing and filtering

6. **UI Updates**
   - JavaFX Platform.runLater for thread-safe updates
   - Observable collections for live data binding
   - Event-driven UI updates

## API Reference

### Log Entry Fields
| Field | Type | Description |
|-------|------|-------------|
| id | String | Unique identifier |
| clientIp | String | Source IP address |
| timestamp | String | Event timestamp |
| httpMethod | String | HTTP method |
| request | String | Request endpoint |
| statusCode | int | HTTP status code |
| logLevel | String | Log severity level |

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

### Coding Standards
- Follow Java naming conventions
- Add JavaDoc comments
- Write unit tests
- Keep methods focused and small

## Troubleshooting

### Common Issues

1. **Kafka Connection Failed**
   - Verify Kafka server is running
   - Check connection properties
   - Ensure correct topic exists

2. **UI Not Updating**
   - Check Platform.runLater usage
   - Verify ObservableList bindings
   - Debug event handlers

3. **Memory Issues**
   - Adjust JVM heap size
   - Check for memory leaks
   - Monitor garbage collection

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Contact

For questions and support, please contact:
- Email: faizouss123@gmail.com
- GitHub Issues: [Project Issues](https://github.com/faizoussama/Logs-streaming-kafka/issues)

## Acknowledgments

- Apache Kafka team
- JavaFX community
- All contributors to this project
