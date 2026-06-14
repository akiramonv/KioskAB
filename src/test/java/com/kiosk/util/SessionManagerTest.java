package com.kiosk.util;

import com.kiosk.model.Role;
import com.kiosk.model.Specialization;
import com.kiosk.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SessionManager — модульные тесты")
class SessionManagerTest {

    private SessionManager session;

    @BeforeEach
    void setUp() {
        // Сбрасываем состояние синглтона перед каждым тестом.
        session = SessionManager.getInstance();
        session.logout();
    }

    // ---- isLoggedIn() ----

    @Test
    @DisplayName("isLoggedIn(): до входа → false")
    void isLoggedIn_beforeLogin_returnsFalse() {
        assertFalse(session.isLoggedIn());
    }

    @Test
    @DisplayName("isLoggedIn(): после setCurrentUser → true")
    void isLoggedIn_afterSetUser_returnsTrue() {
        session.setCurrentUser(makeUser("u-1", "Иван"));

        assertTrue(session.isLoggedIn());
    }

    @Test
    @DisplayName("isLoggedIn(): после logout() → false")
    void isLoggedIn_afterLogout_returnsFalse() {
        session.setCurrentUser(makeUser("u-1", "Иван"));
        session.logout();

        assertFalse(session.isLoggedIn());
    }

    // ---- logout() полная очистка ----

    @Test
    @DisplayName("logout(): roles и specializations очищаются")
    void logout_clearsRolesAndSpecs() {
        session.setCurrentUser(makeUser("u-1", "Иван"));
        session.setCurrentUserRoles(List.of(makeRole("admin")));
        session.setCurrentUserSpecializations(List.of(makeSpec("IT")));
        session.setCurrentProviderId("prov-001");
        session.setCurrentProviderName("АО Альфа");

        session.logout();

        assertFalse(session.isLoggedIn());
        assertTrue(session.getCurrentUserRoles().isEmpty());
        assertTrue(session.getCurrentUserSpecializations().isEmpty());
        assertNull(session.getCurrentProviderId());
        assertNull(session.getCurrentProviderName());
    }

    // ---- hasProvider() ----

    @Test
    @DisplayName("hasProvider(): providerId задан → true")
    void hasProvider_withId_returnsTrue() {
        session.setCurrentProviderId("prov-001");

        assertTrue(session.hasProvider());
    }

    @Test
    @DisplayName("hasProvider(): providerId=null → false")
    void hasProvider_null_returnsFalse() {
        // После logout() providerId = null
        assertFalse(session.hasProvider());
    }

    @Test
    @DisplayName("hasProvider(): providerId пустая строка → false")
    void hasProvider_blank_returnsFalse() {
        session.setCurrentProviderId("   ");

        assertFalse(session.hasProvider());
    }

    // ---- isAdmin() ----

    @Test
    @DisplayName("isAdmin(): роль \"admin\" → true")
    void isAdmin_withAdminRole_returnsTrue() {
        session.setCurrentUserRoles(List.of(makeRole("admin")));

        assertTrue(session.isAdmin());
    }

    @Test
    @DisplayName("isAdmin(): роль \"superAdmin\" → true")
    void isAdmin_withSuperAdminRole_returnsTrue() {
        session.setCurrentUserRoles(List.of(makeRole("superAdmin")));

        assertTrue(session.isAdmin());
    }

    @Test
    @DisplayName("isAdmin(): роль \"user\" → false")
    void isAdmin_withUserRole_returnsFalse() {
        session.setCurrentUserRoles(List.of(makeRole("user")));

        assertFalse(session.isAdmin());
    }

    @Test
    @DisplayName("isAdmin(): без ролей → false")
    void isAdmin_noRoles_returnsFalse() {
        assertFalse(session.isAdmin());
    }

    // ---- isSuperAdmin() ----

    @Test
    @DisplayName("isSuperAdmin(): роль \"superAdmin\" → true")
    void isSuperAdmin_withSuperAdmin_returnsTrue() {
        session.setCurrentUserRoles(List.of(makeRole("superAdmin")));

        assertTrue(session.isSuperAdmin());
    }

    @Test
    @DisplayName("isSuperAdmin(): роль \"admin\" → false")
    void isSuperAdmin_withAdmin_returnsFalse() {
        session.setCurrentUserRoles(List.of(makeRole("admin")));

        assertFalse(session.isSuperAdmin());
    }

    // ---- hasSpecializations() ----

    @Test
    @DisplayName("hasSpecializations(): список не пуст → true")
    void hasSpecializations_nonEmpty_returnsTrue() {
        session.setCurrentUserSpecializations(List.of(makeSpec("IT")));

        assertTrue(session.hasSpecializations());
    }

    @Test
    @DisplayName("hasSpecializations(): список пуст → false")
    void hasSpecializations_empty_returnsFalse() {
        assertFalse(session.hasSpecializations());
    }

    // ---- null-защита сеттеров ----

    @Test
    @DisplayName("setCurrentUserRoles(null): roles остаётся пустым списком")
    void setRoles_null_remainsEmpty() {
        session.setCurrentUserRoles(null);

        assertNotNull(session.getCurrentUserRoles());
        assertTrue(session.getCurrentUserRoles().isEmpty());
    }

    @Test
    @DisplayName("setCurrentUserSpecializations(null): specs остаётся пустым списком")
    void setSpecs_null_remainsEmpty() {
        session.setCurrentUserSpecializations(null);

        assertNotNull(session.getCurrentUserSpecializations());
        assertTrue(session.getCurrentUserSpecializations().isEmpty());
    }

    // ---- singleton ----

    @Test
    @DisplayName("getInstance(): всегда возвращает один и тот же объект")
    void getInstance_returnsSameObject() {
        SessionManager a = SessionManager.getInstance();
        SessionManager b = SessionManager.getInstance();

        assertSame(a, b);
    }

    // ---- вспомогательные методы ----

    private static User makeUser(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(name.toLowerCase() + "@test.com");
        return u;
    }

    private static Role makeRole(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }

    private static Specialization makeSpec(String name) {
        Specialization s = new Specialization();
        s.setName(name);
        return s;
    }
}
