package com.kiosk.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartItem {
    private final ProviderService service;
    // Сумма хранится за одну единицу услуги, потому что количество выбирается отдельно.
    private final BigDecimal amount;
    // Количество нужно для корзины: итог позиции считается как amount * quantity.
    private final int quantity;

    public CartItem(ProviderService service, BigDecimal amount, int quantity) {
        this.service = service;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.quantity = Math.max(1, quantity);
    }

    public ProviderService getService() {
        return service;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getTotal() {
        // Итог позиции считаем здесь, чтобы контроллеры не дублировали формулу.
        return amount.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
