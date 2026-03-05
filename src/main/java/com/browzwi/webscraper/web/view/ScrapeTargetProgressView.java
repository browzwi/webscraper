package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import java.util.List;
import java.util.UUID;

/**
 * Communicates scrape progress details to the modal fragment, including grouped steps and timing
 * metadata for the selected target.
 * Isolates progress formatting logic from the template while supporting multi-page scrapes
 * through grouped step collections.
 * Timestamps are represented as formatted strings to avoid runtime conversion in Thymeleaf;
 * duration can be empty when execution is still running.
 *
 * @param id the unique identifier for the target
 * @param url the URL that is being scraped
 * @param status the current status of the target
 * @param startedAtLabel a formatted label for when scraping started
 * @param finishedAtLabel a formatted label for when scraping finished
 * @param durationLabel a formatted label for the duration of scraping
 * @param groups the list of progress groups for multi-page scrapes
 * @since 1.0
 */
public record ScrapeTargetProgressView(
        UUID id,
        String url,
        ScrapeTargetStatus status,
        String startedAtLabel,
        String finishedAtLabel,
        String durationLabel,
        List<ScrapeTargetProgressGroupView> groups) {

    /**
     * Checks if this progress view has any progress groups.
     *
     * @return true if the groups list is not null and not empty, false otherwise
     */
    public boolean hasGroups() {
        return groups != null && !groups.isEmpty();
    }
}
