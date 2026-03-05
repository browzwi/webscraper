package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.DiscoverySourceType;

public class SettingsForm {

    private ScrapeFetcherType fetcherType;
    private DiscoverySourceType discoverySource;
    private String googleSearchSiteFilter;
    private String claudeApiKey;

    public ScrapeFetcherType getFetcherType() {
        return fetcherType;
    }

    public void setFetcherType(ScrapeFetcherType fetcherType) {
        this.fetcherType = fetcherType;
    }

    public DiscoverySourceType getDiscoverySource() {
        return discoverySource;
    }

    public void setDiscoverySource(DiscoverySourceType discoverySource) {
        this.discoverySource = discoverySource;
    }

    public String getGoogleSearchSiteFilter() {
        return googleSearchSiteFilter;
    }

    public void setGoogleSearchSiteFilter(String googleSearchSiteFilter) {
        this.googleSearchSiteFilter = googleSearchSiteFilter;
    }

    public String getClaudeApiKey() {
        return claudeApiKey;
    }

    public void setClaudeApiKey(String claudeApiKey) {
        this.claudeApiKey = claudeApiKey;
    }
}
