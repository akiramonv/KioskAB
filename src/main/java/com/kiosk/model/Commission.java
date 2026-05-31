package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Commission {
    private String id;
    private String commissionType; // percent | fixed
    private double commissionValue;

    public Commission() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String commissionType) { this.commissionType = commissionType; }
    public double getCommissionValue() { return commissionValue; }
    public void setCommissionValue(double commissionValue) { this.commissionValue = commissionValue; }

    public String getDisplayText() {
        if ("percent".equals(commissionType)) {
            return commissionValue + "%";
        } else if ("fixed".equals(commissionType)) {
            return commissionValue + " сом";
        }
        return String.valueOf(commissionValue);
    }
}
