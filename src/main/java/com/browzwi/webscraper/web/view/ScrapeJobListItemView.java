package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeJobStatus;
import java.util.UUID;

/**
 * Aggregates the essential job list information required to render the jobs table without
 * exposing persistence entities directly to the view layer.
 * Separates Thymeleaf presentation concerns from the JPA model so the UI can evolve independently
 * of persistence mappings while still conveying schedule metadata such as cron expressions and countdowns.
 * Countdown text is pre-formatted to avoid complex expressions inside Thymeleaf templates.
 *
 * @param id the unique identifier for the job
 * @param name the display name of the job
 * @param recipeName the name of the recipe used by the job
 * @param status the current status of the job
 * @param scheduleLabel a human-readable label for the job's schedule
 * @param countdownLabel a label showing time until the next scheduled execution
 * @param lastRunLabel a label showing when the job last ran
 * @since 1.0
 */
public record ScrapeJobListItemView(
        UUID id,
        String name,
        String recipeName,
        ScrapeJobStatus status,
        String scheduleLabel,
        String countdownLabel,
        String lastRunLabel) {
}
