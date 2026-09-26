package com.santosh.roommate_marketplace_api.health.controller;

import com.santosh.roommate_marketplace_api.health.dto.HealthResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Public API health check")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                "Roommate Marketplace API is running",
                LocalDateTime.now()
        );
    }
}
