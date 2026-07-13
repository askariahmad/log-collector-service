package com.devops.collector.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Profile("dev")
public class MockSplunkLogGenerator {

    private static final Logger log = LoggerFactory.getLogger(MockSplunkLogGenerator.class);
    private final WebClient.Builder webClientBuilder;
    private final Random random = new Random();

    @Value("${services.analyzer-url}")
    private String analyzerUrl;

    public MockSplunkLogGenerator(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    // Runs every 45 seconds, but randomly decides whether to emit logs to simulate random intervals
    @Scheduled(fixedRate = 45000)
    public void generateMockLogs() {
        if (random.nextInt(100) > 60) {
            log.info("Mock Splunk Log Generator: Simulating random silence. Skipping this interval.");
            return;
        }

        log.info("Mock Splunk Log Generator: Generating random simulated logs...");
        
        List<String> mockLogs = new ArrayList<>();
        String[] events = {
            "Failed login attempt from IP 192.168.1.55 for user admin",
            "Outbound connection to known malicious IP 203.0.113.42",
            "Multiple 401 Unauthorized errors from IP 10.0.0.12",
            "Privilege escalation detected: user 'www-data' executed sudo command",
            "Unusual data egress: 5GB transferred to external bucket"
        };
        
        int numLogs = random.nextInt(3) + 1; // 1 to 3 logs
        for(int i = 0; i < numLogs; i++) {
            mockLogs.add("[MOCK-SPLUNK] " + System.currentTimeMillis() + " " + events[random.nextInt(events.length)]);
        }

        try {
            webClientBuilder.build()
                .post()
                .uri(analyzerUrl)
                .header("X-Tenant-Id", "devops-com-tenant")
                .bodyValue(mockLogs)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            log.info("Mock Splunk logs forwarded to Analyzer for mock-tenant.");
            
            // Also send for real-tenant so the special user can see mock splunk logs!
            webClientBuilder.build()
                .post()
                .uri(analyzerUrl)
                .header("X-Tenant-Id", "real-tenant")
                .bodyValue(mockLogs)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            log.info("Mock Splunk logs forwarded to Analyzer for real-tenant.");
        } catch (Exception e) {
            log.error("Failed to forward mock logs: {}", e.getMessage());
        }
    }
}
