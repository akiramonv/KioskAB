package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderService {
    private String id;
    private String name;
    private BigDecimal price = BigDecimal.ZERO;
    private String categoryId;
    private String provId;
    private String accountId;
    private String commId; // может быть null
    private String categoryName;
    private String providerName;
    private String accountName;
    private Commission commission;
    private List<String> specializations = new ArrayList<>();

    public ProviderService() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price != null ? price : BigDecimal.ZERO; }
    public void setPrice(BigDecimal price) { this.price = price != null ? price : BigDecimal.ZERO; }
    public boolean hasFixedPrice() { return getPrice().compareTo(BigDecimal.ZERO) > 0; }
    public String getDisplayPrice() {
        return hasFixedPrice() ? getPrice().setScale(2, RoundingMode.HALF_UP) + " сом" : null;
    }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getProvId() { return provId; }
    public void setProvId(String provId) { this.provId = provId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getCommId() { return commId; }
    public void setCommId(String commId) { this.commId = commId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public Commission getCommission() { return commission; }
    public void setCommission(Commission commission) { this.commission = commission; }
    public List<String> getSpecializations() { return specializations; }
    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations != null ? specializations : new ArrayList<>();
    }

    public String getDisplayCategory() {
        return categoryName != null && !categoryName.isBlank() ? categoryName : "—";
    }

    public String getDisplayProvider() {
        return providerName != null && !providerName.isBlank() ? providerName : "—";
    }

    public String getDisplayAccount() {
        return accountName != null && !accountName.isBlank() ? accountName : "—";
    }
}
