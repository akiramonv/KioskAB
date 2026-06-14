package com.kiosk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CartItem — модульные тесты")
class CartItemTest {

    // ---- getTotal() ----

    @Test
    @DisplayName("getTotal(): amount * quantity = итог")
    void getTotal_normalValues() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("250.00"), 3);

        assertEquals(new BigDecimal("750.00"), item.getTotal());
    }

    @Test
    @DisplayName("getTotal(): дробное умножение, округление HALF_UP до 2 знаков")
    void getTotal_fractional_rounding() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("33.33"), 3);

        // 33.33 * 3 = 99.99
        assertEquals(new BigDecimal("99.99"), item.getTotal());
    }

    @Test
    @DisplayName("getTotal(): quantity=1 → итог равен amount")
    void getTotal_quantity1() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("500.00"), 1);

        assertEquals(new BigDecimal("500.00"), item.getTotal());
    }

    @Test
    @DisplayName("getTotal(): большое количество")
    void getTotal_largeQuantity() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("10.00"), 100);

        assertEquals(new BigDecimal("1000.00"), item.getTotal());
    }

    // ---- граничные значения quantity ----

    @Test
    @DisplayName("quantity=0: применяется минимум 1")
    void quantity_zero_clampedToOne() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("100.00"), 0);

        assertEquals(1, item.getQuantity());
    }

    @Test
    @DisplayName("quantity=-5: применяется минимум 1")
    void quantity_negative_clampedToOne() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("100.00"), -5);

        assertEquals(1, item.getQuantity());
    }

    @Test
    @DisplayName("quantity=1: остаётся 1 (граница)")
    void quantity_one_stays() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("100.00"), 1);

        assertEquals(1, item.getQuantity());
    }

    // ---- масштабирование amount ----

    @Test
    @DisplayName("amount масштабируется до 2 знаков (100.999 → 101.00)")
    void amount_scaled_halfUp() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("100.999"), 1);

        assertEquals(new BigDecimal("101.00"), item.getAmount());
    }

    @Test
    @DisplayName("amount масштабируется до 2 знаков (100.994 → 100.99)")
    void amount_scaled_halfDown() {
        CartItem item = new CartItem(new ProviderService(), new BigDecimal("100.994"), 1);

        assertEquals(new BigDecimal("100.99"), item.getAmount());
    }

    // ---- getService() ----

    @Test
    @DisplayName("getService(): возвращает переданную услугу")
    void getService_returnsCorrectService() {
        ProviderService svc = new ProviderService();
        svc.setId("svc-001");
        svc.setName("Интернет");
        CartItem item = new CartItem(svc, new BigDecimal("200.00"), 2);

        assertSame(svc, item.getService());
        assertEquals("Интернет", item.getService().getName());
    }
}
