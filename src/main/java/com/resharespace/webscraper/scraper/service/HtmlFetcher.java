package com.browzwi.webscraper.scraper.service;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HtmlFetcher {

    private final int timeoutMillis;
    private final String userAgent;
    private final int maxRedirects;

    @org.springframework.beans.factory.annotation.Autowired
    public HtmlFetcher(
            @Value("${webscraper.fetcher.timeout:15000}") int timeoutMillis,
            @Value("${webscraper.fetcher.user-agent:WebScraperBot/1.0}") String userAgent,
            @Value("${webscraper.fetcher.max-redirects:5}") int maxRedirects) {
        this.timeoutMillis = timeoutMillis;
        this.userAgent = userAgent;
        this.maxRedirects = maxRedirects;
    }

    protected HtmlFetcher(int timeoutMillis, String userAgent, int maxRedirects, boolean testing) {
        this.timeoutMillis = timeoutMillis;
        this.userAgent = userAgent;
        this.maxRedirects = maxRedirects;
    }

    public String fetch(String url) {
        try (WebClient client = buildClient()) {
            WebRequest request = new WebRequest(new URL(url));
            request.setAdditionalHeader("User-Agent", userAgent);
            HtmlPage page = client.getPage(request);
            client.waitForBackgroundJavaScriptStartingBefore(1000);
            client.waitForBackgroundJavaScript(timeoutMillis);
            return page.asXml();
        } catch (IOException ex) {
            throw new HtmlFetchException("Failed to fetch %s".formatted(url), ex);
        }
    }

    private WebClient buildClient() {
        WebClient client = new WebClient(BrowserVersion.BEST_SUPPORTED);
        client.getOptions().setCssEnabled(true);
        client.getOptions().setJavaScriptEnabled(true);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setTimeout(timeoutMillis);
        client.getOptions().setMaxInMemory(10_000_000);
        client.getOptions().setRedirectEnabled(maxRedirects > 0);
        return client;
    }

    public static class HtmlFetchException extends RuntimeException {
        public HtmlFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
