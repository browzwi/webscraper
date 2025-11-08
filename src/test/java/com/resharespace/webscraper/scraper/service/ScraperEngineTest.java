package com.browzwi.webscraper.scraper.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.browzwi.webscraper.scraper.model.DataSourceType;
import com.browzwi.webscraper.scraper.model.FieldConfig;
import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.PageConfig;
import com.browzwi.webscraper.scraper.model.RecipeConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScraperEngineTest {

    private ScraperEngine scraperEngine;

    @BeforeEach
    void setup() {
        HtmlFetcher fetcher = new StubHtmlFetcher("<html><body><h1 data-id=\"123\">Title</h1><a href=\"/foo\">Foo</a><script>var x = 1;</script></body></html>");
        scraperEngine = new ScraperEngine(fetcher, new HtmlProcessingService(), new FieldExtractionService());
    }

    @Test
    void executeReturnsStructuredData() {
        RecipeConfig recipe = buildRecipe();
        var result = scraperEngine.execute(recipe, "https://example.com", overrides());

        assertThat(result.rawHtml()).contains("<h1");
        assertThat(result.processedHtml()).doesNotContain("<script");
        assertThat(result.structuredData()).containsEntry("title", "Title");
        assertThat(result.hrefs()).containsExactly("/foo");
    }

    private RecipeConfig buildRecipe() {
        FieldConfig title = new FieldConfig();
        title.setName("title");
        title.setSelectors(List.of("h1"));
        title.setSource(DataSourceType.TEXT);

        PageConfig page = new PageConfig();
        page.setFields(List.of(title));

        RecipeConfig recipe = new RecipeConfig();
        recipe.setName("Test");
        recipe.setPage(page);
        recipe.setOptions(new OptionsConfig());
        return recipe;
    }

    private OptionsConfig overrides() {
        OptionsConfig overrides = new OptionsConfig();
        overrides.setStripJs(true);
        return overrides;
    }

    private static class StubHtmlFetcher extends HtmlFetcher {
        private final String html;

        private StubHtmlFetcher(String html) {
            super(5000, "JUnit", 3, true);
            this.html = html;
        }

        @Override
        public String fetch(String url) {
            return html;
        }
    }
}
