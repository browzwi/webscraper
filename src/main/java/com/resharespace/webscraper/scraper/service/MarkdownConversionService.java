package com.browzwi.webscraper.scraper.service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarkdownConversionService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownConversionService.class);
    private final HtmlParser parser = new HtmlParser();

    public String toMarkdown(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        try {
            parser.parse(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)), handler, metadata, new ParseContext());
            // Tika gives plain text; treat as Markdown-friendly text.
            return handler.toString();
        } catch (Exception e) {
            log.warn("Unable to convert HTML to Markdown", e);
            return html;
        }
    }
}
