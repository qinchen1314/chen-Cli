package com.paicli.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GLMClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GLMClient() {
        this(loadEnv(Path.of(".env")));
    }

    public GLMClient(Map<String, String> config) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = firstNonBlank(System.getenv("GLM_API_KEY"), config.get("GLM_API_KEY"));
        this.baseUrl = firstNonBlank(System.getenv("GLM_BASE_URL"), config.get("GLM_BASE_URL"),
                "https://open.bigmodel.cn/api/paas/v4/chat/completions");
        this.model = firstNonBlank(System.getenv("GLM_MODEL"), config.get("GLM_MODEL"), "glm-5.1");
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GLM_API_KEY is required. Set it in environment variables or .env.");
        }
        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("GLM API error " + response.statusCode() + ": " + response.body());
            }
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("choices").path(0).path("message").path("content").asText();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call GLM API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("GLM API call interrupted", e);
        }
    }

    private static Map<String, String> loadEnv(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(path)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                values.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read .env", e);
        }
        return values;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
