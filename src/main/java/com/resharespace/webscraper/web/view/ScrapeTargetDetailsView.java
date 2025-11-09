package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import java.util.UUID;

/**
 * Encapsulates the detailed result presentation for a scrape target including execution metadata,
 * formatted timestamps, and structured payloads.
 *
 * <p>Architectural rationale: centralises rendering decisions for the target detail fragment so the
 * controller can focus on orchestration while keeping Thymeleaf markup free from conditional logic
 * around missing data.
 *
 * <p>Key constraints: structured JSON is stored as a pre-formatted string to avoid runtime parsing
 * inside the template and the archive flag explicitly signals when the download action is available.
 *
 * @since 1.0
 */
public record ScrapeTargetDetailsView(
        UUID id,
        String url,
        ScrapeTargetStatus status,
        String startedAtLabel,
        String finishedAtLabel,
        String durationLabel,
        String errorMessage,
        boolean hasError,
        boolean hasResult,
        String structuredJson,
        boolean archiveAvailable,
        String archiveFileName) {
}
