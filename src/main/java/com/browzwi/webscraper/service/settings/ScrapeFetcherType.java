package com.browzwi.webscraper.service.settings;

/**
 * Enum representing the different types of fetchers that can be used for scraping web content.
 * Each type represents a different strategy for retrieving and rendering web pages.
 *
 * @since 1.0
 */
public enum ScrapeFetcherType {
    /**
     * Uses HtmlUnit for fetching and rendering HTML content.
     */
    HTMLUNIT,
    /**
     * Uses Playwright for full browser capabilities when scraping JavaScript-heavy sites.
     */
    PLAYWRIGHT
}
