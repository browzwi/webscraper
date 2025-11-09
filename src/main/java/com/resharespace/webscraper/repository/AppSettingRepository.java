package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing application settings in the database.
 * Provides CRUD operations for AppSetting entities, which store
 * runtime configuration values that can be changed without redeployment.
 *
 * @since 1.0
 */
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
