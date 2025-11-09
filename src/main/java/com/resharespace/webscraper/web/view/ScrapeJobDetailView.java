package com.browzwi.webscraper.web.view;

import com.browzwi.webscraper.domain.ScrapeJobStatus;
import java.util.List;
import java.util.UUID;

/**
 * Provides a presentation-safe snapshot of a scrape job and its associated targets for the detail
 * page.
 *
 * <p>Architectural rationale: decouples the Thymeleaf template from JPA entities and centralises
 * formatting of schedule metadata and target summaries so the controller can honour the
 * Single Responsibility Principle.
 *
 * <p>Key constraints: targets are represented as lightweight {@link ScrapeTargetRowView} records to
 * minimise template logic while preserving ordering defined by the service layer.
 *
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
