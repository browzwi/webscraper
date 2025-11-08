package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, UUID> {

    @EntityGraph(attributePaths = "recipe")
    List<ScrapeJob> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "recipe")
    Optional<ScrapeJob> findWithRecipeById(UUID id);
}
