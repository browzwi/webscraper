package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import java.util.UUID;

/**
 * Prepares a scrape target summary for table rendering including formatted timestamps and
 * lightweight status information.
 *
 * <p>Architectural rationale: avoids duplicating temporal formatting logic inside the Thymeleaf
 * template while keeping persistence entities out of the view tier.
 *
 * <p>Key constraints: timestamp strings are precomputed to ensure NullPointerExceptions cannot occur
 * inside the template when optional values are absent.
 *
 * @since 1.0
 */
public record ScrapeTargetRowView(
        UUID id,
        String url,
        ScrapeTargetStatus status,
        String startedAtLabel,
        String finishedAtLabel,
        String errorMessage) {

    public boolean hasError() {
        return errorMessage != null && !errorMessage.isBlank();
    }
}
