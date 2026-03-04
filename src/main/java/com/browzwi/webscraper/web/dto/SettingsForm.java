package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.service.settings.ScrapeFetcherType;

/**
 * Data transfer object for application settings form data.
 * Contains the fields for configuring application settings through the web interface.
 *
 * @since 1.0
 */
public class SettingsForm {

    private ScrapeFetcherType fetcherType;

    /**
     * Gets the selected scraper fetcher type.
     *
     * @return the selected fetcher type
     */
    public ScrapeFetcherType getFetcherType() {
        return fetcherType;
    }

    /**
     * Sets the scraper fetcher type.
     *
     * @param fetcherType the fetcher type to set
     */
    public void setFetcherType(ScrapeFetcherType fetcherType) {
        this.fetcherType = fetcherType;
    }
}
