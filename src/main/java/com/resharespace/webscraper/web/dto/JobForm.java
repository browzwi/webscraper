package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JobForm {

    @NotBlank
    private String name;

    @NotBlank
    private String recipeId;

    @NotBlank
    private String urls;

    private String cronExpression;
    private boolean stripCss;
    private boolean stripJs;
    private boolean removeAttributes;
    private boolean extractHrefsFirst;

    public List<String> urlList() {
        return Arrays.stream(urls.split("\r?\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public OptionsConfig toOptionsConfig() {
        OptionsConfig options = new OptionsConfig();
        options.setStripCss(stripCss);
        options.setStripJs(stripJs);
        options.setRemoveAttributes(removeAttributes);
        options.setExtractHrefsFirst(extractHrefsFirst);
        return options;
    }

    public UUID recipeUuid() {
        return UUID.fromString(recipeId);
    }

    // getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isStripCss() {
        return stripCss;
    }

    public void setStripCss(boolean stripCss) {
        this.stripCss = stripCss;
    }

    public boolean isStripJs() {
        return stripJs;
    }

    public void setStripJs(boolean stripJs) {
        this.stripJs = stripJs;
    }

    public boolean isRemoveAttributes() {
        return removeAttributes;
    }

    public void setRemoveAttributes(boolean removeAttributes) {
        this.removeAttributes = removeAttributes;
    }

    public boolean isExtractHrefsFirst() {
        return extractHrefsFirst;
    }

    public void setExtractHrefsFirst(boolean extractHrefsFirst) {
        this.extractHrefsFirst = extractHrefsFirst;
    }
}
