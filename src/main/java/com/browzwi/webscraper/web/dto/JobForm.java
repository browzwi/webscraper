package com.browzwi.webscraper.web.dto;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Data transfer object for scraping job form data.
 * Contains the fields for creating or updating a scraping job through the web interface.
 *
 * @since 1.0
 */
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

    /**
     * Gets the list of URLs from the form input, splitting by newlines.
     *
     * @return a list of URL strings with empty entries filtered out
     */
    public List<String> urlList() {
        return Arrays.stream(urls.split("\r?\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Converts the form options to an OptionsConfig object for use in scraping operations.
     *
     * @return an OptionsConfig object with the settings from this form
     */
    public OptionsConfig toOptionsConfig() {
        OptionsConfig options = new OptionsConfig();
        options.setStripCss(stripCss);
        options.setStripJs(stripJs);
        options.setRemoveAttributes(removeAttributes);
        options.setExtractHrefsFirst(extractHrefsFirst);
        return options;
    }

    /**
     * Gets the recipe ID as a UUID.
     *
     * @return the recipe ID as a UUID
     * @throws IllegalArgumentException if the recipe ID is not a valid UUID string
     */
    public UUID recipeUuid() {
        return UUID.fromString(recipeId);
    }

    /**
     * Gets the name of the job.
     *
     * @return the job name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the job.
     *
     * @param name the job name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the ID of the recipe to use for this job.
     *
     * @return the recipe ID
     */
    public String getRecipeId() {
        return recipeId;
    }

    /**
     * Sets the ID of the recipe to use for this job.
     *
     * @param recipeId the recipe ID to set
     */
    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    /**
     * Gets the URLs for the job as a multi-line string.
     *
     * @return the URLs string
     */
    public String getUrls() {
        return urls;
    }

    /**
     * Sets the URLs for the job as a multi-line string.
     *
     * @param urls the URLs string to set
     */
    public void setUrls(String urls) {
        this.urls = urls;
    }

    /**
     * Gets the cron expression for scheduling the job.
     *
     * @return the cron expression, or null if not scheduled
     */
    public String getCronExpression() {
        return cronExpression;
    }

    /**
     * Sets the cron expression for scheduling the job.
     *
     * @param cronExpression the cron expression to set
     */
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    /**
     * Checks if CSS should be stripped during scraping.
     *
     * @return true if CSS should be stripped, false otherwise
     */
    public boolean isStripCss() {
        return stripCss;
    }

    /**
     * Sets whether CSS should be stripped during scraping.
     *
     * @param stripCss true to strip CSS, false otherwise
     */
    public void setStripCss(boolean stripCss) {
        this.stripCss = stripCss;
    }

    /**
     * Checks if JavaScript should be stripped during scraping.
     *
     * @return true if JavaScript should be stripped, false otherwise
     */
    public boolean isStripJs() {
        return stripJs;
    }

    /**
     * Sets whether JavaScript should be stripped during scraping.
     *
     * @param stripJs true to strip JavaScript, false otherwise
     */
    public void setStripJs(boolean stripJs) {
        this.stripJs = stripJs;
    }

    /**
     * Checks if HTML attributes should be removed during scraping.
     *
     * @return true if attributes should be removed, false otherwise
     */
    public boolean isRemoveAttributes() {
        return removeAttributes;
    }

    /**
     * Sets whether HTML attributes should be removed during scraping.
     *
     * @param removeAttributes true to remove attributes, false otherwise
     */
    public void setRemoveAttributes(boolean removeAttributes) {
        this.removeAttributes = removeAttributes;
    }

    /**
     * Checks if hrefs should be extracted before other processing.
     *
     * @return true if hrefs should be extracted first, false otherwise
     */
    public boolean isExtractHrefsFirst() {
        return extractHrefsFirst;
    }

    /**
     * Sets whether hrefs should be extracted before other processing.
     *
     * @param extractHrefsFirst true to extract hrefs first, false otherwise
     */
    public void setExtractHrefsFirst(boolean extractHrefsFirst) {
        this.extractHrefsFirst = extractHrefsFirst;
    }
}
