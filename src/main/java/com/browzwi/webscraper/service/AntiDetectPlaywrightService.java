package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Playwright service with anti-detection techniques for Google search.
 * Uses human-like behavior to avoid bot detection.
 */
@Service
public class AntiDetectPlaywrightService {

    private static final Logger log = LoggerFactory.getLogger(AntiDetectPlaywrightService.class);
    private static final Random random = new Random();

    /**
     * Searches Google using Playwright with anti-detection techniques.
     */
    public List<DiscoveredBusiness> searchGoogle(String keyword, String location) {
        List<DiscoveredBusiness> businesses = new ArrayList<>();
        String query = keyword + " in " + location;

        try (Playwright playwright = Playwright.create();
             Browser browser = launchBrowser(playwright);
             BrowserContext context = createStealthContext(browser);
             Page page = context.newPage()) {

            // Navigate to Google
            log.info("Navigating to Google...");
            page.navigate("https://www.google.com", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            humanDelay(2000, 4000);

            // Accept cookies if dialog appears
            try {
                page.click("button#L2AGLb");
                humanDelay(1000, 2000);
            } catch (Exception e) {
                // No cookie dialog
            }

            // Type search query with human-like typing
            log.info("Typing search query: {}", query);
            Locator searchBox = page.locator("textarea[name='q']").first();
            humanType(searchBox, query);

            // Press Enter
            searchBox.press("Enter");
            humanDelay(3000, 5000);

            // Check if Google blocked us
            String url = page.url();
            if (url.contains("sorry")) {
                log.warn("Google blocked the request (CAPTCHA)");
                throw new RuntimeException("Google detected automated browsing. Try again later or use Brave Search API.");
            }

            // Extract search results
            List<Locator> results = page.locator("div.g").all();
            log.info("Found {} search results", results.size());

            int count = 0;
            for (Locator result : results) {
                if (count >= 15) break;

                try {
                    Locator titleElement = result.locator("h3").first();
                    Locator linkElement = result.locator("a").first();
                    Locator snippetElement = result.locator("div.VwiC3b").first();

                    if (!titleElement.isVisible()) continue;

                    String title = titleElement.textContent();
                    String urlResult = linkElement.getAttribute("href");
                    String snippet = snippetElement.textContent();

                    if (title != null && !title.isBlank() && urlResult != null && !urlResult.isBlank()) {
                        // Skip Google's own links
                        if (urlResult.contains("google.com") || urlResult.contains("youtube.com")) {
                            continue;
                        }

                        DiscoveredBusiness business = new DiscoveredBusiness();
                        business.setBusinessName(cleanTitle(title, keyword));
                        business.setWebsiteUrl(urlResult);
                        business.setAddress(location);
                        business.setPhoneNumber(extractPhone(snippet));
                        business.setEmailAddress(extractEmail(snippet));
                        business.setSocialMediaLinks("");
                        business.setStatus("DISCOVERED");

                        businesses.add(business);
                        log.info("Found: {} - {}", business.getBusinessName(), urlResult);

                        count++;

                        // Human-like delay between extractions
                        if (count % 5 == 0) {
                            humanDelay(2000, 4000);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parsing result: {}", e.getMessage());
                }
            }

            log.info("Successfully extracted {} businesses", businesses.size());

        } catch (Exception e) {
            log.error("Playwright search failed: {}", e.getMessage());
            throw new RuntimeException("Search failed: " + e.getMessage());
        }

        return businesses;
    }

    /**
     * Launches browser with anti-detection settings.
     */
    private Browser launchBrowser(Playwright playwright) {
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
            .setHeadless(false) // Use visible browser (less suspicious)
            .setArgs(List.of(
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
                "--no-sandbox",
                "--disable-setuid-sandbox",
                "--disable-web-security",
                "--disable-features=IsolateOrigins,site-per-process"
            ))
        );
    }

    /**
     * Creates browser context with stealth settings.
     */
    private BrowserContext createStealthContext(Browser browser) {
        return browser.newContext(new Browser.NewContextOptions()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setViewportSize(null)
            .setJavaScriptEnabled(true)
        );
    }

    /**
     * Types text with human-like delays between keystrokes.
     */
    private void humanType(Locator element, String text) {
        for (char c : text.toCharArray()) {
            element.press(String.valueOf(c));
            humanDelay(50, 200); // Random delay between keystrokes
        }
    }

    /**
     * Random delay to simulate human behavior.
     */
    private void humanDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + random.nextInt(maxMs - minMs);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractPhone(String text) {
        if (text == null) return "";
        
        String[] patterns = {
            "\\+?63\\s?\\d{10}",
            "\\+?63\\s?\\d{3}\\s?\\d{3}\\s?\\d{4}",
            "09\\d{9}",
            "\\(0\\d{2}\\)\\s?\\d{3}\\s?\\d{4}"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return "";
    }

    private String extractEmail(String text) {
        if (text == null) return "";
        
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        ).matcher(text);
        
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }

    private String cleanTitle(String title, String keyword) {
        if (title == null || title.isBlank()) return "Unknown Business";
        
        String cleaned = title.replaceAll(" - .*", "")
                              .replaceAll(" \\| .*", "")
                              .replaceAll(": .*", "")
                              .trim();
        
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }
}
