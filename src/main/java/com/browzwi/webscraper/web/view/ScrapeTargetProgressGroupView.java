package com.browzwi.webscraper.web.view;

import java.util.List;

/**
 * Represents a logical grouping of scrape progress steps for a specific page when multi-page
 * scraping is in effect.
 * Enables the UI to present a clean timeline per page without parsing raw progress strings
 * client-side, keeping business rules on the server.
 * Step descriptions are preserved in order and assumed to be human-readable.
 *
 * @param title the display title for this progress group
 * @param url the URL associated with this progress group
 * @param steps the list of progress steps for this group
 * @since 1.0
 */
public record ScrapeTargetProgressGroupView(
        String title,
        String url,
        List<String> steps) {

    /**
     * Checks if this progress group has a valid URL.
     *
     * @return true if the URL is not null and not blank, false otherwise
     */
    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }
}
