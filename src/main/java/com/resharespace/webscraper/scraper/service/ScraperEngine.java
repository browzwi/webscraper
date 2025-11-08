package com.browzwi.webscraper.scraper.service;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import com.browzwi.webscraper.scraper.service.HtmlProcessingService.ProcessedHtmlResult;
import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.SettingsService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ScraperEngine {

    private final HtmlFetcher htmlFetcher;
    private final PlaywrightFetcher playwrightFetcher;
    private final HtmlProcessingService htmlProcessingService;
    private final FieldExtractionService fieldExtractionService;
    private final SettingsService settingsService;

    public ScraperEngine(HtmlFetcher htmlFetcher,
                         PlaywrightFetcher playwrightFetcher,
                         HtmlProcessingService htmlProcessingService,
                         FieldExtractionService fieldExtractionService,
                         SettingsService settingsService) {
        this.htmlFetcher = htmlFetcher;
        this.playwrightFetcher = playwrightFetcher;
        this.htmlProcessingService = htmlProcessingService;
        this.fieldExtractionService = fieldExtractionService;
        this.settingsService = settingsService;
    }

    public ScrapeExecutionResult execute(RecipeConfig recipe, String url, OptionsConfig overrides) {
        if (recipe == null) {
            throw new ScrapeException("Recipe is required");
        }
        try {
            String rawHtml = selectFetcher().fetch(url);
            OptionsConfig effectiveOptions = mergeOptions(recipe.getOptions(), overrides);
            ProcessedHtmlResult processed = htmlProcessingService.process(rawHtml, effectiveOptions, recipe.getPage());
            Map<String, Object> fields = fieldExtractionService.extractFields(processed.document(), recipe.getPage());

            Map<String, Object> structuredData = new HashMap<>(fields);
            structuredData.put("hrefs", processed.hrefs());
            structuredData.put("url", url);
            structuredData.put("timestamp", Instant.now().toString());

            return new ScrapeExecutionResult(rawHtml, processed.processedHtml(), structuredData, processed.hrefs());
        } catch (RuntimeException ex) {
            throw new ScrapeException("Failed to execute scraper", ex);
        }
    }

    private PageFetcher selectFetcher() {
        return settingsService.getFetcherType() == ScrapeFetcherType.PLAYWRIGHT
                ? playwrightFetcher
                : htmlFetcher;
    }

    private OptionsConfig mergeOptions(OptionsConfig base, OptionsConfig overrides) {
        OptionsConfig merged = new OptionsConfig();
        if (base != null) {
            merged.setStripCss(base.isStripCss());
            merged.setStripJs(base.isStripJs());
            merged.setRemoveAttributes(base.isRemoveAttributes());
            merged.setExtractHrefsFirst(base.isExtractHrefsFirst());
        }
        if (overrides != null) {
            merged.setStripCss(overrides.isStripCss() || merged.isStripCss());
            merged.setStripJs(overrides.isStripJs() || merged.isStripJs());
            merged.setRemoveAttributes(overrides.isRemoveAttributes() || merged.isRemoveAttributes());
            merged.setExtractHrefsFirst(overrides.isExtractHrefsFirst() || merged.isExtractHrefsFirst());
        }
        return merged;
    }

    public record ScrapeExecutionResult(String rawHtml,
                                        String processedHtml,
                                        Map<String, Object> structuredData,
                                        List<String> hrefs) {
    }

    public static class ScrapeException extends RuntimeException {
        public ScrapeException(String message) {
            super(message);
        }

        public ScrapeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
