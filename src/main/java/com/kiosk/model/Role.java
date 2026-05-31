package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {
    private String id;
    private String name; // user, admin, superAdmin

    public Role() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isAdmin() {
        return "admin".equals(name) || "superAdmin".equals(name);
    }
    public boolean isSuperAdmin() {
        return "superAdmin".equals(name);
    }

    @Override
    public String toString() {
        return name != null ? name : "—";
    }
}
