package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeResultData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for managing scraping result data in the database.
 * Provides CRUD operations for ScrapeResultData entities and
 * methods for retrieving results associated with specific targets.
 *
 * @since 1.0
 */
public interface ScrapeResultDataRepository extends JpaRepository<ScrapeResultData, UUID> {
    
    /**
     * Retrieves all scraping result data for a specific target, ordered by creation date (newest first).
     *
     * @param targetId the UUID of the target whose results to retrieve
     * @return a list of result data for the specified target, ordered by creation date
     */
    @Query("SELECT srd FROM ScrapeResultData srd WHERE srd.target.id = :targetId ORDER BY srd.createdAt DESC")
    List<ScrapeResultData> findByTargetIdOrderByCreatedAtDesc(@Param("targetId") UUID targetId);
    
    /**
     * Retrieves the most recent scraping result data for a specific target.
     * This method returns the latest result by ordering results by creation date and taking the first.
     *
     * @param targetId the UUID of the target whose result to retrieve
     * @return an optional containing the most recent result data if found, or empty if not found
     */
    default Optional<ScrapeResultData> findByTargetId(UUID targetId) {
        List<ScrapeResultData> results = findByTargetIdOrderByCreatedAtDesc(targetId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
