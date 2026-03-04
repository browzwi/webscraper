package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScraperRecipe;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing scraper recipes in the database.
 * Provides CRUD operations for ScraperRecipe entities and
 * methods for retrieving recipes by their unique key.
 *
 * @since 1.0
 */
public interface ScraperRecipeRepository extends JpaRepository<ScraperRecipe, UUID> {
    /**
     * Finds a scraper recipe by its unique key.
     *
     * @param key the unique key identifying the recipe
     * @return an optional containing the recipe if found, or empty if not found
     */
    Optional<ScraperRecipe> findByKey(String key);
}
