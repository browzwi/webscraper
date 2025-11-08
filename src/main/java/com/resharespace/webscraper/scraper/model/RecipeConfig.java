package com.browzwi.webscraper.scraper.model;

public class RecipeConfig {

    private String name;
    private String description;
    private MatchConfig match;
    private OptionsConfig options;
    private PageConfig page;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MatchConfig getMatch() {
        return match;
    }

    public void setMatch(MatchConfig match) {
        this.match = match;
    }

    public OptionsConfig getOptions() {
        return options;
    }

    public void setOptions(OptionsConfig options) {
        this.options = options;
    }

    public PageConfig getPage() {
        return page;
    }

    public void setPage(PageConfig page) {
        this.page = page;
    }
}
