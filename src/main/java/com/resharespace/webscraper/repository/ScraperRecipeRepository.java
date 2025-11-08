package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScraperRecipe;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScraperRecipeRepository extends JpaRepository<ScraperRecipe, UUID> {
    Optional<ScraperRecipe> findByKey(String key);
}
