package com.browzwi.webscraper.domain;

/**
 * Represents the possible execution states of a scraping job.
 *
 * <p>Architectural rationale: This enum provides a type-safe way to track
 * the lifecycle of scraping jobs, allowing the system to monitor and manage
 * job execution states effectively.
 *
 * <p>Key constraints: Job status transitions follow a specific sequence
 * to ensure proper tracking and reporting of job execution.
 *
 * @since 1.0
 */
public enum ScrapeJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
