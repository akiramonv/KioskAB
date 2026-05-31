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

    public List<Role> getCurrentUserRoles() { return currentUserRoles; }
    public void setCurrentUserRoles(List<Role> roles) {
        this.currentUserRoles = roles != null ? roles : new ArrayList<>();
    }

    public List<Specialization> getCurrentUserSpecializations() { return currentUserSpecializations; }
    public void setCurrentUserSpecializations(List<Specialization> specs) {
        this.currentUserSpecializations = specs != null ? specs : new ArrayList<>();
    }

    public boolean isAdmin() {
        return currentUserRoles.stream().anyMatch(Role::isAdmin);
    }

    public boolean isSuperAdmin() {
        return currentUserRoles.stream().anyMatch(Role::isSuperAdmin);
    }

    public boolean hasSpecializations() {
        return !currentUserSpecializations.isEmpty();
    }

    public void logout() {
        currentUser = null;
        currentUserRoles.clear();
        currentUserSpecializations.clear();
    }
}
