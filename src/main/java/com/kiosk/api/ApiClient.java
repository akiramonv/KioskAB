package com.kiosk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Низкоуровневый HTTP-клиент для взаимодействия с API Emulator.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:7924";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    // ---- REST endpoints ----

    public static JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        return send(request);
    }

    public static JsonNode post(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public static JsonNode put(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public static JsonNode patch(String path, Object body) throws Exception {
        String json = mapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request);
    }

    public static JsonNode delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
        return send(request);
    }

    /**
     * Универсальный endpoint /api/emulator/execute
     * @param operation — имя операции (ALL_SERVICES, SERVICE_BY_ID и т.д.)
     * @param params    — параметры с ключами в нижнем регистре
     */
    public static JsonNode execute(String operation, Map<String, Object> params) throws Exception {
        Map<String, Object> body = Map.of(
                "operation", operation,
                "params", params != null ? params : Collections.emptyMap()
        );
        return post("/api/emulator/execute", body);
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    private static JsonNode send(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = response.body() == null || response.body().isBlank()
                ? mapper.createObjectNode()
                : mapper.readTree(response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = root.path("message").asText("HTTP " + response.statusCode());
            throw new IllegalStateException(message);
        }
        if (root.has("success") && !root.path("success").asBoolean(true)) {
            throw new IllegalStateException(root.path("message").asText("API вернул ошибку"));
        }
        return root;
    }
}
