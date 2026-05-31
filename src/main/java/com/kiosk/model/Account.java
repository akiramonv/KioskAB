package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Account {
    private String id;
    private String name;
    private String ownerName;
    private String provId;
    private String providerName;

    public Account() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getProvId() { return provId; }
    public void setProvId(String provId) { this.provId = provId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    @Override
    public String toString() {
        return name != null ? name : "—";
    }
}
