package com.bananatrading.engine.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        // Runs a 1-millisecond query to prevent the PostgreSQL connection pool from idling out
        Integer dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", dbCheck != null && dbCheck == 1 ? "CONNECTED" : "UNAVAILABLE",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}