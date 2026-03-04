package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing scraping jobs in the database.
 * Provides CRUD operations for ScrapeJob entities along with
 * custom queries for retrieving jobs with their associated recipe data.
 *
 * @since 1.0
 */
public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, UUID> {

    /**
     * Retrieves all scraping jobs ordered by creation date (newest first).
     * This method uses an entity graph to eagerly fetch the associated recipe data,
     * avoiding N+1 query problems when accessing job recipes.
     *
     * @return a list of all scraping jobs ordered by creation date
     */
    @EntityGraph(attributePaths = "recipe")
    List<ScrapeJob> findAllByOrderByCreatedAtDesc();

    /**
     * Retrieves a scraping job by its ID, including its associated recipe data.
     * This method uses an entity graph to eagerly fetch the recipe data.
     *
     * @param id the UUID of the job to retrieve
     * @return an optional containing the job if found, or empty if not found
     */
    @EntityGraph(attributePaths = "recipe")
    Optional<ScrapeJob> findWithRecipeById(UUID id);
}
