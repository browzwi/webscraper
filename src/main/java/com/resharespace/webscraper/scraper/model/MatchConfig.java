package com.browzwi.webscraper.scraper.model;

import java.util.ArrayList;
import java.util.List;

public class MatchConfig {

    private List<String> domains = new ArrayList<>();
    private List<String> urlRegexes = new ArrayList<>();

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public List<String> getUrlRegexes() {
        return urlRegexes;
    }

    public void setUrlRegexes(List<String> urlRegexes) {
        this.urlRegexes = urlRegexes;
    }
}
