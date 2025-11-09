package com.browzwi.webscraper.web.view;

import java.util.List;

/**
 * Represents a logical grouping of scrape progress steps for a specific page when multi-page
 * scraping is in effect.
 *
 * <p>Architectural rationale: enables the UI to present a clean timeline per page without parsing
 * raw progress strings client-side, keeping business rules on the server.
 *
 * <p>Key constraints: step descriptions are preserved in order and assumed to be human-readable.
 *
 * @since 1.0
 */
public record ScrapeTargetProgressGroupView(
        String title,
        String url,
        List<String> steps) {

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }
}
