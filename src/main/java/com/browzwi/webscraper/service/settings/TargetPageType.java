package com.browzwi.webscraper.service.settings;

/**
 * Defines the types of target pages that can be discovered via Google Search.
 * Each type specifies a search pattern and target domain for URL extraction.
 */
public enum TargetPageType {
    FACEBOOK("Facebook", "facebook.com", "{name} {location} Facebook"),
    WEBSITE("Official Website", "", "{name} {location} official website"),
    INSTAGRAM("Instagram", "instagram.com", "{name} {location} Instagram"),
    LINKEDIN("LinkedIn", "linkedin.com", "{name} {location} LinkedIn"),
    TWITTER("Twitter/X", "twitter.com", "{name} {location} Twitter"),
    TIKTOK("TikTok", "tiktok.com", "{name} {location} TikTok"),
    YOUTUBE("YouTube", "youtube.com", "{name} {location} YouTube"),
    CUSTOM("Custom", "", "");  // User-defined pattern

    private final String displayName;
    private final String targetDomain;
    private final String searchPattern;

    TargetPageType(String displayName, String targetDomain, String searchPattern) {
        this.displayName = displayName;
        this.targetDomain = targetDomain;
        this.searchPattern = searchPattern;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTargetDomain() {
        return targetDomain;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    /**
     * Builds the Google Search query for this target type.
     *
     * @param businessName The business name from Google Maps
     * @param location The location to narrow results
     * @param customPattern Optional custom pattern (only used for CUSTOM type)
     * @return Formatted search query
     */
    public String buildSearchQuery(String businessName, String location, String customPattern) {
        if (this == CUSTOM && customPattern != null && !customPattern.isBlank()) {
            return businessName + " " + location + " " + customPattern;
        }
        return searchPattern
            .replace("{name}", businessName)
            .replace("{location}", location);
    }
}
