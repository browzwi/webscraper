package com.browzwi.webscraper.service.settings;

/**
 * Enumeration of available URL discovery source types.
 * Both sources use Playwright for scraping - no paid APIs required.
 *
 * @since 1.0
 */
public enum DiscoverySourceType {

    /**
     * Scrapes Google Maps using Playwright.
     */
    GOOGLE_MAPS,

    /**
     * Scrapes regular Google Search results using Playwright.
     */
    GOOGLE_SEARCH
}
