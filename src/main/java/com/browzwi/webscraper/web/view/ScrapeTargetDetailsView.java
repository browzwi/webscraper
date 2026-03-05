package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeTargetStatus;
import java.util.UUID;

/**
 * Encapsulates the detailed result presentation for a scrape target including execution metadata,
 * formatted timestamps, and structured payloads.
 * Centralises rendering decisions for the target detail fragment so the controller can focus on
 * orchestration while keeping Thymeleaf markup free from conditional logic around missing data.
 * Structured JSON is stored as a pre-formatted string to avoid runtime parsing inside the template
 * and the archive flag explicitly signals when the download action is available.
 *
 * @param id the unique identifier for the target
 * @param url the URL that was scraped
 * @param status the current status of the target
 * @param startedAtLabel a formatted label for when scraping started
 * @param finishedAtLabel a formatted label for when scraping finished
 * @param durationLabel a formatted label for the duration of scraping
 * @param errorMessage the error message if scraping failed
 * @param hasError whether an error occurred during scraping
 * @param hasResult whether a result is available
 * @param structuredJson the structured data extracted from the page as a JSON string
 * @param archiveAvailable whether an archive can be created for this target
 * @param archiveFileName the filename for the archive if available
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
