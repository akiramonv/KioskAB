package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String id;
    private String name;
    private String email;
    private String password;
    private String provId;
    private String providerName;
    private List<String> roles = new ArrayList<>();
    private List<String> specializations = new ArrayList<>();

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getProvId() { return provId; }
    public void setProvId(String provId) { this.provId = provId; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles != null ? roles : new ArrayList<>(); }
    public List<String> getSpecializations() { return specializations; }
    public void setSpecializations(List<String> specializations) {
        this.specializations = specializations != null ? specializations : new ArrayList<>();
    }
}
