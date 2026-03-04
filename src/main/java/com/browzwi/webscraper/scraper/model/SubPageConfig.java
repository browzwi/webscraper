package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for scraping a sub-page as part of a multi-page scraping operation.
 * Defines the path relative to the seed URL and the fields to extract from the sub-page.
 *
 * @since 1.0
 */
public class SubPageConfig {

    private String path;
    private List<FieldConfig> fields = new ArrayList<>();

    /**
     * Gets the path of the sub-page relative to the seed URL in a multi-page scrape.
     *
     * @return the sub-page path
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the path of the sub-page relative to the seed URL in a multi-page scrape.
     *
     * @param path the sub-page path to set
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Gets the list of fields to extract from the sub-page.
     *
     * @return the list of field configurations for the sub-page
     */
    public List<FieldConfig> getFields() {
        return fields;
    }

    /**
     * Sets the list of fields to extract from the sub-page.
     *
     * @param fields the list of field configurations for the sub-page to set
     */
    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }
}
