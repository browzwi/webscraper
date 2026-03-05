package com.browzwi.webscraper.service.settings;

import com.browzwi.webscraper.domain.AppSetting;
import com.browzwi.webscraper.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * Service for managing application-level settings.
 *
 * @since 1.0
 */
@Service
public class SettingsService {

    private static final String SCRAPER_FETCHER_KEY = "scraper.fetcher";
    private static final String DISCOVERY_SOURCE_KEY = "discovery.source";
    private static final String GOOGLE_SEARCH_SITE_FILTER_KEY = "discovery.google_search_site_filter";
    private static final String CLAUDE_API_KEY = "discovery.claude_api_key";

    private final AppSettingRepository repository;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void ensureDefaults() {
        repository.findById(SCRAPER_FETCHER_KEY)
                .or(() -> {
                    repository.save(new AppSetting(SCRAPER_FETCHER_KEY, ScrapeFetcherType.HTMLUNIT.name()));
                    return repository.findById(SCRAPER_FETCHER_KEY);
                });

        repository.findById(DISCOVERY_SOURCE_KEY)
                .or(() -> {
                    repository.save(new AppSetting(DISCOVERY_SOURCE_KEY, DiscoverySourceType.GOOGLE_MAPS.name()));
                    return repository.findById(DISCOVERY_SOURCE_KEY);
                });

        repository.findById(GOOGLE_SEARCH_SITE_FILTER_KEY)
                .or(() -> {
                    repository.save(new AppSetting(GOOGLE_SEARCH_SITE_FILTER_KEY, ""));
                    return repository.findById(GOOGLE_SEARCH_SITE_FILTER_KEY);
                });

        repository.findById(CLAUDE_API_KEY)
                .or(() -> {
                    repository.save(new AppSetting(CLAUDE_API_KEY, ""));
                    return repository.findById(CLAUDE_API_KEY);
                });
    }

    public ScrapeFetcherType getFetcherType() {
        return repository.findById(SCRAPER_FETCHER_KEY)
                .map(setting -> ScrapeFetcherType.valueOf(setting.getValue()))
                .orElse(ScrapeFetcherType.HTMLUNIT);
    }

    public void updateFetcherType(ScrapeFetcherType type) {
        repository.save(new AppSetting(SCRAPER_FETCHER_KEY, type.name()));
    }

    public String getDiscoverySourceType() {
        return repository.findById(DISCOVERY_SOURCE_KEY)
                .map(AppSetting::getValue)
                .orElse(DiscoverySourceType.GOOGLE_MAPS.name());
    }

    public void updateDiscoverySourceType(DiscoverySourceType type) {
        repository.save(new AppSetting(DISCOVERY_SOURCE_KEY, type.name()));
    }

    public String getGoogleSearchSiteFilter() {
        return repository.findById(GOOGLE_SEARCH_SITE_FILTER_KEY)
                .map(AppSetting::getValue)
                .orElse("");
    }

    public void updateGoogleSearchSiteFilter(String siteFilter) {
        repository.save(new AppSetting(GOOGLE_SEARCH_SITE_FILTER_KEY, siteFilter != null ? siteFilter : ""));
    }

    public String getClaudeApiKey() {
        return repository.findById(CLAUDE_API_KEY)
                .map(AppSetting::getValue)
                .orElse("");
    }

    public void updateClaudeApiKey(String apiKey) {
        repository.save(new AppSetting(CLAUDE_API_KEY, apiKey != null ? apiKey : ""));
    }
}
