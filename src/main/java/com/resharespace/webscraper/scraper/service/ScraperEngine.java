package com.browzwi.webscraper.scraper.service;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import com.browzwi.webscraper.scraper.service.HtmlProcessingService.ProcessedHtmlResult;
import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.SettingsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ScraperEngine {

    private static final Logger log = LoggerFactory.getLogger(ScraperEngine.class);

    private final HtmlFetcher htmlFetcher;
    private final PlaywrightFetcher playwrightFetcher;
    private final HtmlProcessingService htmlProcessingService;
    private final FieldExtractionService fieldExtractionService;
    private final SettingsService settingsService;
    private final MarkdownConversionService markdownConversionService;

    public ScraperEngine(HtmlFetcher htmlFetcher,
                         PlaywrightFetcher playwrightFetcher,
                         HtmlProcessingService htmlProcessingService,
                         FieldExtractionService fieldExtractionService,
                         SettingsService settingsService,
                         MarkdownConversionService markdownConversionService) {
        this.htmlFetcher = htmlFetcher;
        this.playwrightFetcher = playwrightFetcher;
        this.htmlProcessingService = htmlProcessingService;
        this.fieldExtractionService = fieldExtractionService;
        this.settingsService = settingsService;
        this.markdownConversionService = markdownConversionService;
    }

    public ScrapeExecutionResult execute(RecipeConfig recipe, String url, OptionsConfig overrides) {
        if (recipe == null) {
            throw new ScrapeException("Recipe is required");
        }
        List<String> progress = new ArrayList<>();
        try {
            progress.add("Starting scrape for " + url);
            PageFetcher fetcher = selectFetcher();
            log.info("[ScraperEngine] Using {} fetcher for {}", fetcher.getClass().getSimpleName(), url);
            String rawHtml = fetcher.fetch(url);
            progress.add("Fetched raw HTML");
            OptionsConfig effectiveOptions = mergeOptions(recipe.getOptions(), overrides);
            ProcessedHtmlResult processed = htmlProcessingService.process(rawHtml, effectiveOptions, recipe.getPage());
            progress.add("Processed DOM and collected hrefs");
            Map<String, Object> fields = fieldExtractionService.extractFields(processed.document(), recipe.getPage());
            progress.add("Extracted structured data");

            Map<String, Object> structuredData = new HashMap<>(fields);
            structuredData.put("hrefs", processed.hrefs());
            structuredData.put("url", url);
            structuredData.put("timestamp", Instant.now().toString());

            String markdown = markdownConversionService.toMarkdown(processed.processedHtml());
            progress.add("Converted processed HTML to Markdown");

            return new ScrapeExecutionResult(rawHtml, processed.processedHtml(), markdown, structuredData, processed.hrefs(), progress);
        } catch (RuntimeException ex) {
            progress.add("Failed: " + ex.getMessage());
            log.error("[ScraperEngine] Scrape failed for {}", url, ex);
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
                                        String processedMarkdown,
                                        Map<String, Object> structuredData,
                                        List<String> hrefs,
                                        List<String> progressSteps) {
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
