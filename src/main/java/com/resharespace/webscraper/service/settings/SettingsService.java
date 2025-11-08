package com.browzwi.webscraper.service.settings;

import com.browzwi.webscraper.domain.AppSetting;
import com.browzwi.webscraper.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {

    private static final String SCRAPER_FETCHER_KEY = "scraper.fetcher";

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
    }

    public ScrapeFetcherType getFetcherType() {
        return repository.findById(SCRAPER_FETCHER_KEY)
                .map(setting -> ScrapeFetcherType.valueOf(setting.getValue()))
                .orElse(ScrapeFetcherType.HTMLUNIT);
    }

    public void updateFetcherType(ScrapeFetcherType type) {
        repository.save(new AppSetting(SCRAPER_FETCHER_KEY, type.name()));
    }
}
