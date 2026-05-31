package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Provider {
    private String id;
    private String fullName;
    private String shortName;
    private String commLvl;
    private String commId; // может быть null

    public Provider() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getCommLvl() { return commLvl; }
    public void setCommLvl(String commLvl) { this.commLvl = commLvl; }
    public String getCommId() { return commId; }
    public void setCommId(String commId) { this.commId = commId; }

    @Override
    public String toString() {
        return shortName != null ? shortName : fullName;
    }
}
