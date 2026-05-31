package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {
    private String id;
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal fee = BigDecimal.ZERO;
    private String status;
    private String providerName;
    private String qrLink;
    private String qrCode;
    private String createdAt;
    private String updatedAt;

    public Payment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getSum() { return sum; }
    public void setSum(BigDecimal sum) { this.sum = sum != null ? sum : BigDecimal.ZERO; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee != null ? fee : BigDecimal.ZERO; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getQrLink() { return qrLink; }
    public void setQrLink(String qrLink) { this.qrLink = qrLink; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getTotal() {
        return getSum().add(getFee());
    }
}
