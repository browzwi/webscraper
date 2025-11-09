package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import java.util.List;
import java.util.UUID;

/**
 * Communicates scrape progress details to the modal fragment, including grouped steps and timing
 * metadata for the selected target.
 *
 * <p>Architectural rationale: isolates progress formatting logic from the template while supporting
 * multi-page scrapes through grouped step collections.
 *
 * <p>Key constraints: timestamps are represented as formatted strings to avoid runtime conversion in
 * Thymeleaf; duration can be empty when execution is still running.
 *
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

    public boolean hasGroups() {
        return groups != null && !groups.isEmpty();
    }
}
