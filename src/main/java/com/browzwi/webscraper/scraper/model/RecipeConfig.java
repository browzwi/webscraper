package com.browzwi.webscraper.scraper.model;

/**
 * Configuration representing a complete scraping recipe that defines how to
 * extract data from web pages. This is the root configuration object that
 * encompasses all aspects of a scraping operation.
 *
 * @since 1.0
 */
public class RecipeConfig {

    private String name;
    private String description;
    private MatchConfig match;
    private OptionsConfig options;
    private PageConfig page;

    /**
     * Gets the display name of this scraping recipe.
     *
     * @return the recipe name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of this scraping recipe.
     *
     * @param name the recipe name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description of this scraping recipe.
     *
     * @return the recipe description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this scraping recipe.
     *
     * @param description the recipe description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the URL matching configuration that determines which URLs to process.
     *
     * @return the URL matching configuration
     */
    public MatchConfig getMatch() {
        return match;
    }

    /**
     * Sets the URL matching configuration that determines which URLs to process.
     *
     * @param match the URL matching configuration to set
     */
    public void setMatch(MatchConfig match) {
        this.match = match;
    }

    /**
     * Gets the processing options configuration for HTML content.
     *
     * @return the options configuration
     */
    public OptionsConfig getOptions() {
        return options;
    }

    /**
     * Sets the processing options configuration for HTML content.
     *
     * @param options the options configuration to set
     */
    public void setOptions(OptionsConfig options) {
        this.options = options;
    }

    /**
     * Gets the page configuration defining how to extract data from web pages.
     *
     * @return the page configuration
     */
    public PageConfig getPage() {
        return page;
    }

    /**
     * Sets the page configuration defining how to extract data from web pages.
     *
     * @param page the page configuration to set
     */
    public void setPage(PageConfig page) {
        this.page = page;
    }
}
