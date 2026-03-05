package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.DiscoveryJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing discovery job entities.
 *
 * @since 1.0
 */
@Repository
public interface DiscoveryJobRepository extends JpaRepository<DiscoveryJob, Long> {

    List<DiscoveryJob> findAllByOrderByCreatedAtDesc();
}
