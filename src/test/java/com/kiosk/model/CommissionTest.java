package com.kiosk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Commission — модульные тесты")
class CommissionTest {

    @Test
    @DisplayName("getDisplayText(): percent → \"5.0%\"")
    void getDisplayText_percent() {
        Commission c = new Commission();
        c.setCommissionType("percent");
        c.setCommissionValue(5.0);

        assertEquals("5.0%", c.getDisplayText());
    }

    @Test
    @DisplayName("getDisplayText(): fixed → \"100.0 сом\"")
    void getDisplayText_fixed() {
        Commission c = new Commission();
        c.setCommissionType("fixed");
        c.setCommissionValue(100.0);

        assertEquals("100.0 сом", c.getDisplayText());
    }

    @Test
    @DisplayName("getDisplayText(): неизвестный тип → возвращает значение как строку")
    void getDisplayText_unknown() {
        Commission c = new Commission();
        c.setCommissionType("unknown");
        c.setCommissionValue(7.5);

        assertEquals("7.5", c.getDisplayText());
    }

    @Test
    @DisplayName("getDisplayText(): тип не установлен (null) → возвращает значение как строку")
    void getDisplayText_nullType() {
        Commission c = new Commission();
        c.setCommissionValue(3.0);

        assertEquals("3.0", c.getDisplayText());
    }

    @Test
    @DisplayName("toString() совпадает с getDisplayText()")
    void toString_matchesDisplayText() {
        Commission c = new Commission();
        c.setCommissionType("percent");
        c.setCommissionValue(10.0);

        assertEquals(c.getDisplayText(), c.toString());
    }
}
