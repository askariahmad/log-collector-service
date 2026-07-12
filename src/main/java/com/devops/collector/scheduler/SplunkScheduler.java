package com.devops.collector.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SplunkScheduler {

    private static final Logger log = LoggerFactory.getLogger(SplunkScheduler.class);
    private final WebClient.Builder webClientBuilder;

    public SplunkScheduler(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Value("${services.config-url}")
    private String configUrl;

    @Value("${services.analyzer-url}")
    private String analyzerUrl;

    // Inner class to map config response
    public static class ConfigResponse {
        private String tenantId;
        private String splunkUrl;
        private Integer scanIntervalValue;
        private String scanIntervalUnit;
        private java.util.Date lastScanTime;
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getSplunkUrl() { return splunkUrl; }
        public void setSplunkUrl(String splunkUrl) { this.splunkUrl = splunkUrl; }
        public Integer getScanIntervalValue() { return scanIntervalValue; }
        public void setScanIntervalValue(Integer scanIntervalValue) { this.scanIntervalValue = scanIntervalValue; }
        public String getScanIntervalUnit() { return scanIntervalUnit; }
        public void setScanIntervalUnit(String scanIntervalUnit) { this.scanIntervalUnit = scanIntervalUnit; }
        public java.util.Date getLastScanTime() { return lastScanTime; }
        public void setLastScanTime(java.util.Date lastScanTime) { this.lastScanTime = lastScanTime; }
    }

    @Scheduled(fixedRate = 60000) // Run every 60 seconds (1 minute)
    public void fetchLogs() {
        log.info("Starting multi-tenant log collection cycle...");

        try {
            // Fetch all tenant configurations
            List<ConfigResponse> configs = webClientBuilder.build()
                    .get()
                    .uri(configUrl + "/internal/all")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ConfigResponse>>() {})
                    .block();

            if (configs == null || configs.isEmpty()) {
                log.info("No active tenants found. Skipping log collection.");
                return;
            }

            for (ConfigResponse config : configs) {
                if (config.getTenantId() == null || config.getSplunkUrl() == null || config.getSplunkUrl().isEmpty()) {
                    continue;
                }
                
                // Evaluate dynamic scanning interval
                int value = config.getScanIntervalValue() != null && config.getScanIntervalValue() > 0 
                               ? config.getScanIntervalValue() 
                               : 5; // default 5
                               
                String unit = config.getScanIntervalUnit() != null ? config.getScanIntervalUnit().toUpperCase() : "MINUTES";
                long multiplier = switch (unit) {
                    case "MS" -> 1L;
                    case "SECONDS" -> 1000L;
                    case "MINUTES" -> 60 * 1000L;
                    case "HOURS" -> 60 * 60 * 1000L;
                    case "DAYS" -> 24 * 60 * 60 * 1000L;
                    case "WEEKS" -> 7 * 24 * 60 * 60 * 1000L;
                    default -> 60 * 1000L;
                };
                               
                if (config.getLastScanTime() != null) {
                    long elapsedMs = System.currentTimeMillis() - config.getLastScanTime().getTime();
                    long intervalMs = value * multiplier;
                    if (elapsedMs < intervalMs) {
                        log.debug("Skipping tenant {}. Interval not reached.", config.getTenantId());
                        continue;
                    }
                }
                
                log.info("Fetching logs for tenant: {}", config.getTenantId());
                List<String> logs = webClientBuilder.build()
                        .get()
                        .uri(config.getSplunkUrl())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                        .block();

                if (logs != null && !logs.isEmpty()) {
                    log.info("Fetched {} logs for tenant {}. Forwarding to Analyzer...", logs.size(), config.getTenantId());
                    
                    webClientBuilder.build()
                            .post()
                            .uri(analyzerUrl)
                            .header("X-Tenant-Id", config.getTenantId())
                            .bodyValue(logs)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();
                            
                    log.info("Logs forwarded successfully for tenant {}.", config.getTenantId());
                    
                    // Fire-and-forget: we don't have a direct internal endpoint to update just lastScanTime easily 
                    // without wiping the rest of the config, so in a real system we'd hit a dedicated PATCH endpoint.
                    // For now, assume config-service or db handles tracking if needed.
                }
            }
        } catch (Exception e) {
            log.error("Error during log fetch and forward: {}", e.getMessage());
        }
    }
}
