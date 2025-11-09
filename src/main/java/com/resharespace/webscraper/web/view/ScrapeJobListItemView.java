package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeJobStatus;
import java.util.UUID;

/**
 * Aggregates the essential job list information required to render the jobs table without
 * exposing persistence entities directly to the view layer.
 *
 * <p>Architectural rationale: separates Thymeleaf presentation concerns from the JPA model so the
 * UI can evolve independently of persistence mappings while still conveying schedule metadata such
 * as cron expressions and countdowns.
 *
 * <p>Key constraints: countdown text is pre-formatted to avoid complex expressions inside
 * Thymeleaf templates.
 *
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
