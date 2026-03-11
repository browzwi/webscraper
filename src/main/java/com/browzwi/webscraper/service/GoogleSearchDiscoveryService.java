package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for discovering businesses via Google Search using Playwright.
 * Scrapes regular Google Search results to extract business information.
 *
 * @since 1.0
 */
@Service
public class GoogleSearchDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSearchDiscoveryService.class);
    private static final int MAX_PAGES = 3;
    private static final int RESULTS_PER_PAGE = 10;
    private static final Random random = new Random();

    private final SettingsService settingsService;

    public GoogleSearchDiscoveryService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public List<DiscoveredBusiness> discover(String keyword, String location, Integer maxResults) {
        if (maxResults == null || maxResults < 1) {
            maxResults = 30;
        }
        
        String claudeApiKey = settingsService.getClaudeApiKey();
        
        // Use Claude AI if API key is configured
        if (claudeApiKey != null && !claudeApiKey.isBlank()) {
            log.info("Using Claude AI for search: {} {}", keyword, location);
            return discoverWithClaude(keyword, location, claudeApiKey, maxResults);
        }
        
        // Fallback to Playwright scraping
        log.info("Using Playwright for search: {} {}", keyword, location);
        return discoverWithPlaywright(keyword, location, maxResults);
    }

    private List<DiscoveredBusiness> discoverWithClaude(String keyword, String location, String apiKey, Integer maxResults) {
        String siteFilter = settingsService.getGoogleSearchSiteFilter();
        String searchContext = siteFilter != null && !siteFilter.isBlank() 
            ? " focusing on " + siteFilter + " results" 
            : "";

        String prompt = String.format(
            "Search Google for '%s %s'%s and extract business information. " +
            "For each result, provide: business name, website URL, and any social media links. " +
            "Return up to %d results in this exact JSON format:\n" +
            "[{\"name\":\"Business Name\",\"website\":\"https://example.com\",\"social\":\"Facebook, Instagram\"}]\n" +
            "Only return the JSON array, no other text.",
            keyword, location, searchContext, maxResults
        );

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            String requestBody = String.format(
                "{\"model\":\"claude-3-5-sonnet-20241022\"," +
                "\"max_tokens\":4096," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
                prompt.replace("\"", "\\\"").replace("\n", "\\n")
            );

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            log.info("Sending request to Claude API...");
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Claude API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("Claude API failed: " + response.body());
            }

            String responseBody = response.body();
            log.debug("Claude response: {}", responseBody);

            return parseClaudeResponse(responseBody);

        } catch (Exception e) {
            log.error("Error using Claude AI", e);
            throw new RuntimeException("Claude AI search failed: " + e.getMessage(), e);
        }
    }

    private List<DiscoveredBusiness> parseClaudeResponse(String responseBody) {
        List<DiscoveredBusiness> businesses = new ArrayList<>();
        
        try {
            Pattern contentPattern = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = contentPattern.matcher(responseBody);
            
            if (!matcher.find()) {
                log.error("No content found in Claude response");
                return businesses;
            }
            
            String content = matcher.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\/", "/");
            
            log.info("Extracted content from Claude: {}", content.substring(0, Math.min(200, content.length())));
            
            Pattern jsonArrayPattern = Pattern.compile("\\[.*\\]", Pattern.DOTALL);
            Matcher jsonMatcher = jsonArrayPattern.matcher(content);
            
            if (!jsonMatcher.find()) {
                log.error("No JSON array found in content");
                return businesses;
            }
            
            String jsonArray = jsonMatcher.group();
            Pattern businessPattern = Pattern.compile(
                "\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"website\"\\s*:\\s*\"([^\"]+)\"\\s*(?:,\\s*\"social\"\\s*:\\s*\"([^\"]*)\")?\\s*\\}"
            );
            Matcher businessMatcher = businessPattern.matcher(jsonArray);
            
            while (businessMatcher.find()) {
                String name = businessMatcher.group(1);
                String website = businessMatcher.group(2);
                String social = businessMatcher.group(3);
                
                DiscoveredBusiness business = new DiscoveredBusiness();
                business.setBusinessName(name);
                business.setWebsiteUrl(website);
                business.setStatus("DISCOVERED");
                
                if (social != null && !social.isBlank()) {
                    business.setSocialMediaLinks(social);
                }
                
                businesses.add(business);
                log.info("Parsed from Claude: {}", name);
            }
            
            log.info("Claude AI extracted {} businesses", businesses.size());
            
        } catch (Exception e) {
            log.error("Error parsing Claude response", e);
        }
        
        return businesses;
    }

    private List<DiscoveredBusiness> discoverWithPlaywright(String keyword, String location, Integer maxResults) {
        String query = keyword + " " + location;
        String siteFilter = settingsService.getGoogleSearchSiteFilter();
        
        if (siteFilter != null && !siteFilter.isBlank()) {
            query += " site:" + siteFilter.trim();
        }

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        List<DiscoveredBusiness> businesses = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            
            java.nio.file.Path userDataDir = java.nio.file.Paths.get("./playwright-google-cache");
            
            log.info("Using persistent browser context at: {}", userDataDir.toAbsolutePath());
            
            BrowserContext context = playwright.chromium().launchPersistentContext(
                userDataDir,
                new com.microsoft.playwright.BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(false)  // Visible browser
                    .setViewportSize(1920, 1080)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .setArgs(java.util.List.of(
                        "--disable-blink-features=AutomationControlled",
                        "--exclude-switches=enable-automation",
                        "--disable-dev-shm-usage",
                        "--disable-infobars",
                        "--start-maximized"
                    ))
            );

            Page page = context.pages().get(0);  // Use existing page
            
            // Remove automation indicators
            page.addInitScript("" +
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                "delete window.cdc_adoQpoasnfa76pfcZLmcfl_Array;" +
                "delete window.cdc_adoQpoasnfa76pfcZLmcfl_Promise;" +
                "delete window.cdc_adoQpoasnfa76pfcZLmcfl_Symbol;" +
                "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});" +
                "Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});"
            );

            // First time setup: Let user manually interact
            boolean isFirstRun = !java.nio.file.Files.exists(userDataDir.resolve("Default/Cookies"));
            
            if (isFirstRun) {
                log.info("===========================================");
                log.info("FIRST RUN DETECTED - MANUAL SETUP REQUIRED");
                log.info("===========================================");
                log.info("Chrome browser opened. Please:");
                log.info("1. Search for anything on Google");
                log.info("2. Accept cookies if prompted");
                log.info("3. Solve CAPTCHA if shown");
                log.info("4. Wait 30 seconds for automatic continuation...");
                log.info("===========================================");
                
                page.navigate("https://www.google.com");
                
                // Wait 30 seconds for user to interact
                Thread.sleep(30000);
                
                log.info("Continuing with automated scraping...");
            }

            int maxPages = (int) Math.ceil(maxResults / (double) RESULTS_PER_PAGE);
            for (int pageNum = 0; pageNum < maxPages; pageNum++) {
                int start = pageNum * RESULTS_PER_PAGE;
                String searchUrl = "https://www.google.com/search?q=" + encodedQuery + "&start=" + start + "&hl=en";

                log.info("Scraping Google Search page {}: {}", pageNum + 1, searchUrl);
                
                try {
                    page.navigate(searchUrl, new Page.NavigateOptions().setTimeout(30000));
                } catch (Exception e) {
                    log.error("Failed to navigate to Google Search: {}", e.getMessage());
                    break;
                }
                
                if (businesses.size() >= maxResults) {
                    log.info("Reached max results limit: {}", maxResults);
                    break;
                }

                randomDelay();

                try {
                    page.waitForSelector("div#search, div#rso, div#rcnt", new Page.WaitForSelectorOptions().setTimeout(15000));
                } catch (Exception e) {
                    log.warn("Search results not loaded on page {}", pageNum + 1);
                    break;
                }

                List<Locator> resultBlocks = page.locator("div.g").all();
                
                if (resultBlocks.isEmpty()) {
                    resultBlocks = page.locator("div[data-hveid]").all();
                }
                
                if (resultBlocks.isEmpty()) {
                    resultBlocks = page.locator("div.Gx5Zad").all();
                }
                
                log.info("Found {} result blocks on page {}", resultBlocks.size(), pageNum + 1);

                if (resultBlocks.isEmpty()) {
                    log.warn("No results found on page {}. Checking for CAPTCHA or blocks...", pageNum + 1);
                    
                    // Check if CAPTCHA is present
                    if (page.locator("iframe[src*='recaptcha']").count() > 0) {
                        log.error("CAPTCHA detected! Please solve it manually in the browser.");
                        log.info("Waiting 60 seconds for you to solve CAPTCHA...");
                        Thread.sleep(60000);
                        continue; // Retry this page
                    }
                    
                    // Check for "unusual traffic" message
                    if (page.content().contains("unusual traffic") || page.content().contains("not a robot")) {
                        log.error("Google detected unusual traffic. Session may be blocked.");
                    }
                    
                    // Save screenshot for debugging
                    try {
                        page.screenshot(new Page.ScreenshotOptions()
                            .setPath(java.nio.file.Paths.get("./google-search-debug.png")));
                        log.info("Screenshot saved to: ./google-search-debug.png");
                    } catch (Exception e) {
                        log.debug("Could not save screenshot");
                    }
                    
                    break;
                }

                for (Locator block : resultBlocks) {
                    if (businesses.size() >= maxResults) {
                        break;
                    }
                    
                    try {
                        DiscoveredBusiness business = extractBusinessFromResult(block, keyword);
                        if (business != null) {
                            businesses.add(business);
                            log.info("Extracted: {}", business.getBusinessName());
                        }
                    } catch (Exception e) {
                        log.debug("Failed to extract business from result block: {}", e.getMessage());
                    }
                }

                if (businesses.size() >= maxResults) {
                    break;
                }
            }

            log.info("Google Search discovery completed. Found {} businesses", businesses.size());
            
            // Keep context open for reuse (persistent browser session)

        } catch (Exception e) {
            log.error("Error during Google Search discovery", e);
            throw new RuntimeException("Google Search discovery failed: " + e.getMessage(), e);
        }

        return businesses;
    }

    private DiscoveredBusiness extractBusinessFromResult(Locator resultBlock, String expectedBusinessName) {
        try {
            String title = null;
            List<String> allLinks = new ArrayList<>();
            String email = null;
            
            // Extract title
            try {
                Locator h3 = resultBlock.locator("h3").first();
                title = h3.textContent(new Locator.TextContentOptions().setTimeout(3000));
            } catch (Exception e) {
                try {
                    title = resultBlock.locator("h1, h2, h3, h4").first()
                        .textContent(new Locator.TextContentOptions().setTimeout(3000));
                } catch (Exception e2) {
                    log.debug("Could not extract title");
                }
            }

            if (title == null || title.isBlank()) {
                return null;
            }

            // Validate: Check if title matches the expected business name
            String normalizedTitle = title.toLowerCase().replaceAll("[^a-z0-9]", "");
            String normalizedExpected = expectedBusinessName.toLowerCase().replaceAll("[^a-z0-9]", "");
            
            // Check if at least 50% of the words match
            if (!normalizedTitle.contains(normalizedExpected.substring(0, Math.min(5, normalizedExpected.length())))) {
                log.debug("Title '{}' doesn't match expected '{}'", title, expectedBusinessName);
                return null;
            }

            // Extract email from text content
            try {
                String textContent = resultBlock.textContent();
                if (textContent != null) {
                    java.util.regex.Pattern emailPattern = java.util.regex.Pattern.compile(
                        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
                    );
                    java.util.regex.Matcher matcher = emailPattern.matcher(textContent);
                    if (matcher.find()) {
                        email = matcher.group();
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to extract email: {}", e.getMessage());
            }

            // Extract ALL links from the result block
            try {
                List<Locator> links = resultBlock.locator("a[href]").all();
                for (Locator link : links) {
                    try {
                        String href = link.getAttribute("href", new Locator.GetAttributeOptions().setTimeout(1000));
                        if (href == null || href.isBlank()) continue;
                        
                        String cleanUrl = cleanGoogleRedirectUrl(href);
                        if (cleanUrl == null || cleanUrl.isBlank()) continue;
                        
                        // Skip Google Maps and irrelevant links
                        if (cleanUrl.contains("google.com/maps") || 
                            cleanUrl.contains("goo.gl/maps") ||
                            cleanUrl.contains("youtube.com") ||
                            cleanUrl.contains("translate.google.com")) {
                            continue;
                        }
                        
                        // Add unique links only
                        if (!allLinks.contains(cleanUrl)) {
                            allLinks.add(cleanUrl);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to process link: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to extract links: {}", e.getMessage());
            }

            if (allLinks.isEmpty() && email == null) {
                return null;
            }

            DiscoveredBusiness business = new DiscoveredBusiness();
            business.setBusinessName(title.trim());
            business.setWebsiteUrl(!allLinks.isEmpty() ? String.join("\n", allLinks) : "");
            business.setEmailAddress(email);
            business.setStatus("DISCOVERED");
            business.setAddress("");
            business.setPhoneNumber("");

            return business;

        } catch (Exception e) {
            log.debug("Failed to extract business: {}", e.getMessage());
            return null;
        }
    }

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

    private String detectSocialMedia(String url) {
        if (url == null) {
            return null;
        }

        String lowerUrl = url.toLowerCase();
        List<String> platforms = new ArrayList<>();

        if (lowerUrl.contains("facebook.com")) platforms.add("Facebook");
        if (lowerUrl.contains("instagram.com")) platforms.add("Instagram");
        if (lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com")) platforms.add("Twitter");
        if (lowerUrl.contains("tiktok.com")) platforms.add("TikTok");
        if (lowerUrl.contains("youtube.com")) platforms.add("YouTube");
        if (lowerUrl.contains("linkedin.com")) platforms.add("LinkedIn");

        return platforms.isEmpty() ? null : String.join(", ", platforms);
    }

    private void randomDelay() {
        try {
            int delay = 1500 + random.nextInt(2000);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
