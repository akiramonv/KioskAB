package com.kiosk.util;

import com.kiosk.model.Role;
import com.kiosk.model.Specialization;
import com.kiosk.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton — хранит состояние текущей сессии пользователя.
 */
public class SessionManager {

    private static SessionManager instance;

    private User currentUser;
    // ID организации сохраняем в сессии, потому что админка и общий платеж фильтруются по нему.
    private String currentProviderId;
    // Название организации нужно только для понятного текста в статусе и интерфейсе.
    private String currentProviderName;
    private List<Role> currentUserRoles = new ArrayList<>();
    private List<Specialization> currentUserSpecializations = new ArrayList<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }
    public String getCurrentProviderId() { return currentProviderId; }
    public void setCurrentProviderId(String currentProviderId) { this.currentProviderId = currentProviderId; }
    public String getCurrentProviderName() { return currentProviderName; }
    public void setCurrentProviderName(String currentProviderName) { this.currentProviderName = currentProviderName; }

    public boolean hasProvider() {
        // Если провайдер не определился при входе, обычный админ не должен видеть чужие данные.
        return currentProviderId != null && !currentProviderId.isBlank();
    }

    public List<Role> getCurrentUserRoles() { return currentUserRoles; }
    public void setCurrentUserRoles(List<Role> roles) {
        this.currentUserRoles = roles != null ? roles : new ArrayList<>();
    }

    public List<Specialization> getCurrentUserSpecializations() { return currentUserSpecializations; }
    public void setCurrentUserSpecializations(List<Specialization> specs) {
        this.currentUserSpecializations = specs != null ? specs : new ArrayList<>();
    }

    public boolean isAdmin() {
        // Роли приходят из ApiAB вместе с пользователем, здесь просто проверяем текущую сессию.
        return currentUserRoles.stream().anyMatch(Role::isAdmin);
    }

    public boolean isSuperAdmin() {
        return currentUserRoles.stream().anyMatch(Role::isSuperAdmin);
    }

    public boolean hasSpecializations() {
        return !currentUserSpecializations.isEmpty();
    }

    public void logout() {
        // При выходе чистим все данные сессии, чтобы следующий пользователь не видел чужой контекст.
        currentUser = null;
        currentProviderId = null;
        currentProviderName = null;
        currentUserRoles.clear();
        currentUserSpecializations.clear();
    }
}
