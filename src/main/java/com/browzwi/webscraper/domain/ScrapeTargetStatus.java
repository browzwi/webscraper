package com.browzwi.webscraper.domain;

/**
 * Represents the possible execution states of a scrape target.
 *
 * <p>Architectural rationale: This enum provides a type-safe way to track
 * the lifecycle of individual targets within a scraping job, allowing
 * detailed monitoring of each URL's processing state.
 *
 * <p>Key constraints: Target status transitions follow a specific sequence
 * to ensure proper tracking and error handling of individual targets.
 *
 * @since 1.0
 */
public enum ScrapeTargetStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
