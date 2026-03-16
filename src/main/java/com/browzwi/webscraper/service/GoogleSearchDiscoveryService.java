package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.browzwi.webscraper.service.settings.TargetPageType;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers target URLs (Facebook, Website, Instagram, etc.) via Google Search.
 * Uses Playwright to search Google for specific page types based on business names.
 */
@Service
public class GoogleSearchDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSearchDiscoveryService.class);
    private static final Random random = new Random();

    public GoogleSearchDiscoveryService() {
    }

    /**
     * Discovers the target URL for a specific business based on page type.
     *
     * @param businessName The business name from Google Maps
     * @param location The location to narrow search results
     * @param pageType The type of page to find (Facebook, Website, Instagram, etc.)
     * @param customPattern Optional custom search pattern (only for CUSTOM type)
     * @return Target URL or null if not found
     */
    public String discoverTargetUrl(String businessName, String location, TargetPageType pageType, String customPattern) {
        String query = pageType.buildSearchQuery(businessName, location, customPattern);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String targetDomain = pageType.getTargetDomain();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)  // Run visible browser
                .setArgs(java.util.List.of(
                    "--disable-blink-features=AutomationControlled",
                    "--disable-dev-shm-usage",
                    "--no-sandbox"
                ))
            );
            
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .setExtraHTTPHeaders(java.util.Map.of(
                    "Accept-Language", "en-US,en;q=0.9",
                    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
                ))
            );

            Page page = context.newPage();
            
            page.addInitScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});" +
                "Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});" +
                "Object.defineProperty(navigator, 'platform', {get: () => 'Win32'});" +
                "window.chrome = { runtime: {} };" +
                "Object.defineProperty(navigator, 'permissions', {get: () => ({query: () => Promise.resolve({state: 'granted'})})});"
            );

            String searchUrl = "https://www.google.com/search?q=" + encodedQuery + "&hl=en";
            log.info("Searching: {} (target: {})", query, pageType.getDisplayName());
            
            page.navigate(searchUrl, new Page.NavigateOptions().setTimeout(30000));
            
            // Wait for results
            try {
                page.waitForSelector("div#search", new Page.WaitForSelectorOptions().setTimeout(10000));
            } catch (Exception e) {
                log.error("Failed to load search results. Google may be blocking requests. URL: {}", searchUrl);
                log.error("Page title: {}", page.title());
                return null;
            }
            
            // Longer delay to appear human (30-40 seconds)
            Thread.sleep(30000 + random.nextInt(10000));

            // Find result blocks
            var resultBlocks = page.locator("div.g").all();
            log.info("Found {} result blocks for query: {}", resultBlocks.size(), query);
            
            if (resultBlocks.isEmpty()) {
                log.warn("No search results found. Trying alternative selector...");
                // Try alternative selector
                resultBlocks = page.locator("div[data-sokoban-container]").all();
                log.info("Alternative selector found {} blocks", resultBlocks.size());
            }
            
            if (resultBlocks.isEmpty()) {
                log.error("No search results found with any selector. Google may be blocking or no results exist.");
                log.error("Page HTML preview: {}", page.content().substring(0, Math.min(500, page.content().length())));
                return null;
            }
            
            for (int i = 0; i < resultBlocks.size(); i++) {
                Locator block = resultBlocks.get(i);
                String url = extractMainResultLink(block);
                log.debug("Block {}: Extracted URL: {}", i, url);
                
                if (url != null && isValidDomain(url, targetDomain, pageType)) {
                    log.info("✓ Found {} URL for {}: {}", pageType.getDisplayName(), businessName, url);
                    return url;
                } else if (url != null) {
                    log.debug("✗ URL rejected (domain mismatch): {}", url);
                }
            }
            
            log.warn("No {} URL found for: {}", pageType.getDisplayName(), businessName);
            return null;
            
        } catch (Exception e) {
            log.error("Error finding {} URL for {}: {}", pageType.getDisplayName(), businessName, e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the main search result link from a Google result block.
     * Uses multiple fallback strategies to handle Google's dynamic HTML.
     */
    private String extractMainResultLink(Locator resultBlock) {
        try {
            // Strategy 1: Find anchor containing h3.LC20lb (Google's main result title class)
            try {
                var h3Links = resultBlock.locator("a:has(h3.LC20lb)").all();
                if (!h3Links.isEmpty()) {
                    String href = h3Links.get(0).getAttribute("href");
                    if (href != null && !href.isBlank()) {
                        String cleaned = cleanGoogleRedirectUrl(href);
                        if (cleaned != null) {
                            log.debug("Strategy 1 (a:has(h3.LC20lb)) found: {}", cleaned);
                            return cleaned;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Strategy 1 failed: {}", e.getMessage());
            }
            
            // Strategy 2: Find anchor that contains any h3 (fallback)
            try {
                var h3Links = resultBlock.locator("a:has(h3)").all();
                if (!h3Links.isEmpty()) {
                    String href = h3Links.get(0).getAttribute("href");
                    if (href != null && !href.isBlank()) {
                        String cleaned = cleanGoogleRedirectUrl(href);
                        if (cleaned != null) {
                            log.debug("Strategy 2 (a:has(h3)) found: {}", cleaned);
                            return cleaned;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Strategy 2 failed: {}", e.getMessage());
            }
            
            // Strategy 3: Target h3's parent anchor
            try {
                Locator h3 = resultBlock.locator("h3").first();
                Locator mainLink = h3.locator("xpath=ancestor::a[1]").first();
                String href = mainLink.getAttribute("href");
                if (href != null && !href.isBlank()) {
                    String cleaned = cleanGoogleRedirectUrl(href);
                    if (cleaned != null) {
                        log.debug("Strategy 3 (h3 ancestor) found: {}", cleaned);
                        return cleaned;
                    }
                }
            } catch (Exception e) {
                log.debug("Strategy 3 failed: {}", e.getMessage());
            }
            
            // Strategy 4: Find first valid anchor with href in result block
            try {
                var links = resultBlock.locator("a[href]").all();
                for (Locator link : links) {
                    String href = link.getAttribute("href");
                    if (href != null && !href.isBlank() && !href.startsWith("#")) {
                        String cleaned = cleanGoogleRedirectUrl(href);
                        if (cleaned != null && (cleaned.startsWith("http://") || cleaned.startsWith("https://"))) {
                            log.debug("Strategy 4 (first valid link) found: {}", cleaned);
                            return cleaned;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Strategy 4 failed: {}", e.getMessage());
            }
            
            // Strategy 5: Try cite tag (shows visible URL)
            try {
                String citeUrl = resultBlock.locator("cite").first().textContent();
                if (citeUrl != null && !citeUrl.isBlank()) {
                    if (!citeUrl.startsWith("http")) {
                        citeUrl = "https://" + citeUrl;
                    }
                    log.debug("Strategy 5 (cite tag) found: {}", citeUrl);
                    return citeUrl;
                }
            } catch (Exception e) {
                log.debug("Strategy 5 failed: {}", e.getMessage());
            }
            
            return null;
            
        } catch (Exception e) {
            log.debug("All extraction strategies failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validates URL against target domain and blocks unwanted domains.
     */
    private boolean isValidDomain(String url, String targetDomain, TargetPageType pageType) {
        if (url == null || url.isBlank()) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        
        // Block list
        String[] blockedDomains = {
            "google.com", "google.co", "goo.gl",
            "indeed.com", "jobstreet.com", "jobsdb.com",
            "yelp.com", "yellowpages.com", "whitepages.com",
            "linkedin.com/jobs", "glassdoor.com"
        };
        
        for (String blocked : blockedDomains) {
            if (lowerUrl.contains(blocked)) {
                return false;
            }
        }
        
        // For WEBSITE type, accept any non-blocked domain
        if (pageType == TargetPageType.WEBSITE) {
            return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
        }
        
        // For specific types, must match target domain
        if (targetDomain != null && !targetDomain.isBlank()) {
            return lowerUrl.contains(targetDomain);
        }
        
        return true;
    }

    /**
     * Cleans Google redirect URLs to get actual destination.
     */
    private String cleanGoogleRedirectUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }

        if (href.startsWith("/url?q=")) {
            Pattern pattern = Pattern.compile("/url\\?q=([^&]+)");
            Matcher matcher = pattern.matcher(href);
            if (matcher.find()) {
                String encoded = matcher.group(1);
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            }
        }

        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }

        return null;
    }
}
