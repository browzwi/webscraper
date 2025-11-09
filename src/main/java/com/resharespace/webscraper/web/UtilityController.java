package com.browzwi.webscraper.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * Controller for utility endpoints that provide common formatting and processing functions.
 * Currently provides a service to beautify HTML content for display in the UI.
 *
 * @since 1.0
 */
@RestController
public class UtilityController {

    /**
     * Beautifies the provided HTML content and returns it wrapped in a pre tag for display.
     * The HTML is parsed, formatted with proper indentation, escaped for safety, and wrapped
     * in a pre tag with the specified target ID.
     *
     * @param html the HTML content to beautify
     * @param targetId the ID to assign to the output pre tag
     * @return a ResponseEntity containing the beautified HTML wrapped in a pre tag
     */
    @PostMapping(value = "/utils/beautify", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> beautify(@RequestParam("html") String html,
                                           @RequestParam("targetId") String targetId) {
        Document document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(true).indentAmount(2);
        String prettified = document.outerHtml();
        String escaped = HtmlUtils.htmlEscape(prettified);
        String pre = "<pre id='" + HtmlUtils.htmlEscape(targetId) + "' class=\"mt-2 bg-slate-900 text-indigo-100 text-xs rounded-lg p-3 overflow-auto\">"
                + escaped + "</pre>";
        return ResponseEntity.ok(pre);
    }
}
