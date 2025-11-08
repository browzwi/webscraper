package com.browzwi.webscraper.repository;

import com.browzwi.webscraper.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}
