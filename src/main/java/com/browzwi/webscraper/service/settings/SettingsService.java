package com.browzwi.webscraper.service.settings;

import com.browzwi.webscraper.domain.AppSetting;
import com.browzwi.webscraper.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * Service for managing application-level settings that persist across application restarts.
 *
 * <p>Architectural rationale: This service provides a centralized way to store and retrieve
 * runtime configuration settings that can be changed without modifying code or application
 * properties, such as the preferred web scraping engine.
 *
 * <p>Key constraints: Settings are stored in the database and default values are ensured
 * on application startup to maintain consistent behavior.
 *
 * @since 1.0
 */
@Service
public class SettingsService {

    private static final String SCRAPER_FETCHER_KEY = "scraper.fetcher";

    private final AppSettingRepository repository;

    /**
     * Constructor for SettingsService with required dependencies.
     *
     * @param repository repository for managing application settings
     */
    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    /**
     * Ensures default application settings exist in the database on startup.
     *
     * <p>Implementation rationale: This method is called after the bean is constructed
     * to ensure critical settings have default values. This prevents runtime errors
     * if the setting doesn't exist in the database.
     */
    @PostConstruct
    void ensureDefaults() {
        repository.findById(SCRAPER_FETCHER_KEY)
                .or(() -> {
                    repository.save(new AppSetting(SCRAPER_FETCHER_KEY, ScrapeFetcherType.HTMLUNIT.name()));
                    return repository.findById(SCRAPER_FETCHER_KEY);
                });
    }

    /**
     * Gets the currently configured scraper fetcher type.
     *
     * @return the current fetcher type, defaulting to HTMLUNIT if not set
     */
    public ScrapeFetcherType getFetcherType() {
        return repository.findById(SCRAPER_FETCHER_KEY)
                .map(setting -> ScrapeFetcherType.valueOf(setting.getValue()))
                .orElse(ScrapeFetcherType.HTMLUNIT);
    }

    /**
     * Updates the scraper fetcher type in the application settings.
     *
     * @param type the new fetcher type to use for scraping operations
     */
    public void updateFetcherType(ScrapeFetcherType type) {
        repository.save(new AppSetting(SCRAPER_FETCHER_KEY, type.name()));
    }
}
