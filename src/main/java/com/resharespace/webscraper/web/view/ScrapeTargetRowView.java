package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import java.util.UUID;

/**
 * Prepares a scrape target summary for table rendering including formatted timestamps and
 * lightweight status information.
 * Avoids duplicating temporal formatting logic inside the Thymeleaf template while keeping
 * persistence entities out of the view tier.
 * Timestamp strings are precomputed to ensure NullPointerExceptions cannot occur inside the
 * template when optional values are absent.
 *
 * @param id the unique identifier for the target
 * @param url the URL that was scraped
 * @param status the current status of the target
 * @param startedAtLabel a formatted label for when scraping started
 * @param finishedAtLabel a formatted label for when scraping finished
 * @param errorMessage the error message if scraping failed
 * @since 1.0
 */
public record ScrapeTargetRowView(
        UUID id,
        String url,
        ScrapeTargetStatus status,
        String startedAtLabel,
        String finishedAtLabel,
        String errorMessage) {

    /**
     * Checks if this target has an error message.
     *
     * @return true if the error message is not null and not blank, false otherwise
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.isBlank();
    }
}
