package com.browzwi.webscraper.scraper.service;

import com.browzwi.webscraper.scraper.model.OptionsConfig;
import com.browzwi.webscraper.scraper.model.PageConfig;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Service
public class HtmlProcessingService {

    private static final List<String> ATTRIBUTE_WHITELIST = List.of("href", "src", "alt", "title");

    public ProcessedHtmlResult process(String rawHtml, OptionsConfig options, PageConfig pageConfig) {
        Document document = Jsoup.parse(rawHtml);
        options = options == null ? new OptionsConfig() : options;

        if (options.isStripCss()) {
            document.select("style, link[rel=stylesheet]").remove();
        }
        if (options.isStripJs()) {
            document.select("script").remove();
        }
        if (options.isRemoveAttributes()) {
            for (Element element : document.getAllElements()) {
                var attributes = new ArrayList<>(element.attributes().asList());
                attributes.forEach(attribute -> {
                    if (!ATTRIBUTE_WHITELIST.contains(attribute.getKey())) {
                        element.removeAttr(attribute.getKey());
                    }
                });
            }
        }

        var hrefSelector = pageConfig != null && pageConfig.getHrefSelector() != null
                ? pageConfig.getHrefSelector()
                : "a[href]";
        List<String> hrefs = document.select(hrefSelector)
                .stream()
                .map(element -> element.attr("href"))
                .distinct()
                .toList();

        document.outputSettings().prettyPrint(false);
        document.outputSettings().outline(false);
        String processed = document.outerHtml();
        return new ProcessedHtmlResult(processed, hrefs, document);
    }

    public record ProcessedHtmlResult(String processedHtml, List<String> hrefs, Document document) {
    }
}
