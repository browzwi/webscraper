package com.browzwi.webscraper.scraper.service;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import java.io.IOException;
import java.net.URL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of PageFetcher that uses HtmlUnit to fetch and render HTML content.
 * This fetcher supports JavaScript execution and dynamic content loading.
 *
 * @since 1.0
 */
@Service
public class HtmlFetcher implements PageFetcher {

    private final int timeoutMillis;
    private final String userAgent;
    private final int maxRedirects;

    /**
     * Constructor for HtmlFetcher with configuration parameters.
     *
     * @param timeoutMillis the timeout in milliseconds for page loading
     * @param userAgent the user agent string to use for requests
     * @param maxRedirects the maximum number of redirects to follow
     */
    @org.springframework.beans.factory.annotation.Autowired
    public HtmlFetcher(
            @Value("${webscraper.fetcher.timeout:15000}") int timeoutMillis,
            @Value("${webscraper.fetcher.user-agent:WebScraperBot/1.0}") String userAgent,
            @Value("${webscraper.fetcher.max-redirects:5}") int maxRedirects) {
        this.timeoutMillis = timeoutMillis;
        this.userAgent = userAgent;
        this.maxRedirects = maxRedirects;
    }

    /**
     * Constructor for testing purposes.
     *
     * @param timeoutMillis the timeout in milliseconds for page loading
     * @param userAgent the user agent string to use for requests
     * @param maxRedirects the maximum number of redirects to follow
     * @param testing flag to indicate testing mode
     */
    protected HtmlFetcher(int timeoutMillis, String userAgent, int maxRedirects, boolean testing) {
        this.timeoutMillis = timeoutMillis;
        this.userAgent = userAgent;
        this.maxRedirects = maxRedirects;
    }

    @Override
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

    /**
     * Builds and configures a WebClient instance for fetching pages.
     *
     * @return a configured WebClient instance
     */
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

    /**
     * Exception thrown when HTML fetching fails.
     *
     * @since 1.0
     */
    public static class HtmlFetchException extends RuntimeException {
        /**
         * Constructs an exception with the specified detail message and cause.
         *
         * @param message the detail message
         * @param cause the cause of the exception
         */
        public HtmlFetchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
