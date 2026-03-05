package com.browzwi.webscraper.scraper.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarkdownConversionServiceTest {

    private MarkdownConversionService service;

    @BeforeEach
    void setup() {
        service = new MarkdownConversionService();
    }

    @Test
    void convertsProcessedHtmlToMarkdown() throws IOException {
        String html = Files.readString(Path.of("src/test/resources/test-processed-html.html"));
        String markdown = service.toMarkdown(html);

        assertThat(markdown).isNotBlank();
        assertThat(markdown).doesNotContain("<div");
    }
}
