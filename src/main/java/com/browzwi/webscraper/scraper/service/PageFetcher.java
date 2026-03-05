package com.browzwi.webscraper.scraper.service;

/**
 * Interface for fetching HTML content from web pages.
 * Implementations may use different strategies for retrieving content,
 * such as simple HTTP requests or headless browser rendering.
 *
 * @since 1.0
 */
public interface PageFetcher {
    /**
     * Fetches HTML content from the specified URL.
     *
     * @param url the URL to fetch content from
     * @return the HTML content as a string
     * @throws RuntimeException if there's an error fetching the content
     */
    String fetch(String url);
}
