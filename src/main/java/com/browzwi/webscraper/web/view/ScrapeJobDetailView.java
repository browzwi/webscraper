package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeJobStatus;
import java.util.List;
import java.util.UUID;

/**
 * Provides a presentation-safe snapshot of a scrape job and its associated targets for the detail page.
 * Decouples the Thymeleaf template from JPA entities and centralises formatting of schedule metadata
 * and target summaries so the controller can honour the Single Responsibility Principle.
 * Targets are represented as lightweight {@link ScrapeTargetRowView} records to minimise template
 * logic while preserving ordering defined by the service layer.
 *
 * @param id the unique identifier for the job
 * @param name the display name of the job
 * @param recipeName the name of the recipe used by the job
 * @param status the current status of the job
 * @param scheduleLabel a human-readable label for the job's schedule
 * @param countdownLabel a label showing time until the next scheduled execution
 * @param requestedBy the user who requested the job
 * @param targets the list of target views associated with this job
 * @since 1.0
 */
public record ScrapeJobDetailView(
        UUID id,
        String name,
        String recipeName,
        ScrapeJobStatus status,
        String scheduleLabel,
        String countdownLabel,
        String requestedBy,
        List<ScrapeTargetRowView> targets) {
}
