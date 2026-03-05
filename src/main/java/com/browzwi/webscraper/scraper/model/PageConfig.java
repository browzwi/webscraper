package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for scraping a single web page or a main page in a multi-page scrape.
 * Defines the selectors, fields to extract, and related sub-pages to follow.
 *
 * @since 1.0
 */
public class PageConfig {

    private String contentRoot;
    private String hrefSelector;
    private List<FieldConfig> fields = new ArrayList<>();
    private List<SubPageConfig> subPages = new ArrayList<>();

    /**
     * Gets the CSS selector for the root element containing the content to extract.
     *
     * @return the content root selector, or null if not specified
     */
    public String getContentRoot() {
        return contentRoot;
    }

    /**
     * Sets the CSS selector for the root element containing the content to extract.
     *
     * @param contentRoot the content root selector to set
     */
    public void setContentRoot(String contentRoot) {
        this.contentRoot = contentRoot;
    }

    /**
     * Gets the CSS selector for finding href links on the page.
     *
     * @return the href selector, or null if not specified
     */
    public String getHrefSelector() {
        return hrefSelector;
    }

    /**
     * Sets the CSS selector for finding href links on the page.
     *
     * @param hrefSelector the href selector to set
     */
    public void setHrefSelector(String hrefSelector) {
        this.hrefSelector = hrefSelector;
    }

    /**
     * Gets the list of fields to extract from the page.
     *
     * @return the list of field configurations
     */
    public List<FieldConfig> getFields() {
        return fields;
    }

    /**
     * Sets the list of fields to extract from the page.
     *
     * @param fields the list of field configurations to set
     */
    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    /**
     * Gets the list of sub-pages to follow and scrape from this page.
     *
     * @return the list of sub-page configurations
     */
    public List<SubPageConfig> getSubPages() {
        return subPages;
    }

    /**
     * Sets the list of sub-pages to follow and scrape from this page.
     *
     * @param subPages the list of sub-page configurations to set
     */
    public void setSubPages(List<SubPageConfig> subPages) {
        this.subPages = subPages;
    }
}
