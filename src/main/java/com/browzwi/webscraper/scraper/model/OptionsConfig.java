package com.browzwi.webscraper.scraper.model;

/**
 * Configuration options for customizing the scraping and HTML processing behavior.
 * These options control how HTML content is processed after retrieval from web pages.
 *
 * @since 1.0
 */
public class OptionsConfig {

    private boolean stripCss;
    private boolean stripJs;
    private boolean removeAttributes;
    private boolean extractHrefsFirst;

    /**
     * Checks if CSS should be stripped from the HTML content.
     *
     * @return true if CSS should be removed, false otherwise
     */
    public boolean isStripCss() {
        return stripCss;
    }

    /**
     * Sets whether CSS should be stripped from the HTML content.
     *
     * @param stripCss true to remove CSS, false to keep it
     */
    public void setStripCss(boolean stripCss) {
        this.stripCss = stripCss;
    }

    /**
     * Checks if JavaScript should be stripped from the HTML content.
     *
     * @return true if JavaScript should be removed, false otherwise
     */
    public boolean isStripJs() {
        return stripJs;
    }

    /**
     * Sets whether JavaScript should be stripped from the HTML content.
     *
     * @param stripJs true to remove JavaScript, false to keep it
     */
    public void setStripJs(boolean stripJs) {
        this.stripJs = stripJs;
    }

    /**
     * Checks if HTML attributes should be removed from elements.
     *
     * @return true if attributes should be removed, false otherwise
     */
    public boolean isRemoveAttributes() {
        return removeAttributes;
    }

    /**
     * Sets whether HTML attributes should be removed from elements.
     *
     * @param removeAttributes true to remove attributes, false to keep them
     */
    public void setRemoveAttributes(boolean removeAttributes) {
        this.removeAttributes = removeAttributes;
    }

    /**
     * Checks if hrefs should be extracted before other processing steps.
     *
     * @return true if hrefs should be extracted first, false otherwise
     */
    public boolean isExtractHrefsFirst() {
        return extractHrefsFirst;
    }

    /**
     * Sets whether hrefs should be extracted before other processing steps.
     *
     * @param extractHrefsFirst true to extract hrefs first, false otherwise
     */
    public void setExtractHrefsFirst(boolean extractHrefsFirst) {
        this.extractHrefsFirst = extractHrefsFirst;
    }
}
