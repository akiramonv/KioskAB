package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Provider {
    private String id;
    private String fullName;
    private String shortName;
    private String commLvl;
    private String commId; // может быть null
    private String commissionLevel;
    private Commission commission;

    public Provider() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getCommLvl() { return commLvl != null ? commLvl : commissionLevel; }
    public void setCommLvl(String commLvl) { this.commLvl = commLvl; }
    public String getCommId() { return commId; }
    public void setCommId(String commId) { this.commId = commId; }
    public String getCommissionLevel() { return commissionLevel; }
    public void setCommissionLevel(String commissionLevel) { this.commissionLevel = commissionLevel; }
    public Commission getCommission() { return commission; }
    public void setCommission(Commission commission) { this.commission = commission; }

    public boolean matchesName(String value) {
        if (value == null) {
            return false;
        }
        return value.equalsIgnoreCase(String.valueOf(shortName))
                || value.equalsIgnoreCase(String.valueOf(fullName))
                || value.equalsIgnoreCase(toString());
    }

    @Override
    public String toString() {
        return shortName != null ? shortName : fullName;
    }
}
