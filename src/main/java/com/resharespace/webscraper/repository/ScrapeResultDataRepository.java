package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeResultData;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapeResultDataRepository extends JpaRepository<ScrapeResultData, UUID> {
}
