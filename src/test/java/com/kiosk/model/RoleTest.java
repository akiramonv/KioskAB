package com.kiosk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Role — модульные тесты")
class RoleTest {

    @Test
    @DisplayName("isAdmin(): роль \"admin\" → true")
    void isAdmin_admin_returnsTrue() {
        Role r = new Role();
        r.setName("admin");

        assertTrue(r.isAdmin());
    }

    @Test
    @DisplayName("isAdmin(): роль \"superAdmin\" → true")
    void isAdmin_superAdmin_returnsTrue() {
        Role r = new Role();
        r.setName("superAdmin");

        assertTrue(r.isAdmin());
    }

    @Test
    @DisplayName("isAdmin(): роль \"user\" → false")
    void isAdmin_user_returnsFalse() {
        Role r = new Role();
        r.setName("user");

        assertFalse(r.isAdmin());
    }

    @Test
    @DisplayName("isAdmin(): name=null → false")
    void isAdmin_null_returnsFalse() {
        Role r = new Role();

        assertFalse(r.isAdmin());
    }

    @Test
    @DisplayName("isSuperAdmin(): роль \"superAdmin\" → true")
    void isSuperAdmin_superAdmin_returnsTrue() {
        Role r = new Role();
        r.setName("superAdmin");

        assertTrue(r.isSuperAdmin());
    }

    @Test
    @DisplayName("isSuperAdmin(): роль \"admin\" → false")
    void isSuperAdmin_admin_returnsFalse() {
        Role r = new Role();
        r.setName("admin");

        assertFalse(r.isSuperAdmin());
    }

    @Test
    @DisplayName("isSuperAdmin(): роль \"user\" → false")
    void isSuperAdmin_user_returnsFalse() {
        Role r = new Role();
        r.setName("user");

        assertFalse(r.isSuperAdmin());
    }

    @Test
    @DisplayName("toString(): имя задано → возвращает имя")
    void toString_withName() {
        Role r = new Role();
        r.setName("admin");

        assertEquals("admin", r.toString());
    }

    @Test
    @DisplayName("toString(): name=null → \"—\"")
    void toString_null_returnsDash() {
        Role r = new Role();

        assertEquals("—", r.toString());
    }
}
