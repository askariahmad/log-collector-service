package com.devops.collector.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/mock/splunk")
public class MockSplunkController {

    private final List<String> mockLogs = Arrays.asList(
            "[ERROR] 2026-07-11 10:15:22 - DatabaseConnectionException: Failed to acquire lock on table 'users'. Timeout 3000ms exceeded.",
            "[WARN] 2026-07-11 10:16:01 - Memory usage high on node-3. 85% utilization.",
            "[INFO] 2026-07-11 10:16:30 - User login successful. UserID: 40992.",
            "[ERROR] 2026-07-11 10:18:12 - NullPointerException at com.app.payment.PaymentProcessor.process(PaymentProcessor.java:54)",
            "[WARN] 2026-07-11 10:20:00 - Rate limit exceeded for API key XXXXXX. Rejecting request."
    );

    @GetMapping("/logs")
    public List<String> getMockLogs() {
        Random rand = new Random();
        // Return 1 to 3 random mock logs
        int count = rand.nextInt(3) + 1;
        return mockLogs.subList(0, count);
    }
}
