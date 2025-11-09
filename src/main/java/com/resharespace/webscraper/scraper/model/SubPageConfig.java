package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

public class SubPageConfig {

    private String path;
    private List<FieldConfig> fields = new ArrayList<>();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FieldConfig> getFields() {
        return fields;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }
}
