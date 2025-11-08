package com.browzwi.webscraper.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class UtilityController {

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
