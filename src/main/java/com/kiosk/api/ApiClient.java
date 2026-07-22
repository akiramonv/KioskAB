package com.kiosk.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiosk.util.AppLogger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * Низкоуровневый HTTP-клиент для взаимодействия с API Emulator.
 * <p>
 * Если эмулятор недоступен (нет соединения), клиент один раз переключается
 * в демо-режим ({@link LocalApiStub}) и дальше работает с локальными данными,
 * которые сохраняются между запусками. Форма ответов при этом не меняется.
 */
public class ApiClient {

    private static final String BASE_URL = "http://localhost:7924";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    /** true, когда работаем на локальных демо-данных вместо ApiAB. */
    public static boolean isDemoMode() {
        return LocalApiStub.isActive();
    }

    // ---- REST endpoints ----

    public static JsonNode get(String path) throws Exception {
        return request("GET", path, null);
    }

    public static JsonNode post(String path, Object body) throws Exception {
        return request("POST", path, body);
    }

    public static JsonNode put(String path, Object body) throws Exception {
        return request("PUT", path, body);
    }

    public static JsonNode patch(String path, Object body) throws Exception {
        return request("PATCH", path, body);
    }

    public static JsonNode delete(String path) throws Exception {
        return request("DELETE", path, null);
    }

    private static JsonNode request(String method, String path, Object body) throws Exception {
        if (LocalApiStub.isActive()) {
            return LocalApiStub.handle(method, path, body);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json");
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
        HttpRequest request = builder.method(method, publisher).build();
        try {
            return send(request);
        } catch (IOException | InterruptedException e) {
            // Сервер не ответил вовсе (нет соединения) — включаем демо-режим.
            // HTTP-ошибки (сервер жив, но вернул 4xx/5xx) сюда не попадают.
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            AppLogger.warn("ApiAB недоступен (" + method + " " + path + ") — переключаюсь в демо-режим", (Exception) e);
            LocalApiStub.activate();
            return LocalApiStub.handle(method, path, body);
        }
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
