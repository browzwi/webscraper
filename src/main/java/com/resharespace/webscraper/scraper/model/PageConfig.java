package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

public class PageConfig {

    private String contentRoot;
    private String hrefSelector;
    private List<FieldConfig> fields = new ArrayList<>();

    public String getContentRoot() {
        return contentRoot;
    }

    public void setContentRoot(String contentRoot) {
        this.contentRoot = contentRoot;
    }

    public String getHrefSelector() {
        return hrefSelector;
    }

    public void setHrefSelector(String hrefSelector) {
        this.hrefSelector = hrefSelector;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }
}
