package com.browzwi.webscraper.scraper.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.springframework.stereotype.Service;

/**
 * Service for converting HTML content to Markdown format.
 * This service uses the Flexmark library to perform the conversion
 * and applies post-processing to clean up the resulting Markdown.
 *
 * @since 1.0
 */
@Service
public class MarkdownConversionService {

    /**
     * Converts HTML content to Markdown format.
     * Applies post-processing to normalize line breaks and clean up the result.
     *
     * @param html the HTML content to convert
     * @return the converted Markdown content, or an empty string if input is null/blank
     */
    public String toMarkdown(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String markdown = FlexmarkHtmlConverter.builder().build().convert(html);
        return markdown.replaceAll("\n{3,}", "\n\n").trim();
    }
}
