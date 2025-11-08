package com.browzwi.webscraper.scraper.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PlaywrightFetcher implements PageFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightFetcher.class);
    private final boolean headless;
    private final int timeoutMillis;
    private final String browser;

    @Autowired
    public PlaywrightFetcher(@Value("${webscraper.fetcher.playwright.headless:true}") boolean headless,
                             @Value("${webscraper.fetcher.playwright.timeout:20000}") int timeoutMillis,
                             @Value("${webscraper.fetcher.playwright.browser:chromium}") String browser) {
        this.headless = headless;
        this.timeoutMillis = timeoutMillis;
        this.browser = browser;
    }

    protected PlaywrightFetcher(boolean headless, int timeoutMillis, String browser, boolean testing) {
        this.headless = headless;
        this.timeoutMillis = timeoutMillis;
        this.browser = browser;
    }

    @Override
    public String fetch(String url) {
        try (Playwright playwright = Playwright.create();
             Browser browserInstance = selectBrowser(playwright);
             BrowserContext context = browserInstance.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
             Page page = context.newPage()) {

            page.setDefaultTimeout(timeoutMillis);
            page.navigate(url, new Page.NavigateOptions().setTimeout((double) timeoutMillis));
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout((double) timeoutMillis));
            return page.content();
        } catch (PlaywrightException e) {
            throw new RuntimeException("Playwright fetch failed for " + url, e);
        }
    }

    private Browser selectBrowser(Playwright playwright) {
        BrowserType type = switch (browser.toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
        return type.launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }
}
