package com.browzwi.webscraper.scraper.service;

import java.util.List;
import java.util.Map;

/**
 * Record representing the result of a multi-page scraping operation.
 * Contains the combined data from all pages, individual page results,
 * and progress information.
 *
 * @param combinedStructuredData the combined structured data from all pages
 * @param pageResults mapping of page identifiers to individual page results
 * @param progressSteps the list of steps taken during the scraping process
 * @since 1.0
 */
public record MultiPageScrapeResult(
    Map<String, Object> combinedStructuredData,
    Map<String, PageResult> pageResults,
    List<String> progressSteps
) {
    
    /**
     * Record representing the result of scraping a single page in a multi-page operation.
     *
     * @param url the URL of the page that was scraped
     * @param rawHtml the original HTML content from the page
     * @param processedHtml the processed HTML content after applying rules
     * @param processedMarkdown the processed content converted to Markdown
     * @param structuredData the extracted structured data from the page
     * @param hrefs the list of URLs extracted from href attributes
     * @since 1.0
     */
    public record PageResult(
        String url,
        String rawHtml,
        String processedHtml,
        String processedMarkdown,
        Map<String, Object> structuredData,
        List<String> hrefs
    ) {}
}
