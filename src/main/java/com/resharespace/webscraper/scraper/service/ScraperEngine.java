package com.browzwi.webscraper.scraper.service;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import com.browzwi.webscraper.scraper.model.SubPageConfig;
import com.browzwi.webscraper.scraper.service.HtmlProcessingService.ProcessedHtmlResult;
import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.SettingsService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        return execute(recipe, url, overrides, ProgressListener.noop());
    }

    public ScrapeExecutionResult execute(RecipeConfig recipe,
                                         String url,
                                         OptionsConfig overrides,
                                         ProgressListener progressListener) {
        if (recipe == null) {
            throw new ScrapeException("Recipe is required");
        }
        ProgressListener listener = progressListener == null ? ProgressListener.noop() : progressListener;
        List<String> progress = new ArrayList<>();
        String currentStep = null;
        try {
            currentStep = "Starting scrape for " + url;
            listener.onStepStarted(currentStep);
            progress.add(currentStep);
            checkCancellation(listener);
            PageFetcher fetcher = selectFetcher();
            listener.onStepCompleted(currentStep);

            currentStep = "Fetched raw HTML";
            listener.onStepStarted(currentStep);
            progress.add(currentStep);
            checkCancellation(listener);
            log.info("[ScraperEngine] Using {} fetcher for {}", fetcher.getClass().getSimpleName(), url);
            String rawHtml = fetcher.fetch(url);
            Document rawDocument = Jsoup.parse(rawHtml);
            listener.onStepCompleted(currentStep);

            OptionsConfig effectiveOptions = mergeOptions(recipe.getOptions(), overrides);

            currentStep = "Processed DOM and collected hrefs";
            listener.onStepStarted(currentStep);
            progress.add(currentStep);
            checkCancellation(listener);
            ProcessedHtmlResult processed = htmlProcessingService.process(rawHtml, effectiveOptions, recipe.getPage());
            listener.onStepCompleted(currentStep);

            currentStep = "Extracted structured data";
            listener.onStepStarted(currentStep);
            progress.add(currentStep);
            checkCancellation(listener);
            Map<String, Object> fields = fieldExtractionService.extractFields(rawDocument, recipe.getPage());
            listener.onStepCompleted(currentStep);

            Map<String, Object> structuredData = new HashMap<>(fields);
            structuredData.put("hrefs", processed.hrefs());
            structuredData.put("url", url);
            structuredData.put("timestamp", Instant.now().toString());

            currentStep = "Converted processed HTML to Markdown";
            listener.onStepStarted(currentStep);
            progress.add(currentStep);
            checkCancellation(listener);
            String markdown = markdownConversionService.toMarkdown(processed.processedHtml());
            listener.onStepCompleted(currentStep);

            return new ScrapeExecutionResult(rawHtml, processed.processedHtml(), markdown, structuredData, processed.hrefs(), progress);
        } catch (ScrapeCancelledException ex) {
            listener.onCancelled(currentStep);
            log.info("[ScraperEngine] Scrape cancelled for {}", url);
            throw ex;
        } catch (RuntimeException ex) {
            if (currentStep != null) {
                listener.onStepFailed(currentStep, ex.getMessage());
            }
            progress.add("Failed: " + ex.getMessage());
            log.error("[ScraperEngine] Scrape failed for {}", url, ex);
            throw new ScrapeException("Failed to execute scraper", ex);
        }
    }

    public MultiPageScrapeResult executeMultiPage(RecipeConfig recipe,
                                                  String seedUrl,
                                                  OptionsConfig overrides,
                                                  ProgressListener progressListener) {
        if (recipe == null) {
            throw new ScrapeException("Recipe is required");
        }
        
        ProgressListener listener = progressListener == null ? ProgressListener.noop() : progressListener;
        List<String> progress = new ArrayList<>();
        Map<String, MultiPageScrapeResult.PageResult> pageResults = new HashMap<>();
        Map<String, Object> combinedData = new HashMap<>();
        
        try {
            // Scrape main page
            String currentStep = "Scraping main page: " + seedUrl;
            listener.onStepStarted(currentStep);
            progress.add(currentStep);
            
            ScrapeExecutionResult mainResult;
            try {
                mainResult = execute(recipe, seedUrl, overrides, listener);
            } catch (Exception e) {
                log.error("[ScraperEngine] Failed to scrape main page: {}", seedUrl, e);
                throw new ScrapeException("Failed to scrape main page: " + e.getMessage(), e);
            }
            
            pageResults.put("main", new MultiPageScrapeResult.PageResult(
                seedUrl, mainResult.rawHtml(), mainResult.processedHtml(), 
                mainResult.processedMarkdown(), mainResult.structuredData(), mainResult.hrefs()
            ));
            
            // Add main page fields to combined data
            combinedData.putAll(mainResult.structuredData());
            listener.onStepCompleted(currentStep);
            
            // Process sub-pages if configured
            if (recipe.getPage().getSubPages() != null && !recipe.getPage().getSubPages().isEmpty()) {
                for (SubPageConfig subPage : recipe.getPage().getSubPages()) {
                    String subPageUrl = buildSubPageUrl(seedUrl, subPage.getPath());
                    currentStep = "Scraping sub-page: " + subPageUrl;
                    listener.onStepStarted(currentStep);
                    progress.add(currentStep);
                    
                    try {
                        ScrapeExecutionResult subResult = scrapeSubPage(recipe, subPageUrl, subPage, overrides, listener);
                        String pageKey = subPage.getPath().startsWith("/") ? subPage.getPath().substring(1) : subPage.getPath();
                        pageResults.put(pageKey, new MultiPageScrapeResult.PageResult(
                            subPageUrl, subResult.rawHtml(), subResult.processedHtml(),
                            subResult.processedMarkdown(), subResult.structuredData(), subResult.hrefs()
                        ));
                        
                        // Merge sub-page fields into combined data
                        combinedData.putAll(subResult.structuredData());
                        listener.onStepCompleted(currentStep);
                    } catch (Exception e) {
                        listener.onStepFailed(currentStep, e.getMessage());
                        progress.add("Failed sub-page: " + subPageUrl + " - " + e.getMessage());
                        log.error("[ScraperEngine] Failed to scrape sub-page: {}", subPageUrl, e);
                    }
                }
            }
            
            return new MultiPageScrapeResult(combinedData, pageResults, progress);
            
        } catch (ScrapeException ex) {
            throw ex; // Re-throw ScrapeException as-is
        } catch (Exception ex) {
            progress.add("Failed: " + ex.getMessage());
            log.error("[ScraperEngine] Multi-page scrape failed for {}", seedUrl, ex);
            throw new ScrapeException("Failed to execute multi-page scraper: " + ex.getMessage(), ex);
        }
    }

    private String buildSubPageUrl(String seedUrl, String path) {
        if (path == null || path.trim().isEmpty()) {
            return seedUrl;
        }
        
        String cleanSeedUrl = seedUrl.endsWith("/") ? seedUrl.substring(0, seedUrl.length() - 1) : seedUrl;
        String cleanPath = path.startsWith("/") ? path : "/" + path;
        
        return cleanSeedUrl + cleanPath;
    }

    private ScrapeExecutionResult scrapeSubPage(RecipeConfig recipe, String url, SubPageConfig subPage, 
                                               OptionsConfig overrides, ProgressListener listener) {
        PageFetcher fetcher = selectFetcher();
        String rawHtml = fetcher.fetch(url);
        Document document = Jsoup.parse(rawHtml);
        
        OptionsConfig effectiveOptions = mergeOptions(recipe.getOptions(), overrides);
        ProcessedHtmlResult processed = htmlProcessingService.process(rawHtml, effectiveOptions, recipe.getPage());
        
        // Extract fields using sub-page specific configuration
        Map<String, Object> fields = fieldExtractionService.extractFieldsFromConfig(document, subPage.getFields());
        
        Map<String, Object> structuredData = new HashMap<>(fields);
        structuredData.put("url", url);
        structuredData.put("timestamp", Instant.now().toString());
        
        String markdown = markdownConversionService.toMarkdown(processed.processedHtml());
        
        return new ScrapeExecutionResult(rawHtml, processed.processedHtml(), markdown, 
                                       structuredData, processed.hrefs(), List.of());
    }

    private void checkCancellation(ProgressListener listener) {
        if (listener.isCancelled()) {
            throw new ScrapeCancelledException("Scrape was cancelled");
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

    public interface ProgressListener {

        default void onStepStarted(String step) {
        }

        default void onStepCompleted(String step) {
        }

        default void onStepFailed(String step, String message) {
        }

        default boolean isCancelled() {
            return false;
        }

        default void onCancelled(String currentStep) {
        }

        static ProgressListener noop() {
            return new ProgressListener() {
            };
        }
    }

    public static class ScrapeCancelledException extends RuntimeException {
        public ScrapeCancelledException(String message) {
            super(message);
        }
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
