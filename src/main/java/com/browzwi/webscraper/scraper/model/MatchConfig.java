package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for matching URLs during scraping operations.
 * Defines criteria for determining which URLs should be processed based on domain
 * or URL pattern matching.
 *
 * @since 1.0
 */
public class MatchConfig {

    private List<String> domains = new ArrayList<>();
    private List<String> urlRegexes = new ArrayList<>();

    /**
     * Gets the list of allowed domains for URL matching.
     *
     * @return the list of allowed domains
     */
    public List<String> getDomains() {
        return domains;
    }

    /**
     * Sets the list of allowed domains for URL matching.
     *
     * @param domains the list of allowed domains to set
     */
    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    /**
     * Gets the list of regular expressions to match against URLs.
     *
     * @return the list of URL matching regular expressions
     */
    public List<String> getUrlRegexes() {
        return urlRegexes;
    }

    /**
     * Sets the list of regular expressions to match against URLs.
     *
     * @param urlRegexes the list of URL matching regular expressions to set
     */
    public void setUrlRegexes(List<String> urlRegexes) {
        this.urlRegexes = urlRegexes;
    }
}
