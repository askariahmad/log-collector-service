package com.devops.collector.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/mock-splunk")
@Profile("dev")
public class MockSplunkController {

    private static final Logger log = LoggerFactory.getLogger(MockSplunkController.class);
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private List<String> allLogs = new ArrayList<>();

    public MockSplunkController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            InputStream is = new ClassPathResource("mock-data/splunk-logs.json").getInputStream();
            allLogs = objectMapper.readValue(is, new TypeReference<List<String>>() {});
            log.info("Loaded {} mock Splunk logs from JSON.", allLogs.size());
        } catch (Exception e) {
            log.error("Failed to load mock Splunk logs from JSON", e);
        }
    }

    @GetMapping("/export")
    public List<String> fetchMockLogs() {
        log.info("Mock Splunk API hit: /export");
        List<String> mockLogs = new ArrayList<>();
        
        // 40% chance of returning empty to simulate silence
        if (random.nextInt(100) > 60) {
            return mockLogs; 
        }

        int numLogs = random.nextInt(5) + 1; // 1 to 5 logs
        for(int i = 0; i < numLogs; i++) {
            String logMsg = allLogs.get(random.nextInt(allLogs.size()));
            mockLogs.add("[MOCK-SPLUNK] " + System.currentTimeMillis() + " " + logMsg);
        }
        
        return mockLogs;
    }
}
