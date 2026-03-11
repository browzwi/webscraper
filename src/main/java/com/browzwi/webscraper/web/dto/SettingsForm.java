package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.DiscoverySourceType;

public class SettingsForm {

    private ScrapeFetcherType fetcherType;

    public ScrapeFetcherType getFetcherType() {
        return fetcherType;
    }

    public void setFetcherType(ScrapeFetcherType fetcherType) {
        this.fetcherType = fetcherType;
    }
}
