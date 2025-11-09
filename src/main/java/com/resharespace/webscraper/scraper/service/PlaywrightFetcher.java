package com.browzwi.webscraper.scraper.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of PageFetcher that uses Playwright to fetch and render HTML content.
 * This fetcher provides full browser capabilities with JavaScript execution and
 * dynamic content loading support.
 *
 * @since 1.0
 */
@Service
public class PlaywrightFetcher implements PageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightFetcher.class);
    private final boolean headless;
    private final int timeoutMillis;
    private final String browser;

    /**
     * Constructor for PlaywrightFetcher with configuration parameters.
     *
     * @param headless whether to run the browser in headless mode
     * @param timeoutMillis the timeout in milliseconds for page loading
     * @param browser the browser type to use (chromium, firefox, or webkit)
     */
    @Autowired
    public PlaywrightFetcher(@Value("${webscraper.fetcher.playwright.headless:true}") boolean headless,
                             @Value("${webscraper.fetcher.playwright.timeout:20000}") int timeoutMillis,
                             @Value("${webscraper.fetcher.playwright.browser:chromium}") String browser) {
        this.headless = headless;
        this.timeoutMillis = timeoutMillis;
        this.browser = browser;
    }

    /**
     * Constructor for testing purposes.
     *
     * @param headless whether to run the browser in headless mode
     * @param timeoutMillis the timeout in milliseconds for page loading
     * @param browser the browser type to use (chromium, firefox, or webkit)
     * @param testing flag to indicate testing mode
     */
    protected PlaywrightFetcher(boolean headless, int timeoutMillis, String browser, boolean testing) {
        this.headless = headless;
        this.timeoutMillis = timeoutMillis;
        this.browser = browser;
    }

    @Override
    public String fetch(String url) {
        try (Playwright playwright = Playwright.create();
             Browser browserInstance = selectBrowser(playwright)) {

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setIgnoreHTTPSErrors(true)
                    .setLocale("en-US")
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
            try (BrowserContext context = browserInstance.newContext(contextOptions)) {
                context.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US,en;q=0.9"));
                try (Page page = context.newPage()) {
                    page.setDefaultTimeout(timeoutMillis);
                    page.navigate(url, new Page.NavigateOptions().setTimeout((double) timeoutMillis));
                    page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout((double) timeoutMillis));
                    return page.content();
                }
            }
        } catch (PlaywrightException e) {
            throw new RuntimeException("Playwright fetch failed for " + url, e);
        }
    }

    /**
     * Selects and launches the appropriate browser based on configuration.
     *
     * @param playwright the Playwright instance to use
     * @return the launched browser instance
     */
    private Browser selectBrowser(Playwright playwright) {
        BrowserType type = switch (browser.toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        return type.launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }
}
