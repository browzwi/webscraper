package com.browzwi.webscraper.scraper.model;

public class OptionsConfig {

    private boolean stripCss;
    private boolean stripJs;
    private boolean removeAttributes;
    private boolean extractHrefsFirst;

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
