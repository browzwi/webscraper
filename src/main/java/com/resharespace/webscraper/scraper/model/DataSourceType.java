package com.browzwi.webscraper.scraper.model;

/**
 * Defines the different types of data sources for field extraction in scraping recipes.
 * Each type specifies how data should be extracted from HTML elements.
 *
 * @since 1.0
 */
public enum DataSourceType {
    /**
     * Extracts the text content of an element.
     */
    TEXT,
    
    /**
     * Extracts the HTML content of an element.
     */
    HTML,
    
    /**
     * Extracts the value of a specific attribute of an element.
     */
    ATTR
}
