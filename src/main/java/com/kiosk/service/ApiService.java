package com.kiosk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.kiosk.api.ApiClient;
import com.kiosk.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервисный слой — все запросы к API Emulator.
 * Возвращает типизированные Java-объекты.
 */
public class ApiService {

    private static final ObjectMapper mapper = ApiClient.getMapper();

    // ==================== SERVICES ====================

    public static List<ProviderService> getAllServices() throws Exception {
        JsonNode root = ApiClient.get("/api/services");
        return parseList(root, ProviderService.class);
    }

    public static ProviderService getServiceById(String id) throws Exception {
        JsonNode root = ApiClient.get("/api/services/" + id);
        return parseData(root, ProviderService.class);
    }

    public static List<ProviderService> getServicesByCategory(String categoryId) throws Exception {
        JsonNode root = ApiClient.get("/api/services/by-category/" + categoryId);
        return parseList(root, ProviderService.class);
    }

    public static List<ProviderService> getServicesByProvider(String providerId) throws Exception {
        JsonNode root = ApiClient.get("/api/services/by-provider/" + providerId);
        return parseList(root, ProviderService.class);
    }

    public static List<ProviderService> getServicesByUser(String userId) throws Exception {
        JsonNode root = ApiClient.get("/api/services/by-user/" + userId);
        return parseList(root, ProviderService.class);
    }

    public static ProviderService createService(Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.post("/api/services", data);
        return parseData(root, ProviderService.class);
    }

    public static ProviderService updateService(String id, Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.put("/api/services/" + id, data);
        return parseData(root, ProviderService.class);
    }

    public static boolean deleteService(String id) throws Exception {
        JsonNode root = ApiClient.delete("/api/services/" + id);
        return root.path("success").asBoolean(false);
    }

    // ==================== CATEGORIES ====================

    public static List<CategoryService> getAllCategories() throws Exception {
        JsonNode root = ApiClient.get("/api/categories");
        return parseList(root, CategoryService.class);
    }

    public static CategoryService getCategoryById(String id) throws Exception {
        JsonNode root = ApiClient.get("/api/categories/" + id);
        return parseData(root, CategoryService.class);
    }

    public static List<CategoryService> getCategoriesByProvider(String providerId) throws Exception {
        JsonNode root = ApiClient.get("/api/categories/by-provider/" + providerId);
        return parseList(root, CategoryService.class);
    }

    public static CategoryService createCategory(Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.post("/api/categories", data);
        return parseData(root, CategoryService.class);
    }

    public static CategoryService updateCategory(String id, Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.put("/api/categories/" + id, data);
        return parseData(root, CategoryService.class);
    }

    public static boolean deleteCategory(String id) throws Exception {
        JsonNode root = ApiClient.delete("/api/categories/" + id);
        return root.path("success").asBoolean(false);
    }

    // ==================== PROVIDERS ====================

    public static List<Provider> getAllProviders() throws Exception {
        JsonNode root = ApiClient.get("/api/providers");
        return parseList(root, Provider.class);
    }

    public static Provider getProviderById(String id) throws Exception {
        JsonNode root = ApiClient.get("/api/providers/" + id);
        return parseData(root, Provider.class);
    }

    public static Provider createProvider(Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.post("/api/providers", data);
        return parseData(root, Provider.class);
    }

    public static Provider updateProvider(String id, Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.put("/api/providers/" + id, data);
        return parseData(root, Provider.class);
    }

    public static boolean deleteProvider(String id) throws Exception {
        JsonNode root = ApiClient.delete("/api/providers/" + id);
        return root.path("success").asBoolean(false);
    }

    // ==================== SPECIALIZATIONS ====================

    public static List<Specialization> getAllSpecializations() throws Exception {
        JsonNode root = ApiClient.get("/api/specializations");
        return parseList(root, Specialization.class);
    }

    public static Specialization getSpecializationById(String id) throws Exception {
        JsonNode root = ApiClient.get("/api/specializations/" + id);
        return parseData(root, Specialization.class);
    }

    public static List<Specialization> getSpecializationsByProvider(String providerId) throws Exception {
        JsonNode root = ApiClient.get("/api/specializations/by-provider/" + providerId);
        return parseList(root, Specialization.class);
    }

    // ==================== USERS ====================

    public static List<User> getAllUsers() throws Exception {
        JsonNode root = ApiClient.get("/api/users");
        return parseList(root, User.class);
    }

    public static User getUserById(String id) throws Exception {
        JsonNode root = ApiClient.get("/api/users/" + id);
        return parseData(root, User.class);
    }

    public static List<User> getUsersByProvider(String providerId) throws Exception {
        JsonNode root = ApiClient.get("/api/users/by-provider/" + providerId);
        return parseList(root, User.class);
    }

    public static List<User> getUsersBySpecialization(String specialId) throws Exception {
        JsonNode root = ApiClient.get("/api/users/by-specialization/" + specialId);
        return parseList(root, User.class);
    }

    public static User createUser(Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.post("/api/users", data);
        return parseData(root, User.class);
    }

    public static User updateUser(String id, Map<String, Object> data) throws Exception {
        JsonNode root = ApiClient.put("/api/users/" + id, data);
        return parseData(root, User.class);
    }

    public static boolean deleteUser(String id) throws Exception {
        JsonNode root = ApiClient.delete("/api/users/" + id);
        return root.path("success").asBoolean(false);
    }

    // ==================== EMULATOR EXECUTE ====================

    public static JsonNode executeOperation(String operation, Map<String, String> params) throws Exception {
        return ApiClient.execute(operation, params != null ? params : new HashMap<>());
    }

    // ==================== AUTH (через users) ====================

    /**
     * Простая аутентификация: загружаем всех пользователей и ищем совпадение.
     * В реальной системе: POST /api/auth/login
     */
    public static User authenticate(String email, String password) throws Exception {
        List<User> users = getAllUsers();
        return users.stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()) && password.equals(u.getPassword()))
                .findFirst()
                .orElse(null);
    }

    // ==================== HELPERS ====================

    private static <T> List<T> parseList(JsonNode root, Class<T> clazz) {
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) return new ArrayList<>();
        try {
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return mapper.convertValue(data, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static <T> T parseData(JsonNode root, Class<T> clazz) {
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) return null;
        try {
            return mapper.convertValue(data, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
