package com.kiosk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryService {
    private String id;
    private String name;
    private String prntCategory; // может быть null
    private String provId;
    private String parentCategoryName;
    private String providerName;

    public CategoryService() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPrntCategory() { return prntCategory; }
    public void setPrntCategory(String prntCategory) { this.prntCategory = prntCategory; }
    public String getProvId() { return provId; }
    public void setProvId(String provId) { this.provId = provId; }
    public String getParentCategoryName() { return parentCategoryName; }
    public void setParentCategoryName(String parentCategoryName) { this.parentCategoryName = parentCategoryName; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public boolean isRootCategory() {
        return (prntCategory == null || prntCategory.isEmpty())
                && (parentCategoryName == null || parentCategoryName.isBlank());
    }

    @Override
    public String toString() {
        return name != null ? name : "—";
    }
}
