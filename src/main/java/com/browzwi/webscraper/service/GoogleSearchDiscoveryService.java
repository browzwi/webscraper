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
                .setHeadless(true)
                .setArgs(java.util.List.of(
                    "--disable-blink-features=AutomationControlled",
                    "--disable-dev-shm-usage"
                ))
            );
            
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );

            Page page = context.newPage();
            
            page.addInitScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
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
            
            // Small delay
            Thread.sleep(500 + random.nextInt(500));

            // Find result blocks
            var resultBlocks = page.locator("div.g").all();
            log.debug("Found {} result blocks", resultBlocks.size());
            
            if (resultBlocks.isEmpty()) {
                log.warn("No search results found. Google may be blocking or no results exist.");
                return null;
            }
            
            for (Locator block : resultBlocks) {
                String url = extractMainResultLink(block);
                log.debug("Extracted URL: {}", url);
                
                if (url != null && isValidDomain(url, targetDomain, pageType)) {
                    log.info("Found {} URL for {}: {}", pageType.getDisplayName(), businessName, url);
                    return url;
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
     * Targets the h3 parent anchor, not random links.
     */
    private String extractMainResultLink(Locator resultBlock) {
        try {
            // Target h3's parent anchor (the main result link)
            Locator h3 = resultBlock.locator("h3").first();
            Locator mainLink = h3.locator("..").first();
            String href = mainLink.getAttribute("href");
            
            if (href == null || href.isBlank()) {
                // Fallback: try a:has(h3)
                mainLink = resultBlock.locator("a:has(h3)").first();
                href = mainLink.getAttribute("href");
            }
            
            return cleanGoogleRedirectUrl(href);
            
        } catch (Exception e) {
            log.debug("Failed to extract main link: {}", e.getMessage());
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
