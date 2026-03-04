package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeTarget;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing scraping targets in the database.
 * Provides CRUD operations for ScrapeTarget entities and
 * methods for retrieving targets associated with specific jobs.
 *
 * @since 1.0
 */
public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, UUID> {
    /**
     * Retrieves all scraping targets associated with a specific job.
     *
     * @param jobId the UUID of the job whose targets to retrieve
     * @return a list of scraping targets for the specified job
     */
    List<ScrapeTarget> findAllByJobId(UUID jobId);
}
