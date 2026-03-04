package com.browzwi.webscraper.scraper.service;

import com.browzwi.webscraper.scraper.model.*;
import com.browzwi.webscraper.service.settings.ScrapeFetcherType;
import com.browzwi.webscraper.service.settings.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiPageScraperEngineTest {

    @Mock
    private HtmlFetcher htmlFetcher;
    @Mock
    private PlaywrightFetcher playwrightFetcher;
    @Mock
    private HtmlProcessingService htmlProcessingService;
    @Mock
    private FieldExtractionService fieldExtractionService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private MarkdownConversionService markdownConversionService;

    private ScraperEngine scraperEngine;

    @BeforeEach
    void setUp() {
        scraperEngine = new ScraperEngine(
                htmlFetcher, playwrightFetcher, htmlProcessingService,
                fieldExtractionService, settingsService, markdownConversionService
        );
        when(settingsService.getFetcherType()).thenReturn(ScrapeFetcherType.HTMLUNIT);
    }

    @Test
    void testMultiPageScraping() {
        // Setup recipe with sub-pages
        RecipeConfig recipe = createMultiPageRecipe();
        
        // Mock main page response
        String mainHtml = "<html><body><h1>Main Page</h1><a href='https://example.com/about'>About</a></body></html>";
        String aboutHtml = "<html><body><h2>About Page</h2><p>Company info</p></body></html>";
        
        when(htmlFetcher.fetch("https://example.com")).thenReturn(mainHtml);
        when(htmlFetcher.fetch("https://example.com/about")).thenReturn(aboutHtml);
        
        // Mock processing service
        when(htmlProcessingService.process(anyString(), any(OptionsConfig.class), any(PageConfig.class)))
                .thenReturn(new HtmlProcessingService.ProcessedHtmlResult(
                        mainHtml, List.of("https://example.com/about"), org.jsoup.Jsoup.parse(mainHtml)
                ));
        
        // Mock field extraction
        when(fieldExtractionService.extractFields(any(), any()))
                .thenReturn(Map.of("title", "Main Page"));
        when(fieldExtractionService.extractFieldsFromConfig(any(), anyList()))
                .thenReturn(Map.of("about_info", "Company info"));
        
        // Mock markdown conversion
        when(markdownConversionService.toMarkdown(anyString())).thenReturn("# Main Page");
        
        // Execute multi-page scraping
        MultiPageScrapeResult result = scraperEngine.executeMultiPage(
                recipe, "https://example.com", new OptionsConfig(), ScraperEngine.ProgressListener.noop()
        );
        
        // Verify results
        assertNotNull(result);
        assertNotNull(result.combinedStructuredData());
        assertNotNull(result.pageResults());
        assertTrue(result.pageResults().containsKey("main"));
        assertTrue(result.pageResults().containsKey("about"));
        
        // Verify combined data contains fields from both pages
        assertTrue(result.combinedStructuredData().containsKey("title"));
        assertTrue(result.combinedStructuredData().containsKey("about_info"));
    }

    private RecipeConfig createMultiPageRecipe() {
        RecipeConfig recipe = new RecipeConfig();
        recipe.setName("Multi-page Test");
        
        MatchConfig match = new MatchConfig();
        match.setUrlRegexes(List.of("https://example.com.*"));
        recipe.setMatch(match);
        
        PageConfig page = new PageConfig();
        page.setHrefSelector("a[href*='example.com']");
        
        // Main page fields
        FieldConfig titleField = new FieldConfig();
        titleField.setName("title");
        titleField.setSelectors(List.of("h1"));
        titleField.setSource(DataSourceType.TEXT);
        page.setFields(List.of(titleField));
        
        // Sub-page configuration
        SubPageConfig aboutPage = new SubPageConfig();
        aboutPage.setPath("/about");
        
        FieldConfig aboutField = new FieldConfig();
        aboutField.setName("about_info");
        aboutField.setSelectors(List.of("p"));
        aboutField.setSource(DataSourceType.TEXT);
        aboutPage.setFields(List.of(aboutField));
        
        page.setSubPages(List.of(aboutPage));
        recipe.setPage(page);
        
        recipe.setOptions(new OptionsConfig());
        
        return recipe;
    }
}
