package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeTarget;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, UUID> {
}
