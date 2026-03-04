package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for extracting a specific field from a web page during scraping.
 * Defines the selector, source type, and extraction behavior for a single data field.
 *
 * @since 1.0
 */
public class FieldConfig {

    private String name;
    private List<String> selectors = new ArrayList<>();
    private DataSourceType source = DataSourceType.TEXT;
    private String attributeName;
    private boolean multiple;
    private String dataPattern;

    /**
     * Gets the name of this field in the extracted data structure.
     *
     * @return the field name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this field in the extracted data structure.
     *
     * @param name the field name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the list of CSS selectors to use for finding the element(s) to extract from.
     *
     * @return the list of CSS selectors
     */
    public List<String> getSelectors() {
        return selectors;
    }

    /**
     * Sets the list of CSS selectors to use for finding the element(s) to extract from.
     *
     * @param selectors the list of CSS selectors to set
     */
    public void setSelectors(List<String> selectors) {
        this.selectors = selectors;
    }

    /**
     * Gets the source type that determines how data is extracted from the selected element(s).
     *
     * @return the data source type
     */
    public DataSourceType getSource() {
        return source;
    }

    /**
     * Sets the source type that determines how data is extracted from the selected element(s).
     *
     * @param source the data source type to set
     */
    public void setSource(DataSourceType source) {
        this.source = source;
    }

    /**
     * Gets the name of the attribute to extract when the source type is ATTR.
     *
     * @return the attribute name, or null if not applicable
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Sets the name of the attribute to extract when the source type is ATTR.
     *
     * @param attributeName the attribute name to set
     */
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    /**
     * Checks if multiple elements matching the selectors should all be extracted.
     *
     * @return true if multiple elements should be extracted, false for first match only
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Sets whether multiple elements matching the selectors should all be extracted.
     *
     * @param multiple true to extract all matching elements, false for first match only
     */
    public void setMultiple(boolean multiple) {
        this.multiple = multiple;
    }

    /**
     * Gets the regular expression pattern to apply to the extracted data for further processing.
     *
     * @return the data pattern, or null if no pattern is applied
     */
    public String getDataPattern() {
        return dataPattern;
    }

    /**
     * Sets the regular expression pattern to apply to the extracted data for further processing.
     *
     * @param dataPattern the data pattern to set
     */
    public void setDataPattern(String dataPattern) {
        this.dataPattern = dataPattern;
    }
}
