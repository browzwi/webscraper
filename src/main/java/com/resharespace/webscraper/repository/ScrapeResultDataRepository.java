package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.ScrapeResultData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScrapeResultDataRepository extends JpaRepository<ScrapeResultData, UUID> {
    
    @Query("SELECT srd FROM ScrapeResultData srd WHERE srd.target.id = :targetId ORDER BY srd.createdAt DESC")
    List<ScrapeResultData> findByTargetIdOrderByCreatedAtDesc(@Param("targetId") UUID targetId);
    
    default Optional<ScrapeResultData> findByTargetId(UUID targetId) {
        List<ScrapeResultData> results = findByTargetIdOrderByCreatedAtDesc(targetId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
