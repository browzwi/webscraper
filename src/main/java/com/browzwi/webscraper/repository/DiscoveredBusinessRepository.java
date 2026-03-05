package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing discovered business entities.
 *
 * @since 1.0
 */
@Repository
public interface DiscoveredBusinessRepository extends JpaRepository<DiscoveredBusiness, Long> {

    List<DiscoveredBusiness> findByDiscoveryJobIdOrderByBusinessNameAsc(Long discoveryJobId);
}
