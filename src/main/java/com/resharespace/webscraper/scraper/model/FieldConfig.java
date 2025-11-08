package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

public class FieldConfig {

    private String name;
    private List<String> selectors = new ArrayList<>();
    private DataSourceType source = DataSourceType.TEXT;
    private String attributeName;
    private boolean multiple;
    private String dataPattern;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSelectors() {
        return selectors;
    }

    public void setSelectors(List<String> selectors) {
        this.selectors = selectors;
    }

    public DataSourceType getSource() {
        return source;
    }

    public void setSource(DataSourceType source) {
        this.source = source;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    public String getDataPattern() {
        return dataPattern;
    }

    public void setDataPattern(String dataPattern) {
        this.dataPattern = dataPattern;
    }
}
