package com.kiosk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProviderService — модульные тесты")
class ProviderServiceTest {

    // ---- hasFixedPrice() ----

    @Test
    @DisplayName("hasFixedPrice(): price > 0 → true")
    void hasFixedPrice_positive_returnsTrue() {
        ProviderService svc = new ProviderService();
        svc.setPrice(new BigDecimal("500"));

        assertTrue(svc.hasFixedPrice());
    }

    @Test
    @DisplayName("hasFixedPrice(): price = 0 → false")
    void hasFixedPrice_zero_returnsFalse() {
        ProviderService svc = new ProviderService();
        svc.setPrice(BigDecimal.ZERO);

        assertFalse(svc.hasFixedPrice());
    }

    @Test
    @DisplayName("hasFixedPrice(): price не установлена → false (по умолчанию ZERO)")
    void hasFixedPrice_notSet_returnsFalse() {
        ProviderService svc = new ProviderService();

        assertFalse(svc.hasFixedPrice());
    }

    @Test
    @DisplayName("hasFixedPrice(): очень маленькая цена 0.01 → true")
    void hasFixedPrice_smallPositive_returnsTrue() {
        ProviderService svc = new ProviderService();
        svc.setPrice(new BigDecimal("0.01"));

        assertTrue(svc.hasFixedPrice());
    }

    // ---- getDisplayPrice() ----

    @Test
    @DisplayName("getDisplayPrice(): price=350 → \"350.00 сом\"")
    void getDisplayPrice_withPrice() {
        ProviderService svc = new ProviderService();
        svc.setPrice(new BigDecimal("350"));

        assertEquals("350.00 сом", svc.getDisplayPrice());
    }

    @Test
    @DisplayName("getDisplayPrice(): price=0 → null")
    void getDisplayPrice_zeroPrice_returnsNull() {
        ProviderService svc = new ProviderService();
        svc.setPrice(BigDecimal.ZERO);

        assertNull(svc.getDisplayPrice());
    }

    @Test
    @DisplayName("getDisplayPrice(): price не установлена → null")
    void getDisplayPrice_notSet_returnsNull() {
        ProviderService svc = new ProviderService();

        assertNull(svc.getDisplayPrice());
    }

    // ---- null-защита ----

    @Test
    @DisplayName("setPrice(null): getPrice() должен вернуть ZERO")
    void setPrice_null_returnsZero() {
        ProviderService svc = new ProviderService();
        svc.setPrice(null);

        assertEquals(BigDecimal.ZERO, svc.getPrice());
    }

    // ---- display helpers ----

    @Test
    @DisplayName("getDisplayCategory(): categoryName задан → возвращает его")
    void getDisplayCategory_withName() {
        ProviderService svc = new ProviderService();
        svc.setCategoryName("Интернет");

        assertEquals("Интернет", svc.getDisplayCategory());
    }

    @Test
    @DisplayName("getDisplayCategory(): categoryName=null → \"—\"")
    void getDisplayCategory_null_returnsDash() {
        ProviderService svc = new ProviderService();

        assertEquals("—", svc.getDisplayCategory());
    }

    @Test
    @DisplayName("getDisplayCategory(): categoryName пустая строка → \"—\"")
    void getDisplayCategory_blank_returnsDash() {
        ProviderService svc = new ProviderService();
        svc.setCategoryName("   ");

        assertEquals("—", svc.getDisplayCategory());
    }

    @Test
    @DisplayName("getDisplayProvider(): providerName задан → возвращает его")
    void getDisplayProvider_withName() {
        ProviderService svc = new ProviderService();
        svc.setProviderName("Alfa Telecom");

        assertEquals("Alfa Telecom", svc.getDisplayProvider());
    }

    @Test
    @DisplayName("getDisplayProvider(): providerName=null → \"—\"")
    void getDisplayProvider_null_returnsDash() {
        ProviderService svc = new ProviderService();

        assertEquals("—", svc.getDisplayProvider());
    }

    @Test
    @DisplayName("getDisplayAccount(): accountName задан → возвращает его")
    void getDisplayAccount_withName() {
        ProviderService svc = new ProviderService();
        svc.setAccountName("Счёт 001");

        assertEquals("Счёт 001", svc.getDisplayAccount());
    }

    @Test
    @DisplayName("getDisplayAccount(): accountName=null → \"—\"")
    void getDisplayAccount_null_returnsDash() {
        ProviderService svc = new ProviderService();

        assertEquals("—", svc.getDisplayAccount());
    }
}
