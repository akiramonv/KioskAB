package com.kiosk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.kiosk.api.ApiClient;
import com.kiosk.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiService {

    // Используем mapper из ApiClient, чтобы все ответы ApiAB разбирались одинаково.
    private static final ObjectMapper mapper = ApiClient.getMapper();

    // ==================== SERVICES ====================

    public static List<ProviderService> getAllServices() throws Exception {
        // Услуги тянутся напрямую из нового REST endpoint эмулятора.
        return parseList(ApiClient.get("/api/services"), ProviderService.class);
    }

    public static ProviderService getServiceById(String id) throws Exception {
        return parseData(ApiClient.get("/api/services/" + id), ProviderService.class);
    }

    public static List<ProviderService> getServicesByCategory(String categoryId) throws Exception {
        return parseList(ApiClient.get("/api/services/by-category/" + categoryId), ProviderService.class);
    }

    public static List<ProviderService> getServicesByProvider(String providerId) throws Exception {
        return parseList(ApiClient.get("/api/services/by-provider/" + providerId), ProviderService.class);
    }

    public static List<ProviderService> getServicesByUser(String userId) throws Exception {
        return parseList(ApiClient.get("/api/services/by-user/" + userId), ProviderService.class);
    }

    public static ProviderService createService(Map<String, Object> data) throws Exception {
        return parseData(ApiClient.post("/api/services", data), ProviderService.class);
    }

    public static ProviderService updateService(String id, Map<String, Object> data) throws Exception {
        return parseData(ApiClient.put("/api/services/" + id, data), ProviderService.class);
    }

    public static boolean deleteService(String id) throws Exception {
        return ApiClient.delete("/api/services/" + id).path("success").asBoolean(false);
    }

    // ==================== CATEGORIES ====================

    public static List<CategoryService> getAllCategories() throws Exception {
        return parseList(ApiClient.get("/api/categories"), CategoryService.class);
    }

    public static CategoryService getCategoryById(String id) throws Exception {
        return parseData(ApiClient.get("/api/categories/" + id), CategoryService.class);
    }

    public static List<CategoryService> getCategoriesByProvider(String providerId) throws Exception {
        return parseList(ApiClient.get("/api/categories/by-provider/" + providerId), CategoryService.class);
    }

    public static CategoryService createCategory(Map<String, Object> data) throws Exception {
        return parseData(ApiClient.post("/api/categories", data), CategoryService.class);
    }

    public static CategoryService updateCategory(String id, Map<String, Object> data) throws Exception {
        return parseData(ApiClient.put("/api/categories/" + id, data), CategoryService.class);
    }

    public static boolean deleteCategory(String id) throws Exception {
        return ApiClient.delete("/api/categories/" + id).path("success").asBoolean(false);
    }

    // ==================== PROVIDERS ====================

    public static List<Provider> getAllProviders() throws Exception {
        return parseList(ApiClient.get("/api/providers"), Provider.class);
    }

    public static Provider getProviderById(String id) throws Exception {
        return parseData(ApiClient.get("/api/providers/" + id), Provider.class);
    }

    public static Provider createProvider(Map<String, Object> data) throws Exception {
        return parseData(ApiClient.post("/api/providers", data), Provider.class);
    }

    public static Provider updateProvider(String id, Map<String, Object> data) throws Exception {
        return parseData(ApiClient.put("/api/providers/" + id, data), Provider.class);
    }

    public static boolean deleteProvider(String id) throws Exception {
        return ApiClient.delete("/api/providers/" + id).path("success").asBoolean(false);
    }

    // ==================== ACCOUNTS ====================

    public static List<Account> getAllAccounts() throws Exception {
        return parseList(ApiClient.get("/api/accounts"), Account.class);
    }

    public static List<Account> getAccountsByProvider(String providerId) throws Exception {
        return parseList(ApiClient.get("/api/accounts/by-provider/" + providerId), Account.class);
    }

    // ==================== COMMISSIONS ====================

    public static List<Commission> getAllCommissions() throws Exception {
        return parseList(ApiClient.get("/api/commissions"), Commission.class);
    }

    public static List<CommissionLevel> getAllCommissionLevels() throws Exception {
        return parseList(ApiClient.get("/api/commission-levels"), CommissionLevel.class);
    }

    // ==================== SPECIALIZATIONS ====================

    public static List<Specialization> getAllSpecializations() throws Exception {
        return parseList(ApiClient.get("/api/specializations"), Specialization.class);
    }

    public static Specialization getSpecializationById(String id) throws Exception {
        return parseData(ApiClient.get("/api/specializations/" + id), Specialization.class);
    }

    public static List<Specialization> getSpecializationsByProvider(String providerId) throws Exception {
        return parseList(ApiClient.get("/api/specializations/by-provider/" + providerId), Specialization.class);
    }

    public static Specialization createSpecialization(Map<String, Object> data) throws Exception {
        return parseData(ApiClient.post("/api/specializations", data), Specialization.class);
    }

    public static Specialization updateSpecialization(String id, Map<String, Object> data) throws Exception {
        return parseData(ApiClient.put("/api/specializations/" + id, data), Specialization.class);
    }

    public static boolean deleteSpecialization(String id) throws Exception {
        return ApiClient.delete("/api/specializations/" + id).path("success").asBoolean(false);
    }

    // ==================== USERS / ROLES ====================

    public static List<User> getAllUsers() throws Exception {
        return parseList(ApiClient.get("/api/users"), User.class);
    }

    public static User getUserById(String id) throws Exception {
        return parseData(ApiClient.get("/api/users/" + id), User.class);
    }

    public static List<User> getUsersByProvider(String providerId) throws Exception {
        return parseList(ApiClient.get("/api/users/by-provider/" + providerId), User.class);
    }

    public static List<User> getUsersBySpecialization(String specialId) throws Exception {
        return parseList(ApiClient.get("/api/users/by-specialization/" + specialId), User.class);
    }

    public static User createUser(Map<String, Object> data) throws Exception {
        return parseData(ApiClient.post("/api/users", data), User.class);
    }

    public static User updateUser(String id, Map<String, Object> data) throws Exception {
        return parseData(ApiClient.put("/api/users/" + id, data), User.class);
    }

    public static boolean deleteUser(String id) throws Exception {
        return ApiClient.delete("/api/users/" + id).path("success").asBoolean(false);
    }

    public static List<Role> getAllRoles() throws Exception {
        return parseList(ApiClient.get("/api/roles"), Role.class);
    }

    public static List<Role> getRolesByUser(String userId) throws Exception {
        return parseList(ApiClient.get("/api/roles/by-user/" + userId), Role.class);
    }

    // ==================== PAYMENTS ====================

    public static List<Payment> getAllPayments() throws Exception {
        return parseList(ApiClient.get("/api/payments"), Payment.class);
    }

    public static List<Payment> getPaymentsByStatus(String status) throws Exception {
        return parseList(ApiClient.get("/api/payments/by-status/" + status), Payment.class);
    }

    public static List<Payment> getPaymentsByProvider(String providerId) throws Exception {
        // Для админа организации берем только платежи его провайдера.
        return parseList(ApiClient.get("/api/payments/by-provider/" + providerId), Payment.class);
    }

    public static Payment getPaymentById(String id) throws Exception {
        return parseData(ApiClient.get("/api/payments/" + id), Payment.class);
    }

    public static Payment createPayment(BigDecimal sum, String providerId) throws Exception {
        // ApiAB принимает один платеж на провайдера: сумма уже посчитана по всей корзине.
        Map<String, Object> data = new HashMap<>();
        data.put("sum", sum);
        data.put("status", "processing");
        data.put("provId", providerId);
        return parseData(ApiClient.post("/api/payments", data), Payment.class);
    }

    public static Payment updatePaymentStatus(String id, String status) throws Exception {
        return parseData(ApiClient.patch("/api/payments/" + id + "/status", Map.of("status", status)), Payment.class);
    }

    public static boolean deletePayment(String id) throws Exception {
        return ApiClient.delete("/api/payments/" + id).path("success").asBoolean(false);
    }

    // ==================== EMULATOR EXECUTE ====================

    public static JsonNode executeOperation(String operation, Map<String, Object> params) throws Exception {
        return ApiClient.execute(operation, params != null ? params : Collections.emptyMap());
    }

    // ==================== AUTH ====================

    public static User authenticate(String email, String password) throws Exception {
        // В эмуляторе нет отдельного auth endpoint, поэтому вход проверяем по списку пользователей.
        List<User> users = getAllUsers();
        return users.stream()
                .filter(u -> email.equalsIgnoreCase(nullToEmpty(u.getEmail()))
                        && password.equals(nullToEmpty(u.getPassword())))
                .findFirst()
                .orElse(null);
    }

    // ==================== HELPERS ====================

    private static <T> List<T> parseList(JsonNode root, Class<T> clazz) {
        // Все ответы ApiAB лежат внутри поля data, поэтому сначала забираем именно его.
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return new ArrayList<>();
        }
        try {
            CollectionType listType = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return mapper.convertValue(data, listType);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static <T> T parseData(JsonNode root, Class<T> clazz) {
        // Для одиночной записи схема та же: GenericResponse.data -> нужная модель.
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            return null;
        }
        return mapper.convertValue(data, clazz);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
