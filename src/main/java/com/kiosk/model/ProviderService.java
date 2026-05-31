package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProviderService {
    private String id;
    private String name;
    private String categoryId;
    private String provId;
    private String accountId;
    private String commId; // может быть null

    public ProviderService() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getProvId() { return provId; }
    public void setProvId(String provId) { this.provId = provId; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getCommId() { return commId; }
    public void setCommId(String commId) { this.commId = commId; }
}
