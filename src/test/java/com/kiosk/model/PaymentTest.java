package com.kiosk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment — модульные тесты")
class PaymentTest {

    // ---- getTotal() ----

    @Test
    @DisplayName("getTotal(): сумма + комиссия = итог")
    void getTotal_normalValues() {
        Payment p = new Payment();
        p.setSum(new BigDecimal("1000"));
        p.setFee(new BigDecimal("50"));

        assertEquals(new BigDecimal("1050"), p.getTotal());
    }

    @Test
    @DisplayName("getTotal(): нулевые сумма и комиссия")
    void getTotal_zeroValues() {
        Payment p = new Payment();
        // setSum/setFee не вызываются — должны использоваться ZERO по умолчанию

        assertEquals(BigDecimal.ZERO, p.getTotal());
    }

    @Test
    @DisplayName("getTotal(): крупные значения — 99999.99 + 999.01")
    void getTotal_largeValues() {
        Payment p = new Payment();
        p.setSum(new BigDecimal("99999.99"));
        p.setFee(new BigDecimal("999.01"));

        assertEquals(new BigDecimal("100999.00"), p.getTotal());
    }

    @Test
    @DisplayName("getTotal(): только комиссия, сумма по умолчанию 0")
    void getTotal_onlyFeeSet() {
        Payment p = new Payment();
        p.setFee(new BigDecimal("25.50"));

        assertEquals(new BigDecimal("25.50"), p.getTotal());
    }

    // ---- null-защита в сеттерах ----

    @Test
    @DisplayName("setSum(null): getSum() должен вернуть ZERO")
    void setSum_null_returnsZero() {
        Payment p = new Payment();
        p.setSum(null);

        assertEquals(BigDecimal.ZERO, p.getSum());
    }

    @Test
    @DisplayName("setFee(null): getFee() должен вернуть ZERO")
    void setFee_null_returnsZero() {
        Payment p = new Payment();
        p.setFee(null);

        assertEquals(BigDecimal.ZERO, p.getFee());
    }

    @Test
    @DisplayName("getTotal() когда sum=null и fee=null: оба ZERO, итог ZERO")
    void getTotal_bothNull() {
        Payment p = new Payment();
        p.setSum(null);
        p.setFee(null);

        assertEquals(BigDecimal.ZERO, p.getTotal());
    }

    // ---- прочие поля ----

    @Test
    @DisplayName("setStatus / getStatus: сохраняет строку")
    void statusField_setAndGet() {
        Payment p = new Payment();
        p.setStatus("processing");

        assertEquals("processing", p.getStatus());
    }

    @Test
    @DisplayName("setInn / getInn: сохраняет значение")
    void innField_setAndGet() {
        Payment p = new Payment();
        p.setInn("12345678901234");

        assertEquals("12345678901234", p.getInn());
    }
}
