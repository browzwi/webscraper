package com.browzwi.webscraper.scraper.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.springframework.stereotype.Service;

@Service
public class MarkdownConversionService {

    public String toMarkdown(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String markdown = FlexmarkHtmlConverter.builder().build().convert(html);
        return markdown.replaceAll("\n{3,}", "\n\n").trim();
    }
}
