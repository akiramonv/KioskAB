package com.kiosk.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CartItem {
    private final ProviderService service;
    private final BigDecimal amount;

    public CartItem(ProviderService service, BigDecimal amount) {
        this.service = service;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public ProviderService getService() {
        return service;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
