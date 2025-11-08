package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, UUID> {
}
