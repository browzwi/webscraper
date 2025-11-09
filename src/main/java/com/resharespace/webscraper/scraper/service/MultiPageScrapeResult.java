package com.browzwi.webscraper.scraper.service;

import java.util.List;
import java.util.Map;

public record MultiPageScrapeResult(
    Map<String, Object> combinedStructuredData,
    Map<String, PageResult> pageResults,
    List<String> progressSteps
) {
    
    public record PageResult(
        String url,
        String rawHtml,
        String processedHtml,
        String processedMarkdown,
        Map<String, Object> structuredData,
        List<String> hrefs
    ) {}
}
