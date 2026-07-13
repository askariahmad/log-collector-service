# Log Collector Service

## Low-Level Design (LLD) & In-Depth Overview

The **Log Collector Service** is responsible for the ingestion of telemetry data from external observability platforms.

### Key Responsibilities
1. **Polling Engine**: Runs scheduled CRON jobs to query external systems (like Splunk or Datadog) for recent logs.
2. **Mock Generation**: In non-production environments, it relies on `MockSplunkController` to generate highly realistic, simulated production stack traces (e.g., Java `OutOfMemoryError`, `NullPointerException`, Spring Boot routing failures).
3. **Kafka Publishing**: Pushes all ingested logs to the `raw-logs` Kafka topic for downstream analysis.

### How to Interact
- **Port**: `8083` (Internal Docker port, mapped to 8087 on host)
- **Kafka Topic**: Produces to `raw-logs`.
- **Mock Data Endpoint**: `GET /mock/services/collector/event`
