package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;

public class SettingsForm {

    private ScrapeFetcherType fetcherType;

    public ScrapeFetcherType getFetcherType() {
        return fetcherType;
    }

    public void setFetcherType(ScrapeFetcherType fetcherType) {
        this.fetcherType = fetcherType;
    }
}
