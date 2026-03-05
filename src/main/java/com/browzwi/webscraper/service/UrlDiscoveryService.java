package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.browzwi.webscraper.domain.DiscoveryJob;
import com.browzwi.webscraper.repository.DiscoveredBusinessRepository;
import com.browzwi.webscraper.repository.DiscoveryJobRepository;
import com.browzwi.webscraper.scraper.service.PlaywrightFetcher;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for discovering business URLs from Google Maps.
 *
 * @since 1.0
 */
@Service
public class UrlDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(UrlDiscoveryService.class);

    private final SettingsService settingsService;
    private final PlaywrightFetcher playwrightFetcher;
    private final DiscoveredBusinessRepository businessRepository;
    private final DiscoveryJobRepository discoveryJobRepository;
    private final GoogleSearchDiscoveryService googleSearchDiscoveryService;

    public UrlDiscoveryService(SettingsService settingsService,
                               PlaywrightFetcher playwrightFetcher,
                               DiscoveredBusinessRepository businessRepository,
                               DiscoveryJobRepository discoveryJobRepository,
                               GoogleSearchDiscoveryService googleSearchDiscoveryService) {
        this.settingsService = settingsService;
        this.playwrightFetcher = playwrightFetcher;
        this.businessRepository = businessRepository;
        this.discoveryJobRepository = discoveryJobRepository;
        this.googleSearchDiscoveryService = googleSearchDiscoveryService;
    }

    @Transactional
    public DiscoveryJob discover(String keyword, String location) {
        DiscoveryJob job = new DiscoveryJob();
        job.setKeyword(keyword);
        job.setLocation(location);
        job.setStatus("RUNNING");
        job.setCreatedAt(LocalDateTime.now());
        discoveryJobRepository.save(job);

        List<DiscoveredBusiness> results;
        String source = settingsService.getDiscoverySourceType();

        if ("GOOGLE_SEARCH".equals(source)) {
            results = googleSearchDiscoveryService.discover(keyword, location);
            job.setDiscoverySource("GOOGLE_SEARCH");
        } else {
            results = discoverViaGoogleMaps(keyword, location);
            job.setDiscoverySource("GOOGLE_MAPS");
        }

        results.forEach(b -> b.setDiscoveryJob(job));
        job.setBusinesses(results);
        job.setResultsCount(results.size());
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        discoveryJobRepository.save(job);

        return job;
    }

    private List<DiscoveredBusiness> discoverViaGoogleMaps(String keyword, String location) {
        String searchQuery = URLEncoder.encode(keyword + " " + location, StandardCharsets.UTF_8);
        String mapsUrl = "https://www.google.com/maps/search/" + searchQuery;

        List<DiscoveredBusiness> businesses = new ArrayList<>();

        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch();
             BrowserContext context = browser.newContext();
             Page page = context.newPage()) {

            log.info("Navigating to Google Maps: {}", mapsUrl);
            page.navigate(mapsUrl);
            page.waitForLoadState();
            Thread.sleep(5000);

            Locator resultsPanel = page.locator("div[role='feed']").first();
            
            log.info("Scrolling to load more results...");
            for (int scroll = 0; scroll < 20; scroll++) {
                try {
                    resultsPanel.evaluate("element => element.scrollTo(0, element.scrollHeight)");
                    Thread.sleep(2000);
                    
                    List<Locator> currentCards = page.locator("div[role='article']").all();
                    log.info("Scroll {}: Found {} business cards", scroll + 1, currentCards.size());
                    
                    if (currentCards.size() >= 100) {
                        log.info("Reached 100 results, stopping scroll");
                        break;
                    }
                    
                    try {
                        Locator endMessage = page.locator("span:has-text('You've reached the end')");
                        if (endMessage.count() > 0) {
                            log.info("Reached end of results");
                            break;
                        }
                    } catch (Exception e) {
                        // Continue scrolling
                    }
                } catch (Exception e) {
                    log.warn("Error during scroll {}: {}", scroll + 1, e.getMessage());
                    break;
                }
            }

            List<Locator> businessCards = page.locator("div[role='article']").all();
            log.info("Total business cards found after scrolling: {}", businessCards.size());

            int limit = Math.min(businessCards.size(), 100);
            for (int i = 0; i < limit; i++) {
                Locator card = businessCards.get(i);
                try {
                    DiscoveredBusiness business = new DiscoveredBusiness();
                    business.setStatus("DISCOVERED");

                    String name = null;
                    try {
                        name = card.locator("div.fontHeadlineSmall").first().textContent();
                    } catch (Exception e) {
                        try {
                            name = card.locator("a.hfpxzc").first().getAttribute("aria-label");
                        } catch (Exception e2) {
                            try {
                                name = card.locator("h3, h2, .qBF1Pd").first().textContent();
                            } catch (Exception e3) {
                                log.warn("Could not find business name for card {}", i);
                            }
                        }
                    }
                    
                    if (name != null && !name.isBlank()) {
                        business.setBusinessName(name.trim());
                    } else {
                        log.warn("Skipping card {} - no name found", i);
                        continue;
                    }

                    try {
                        String address = card.locator("button[data-tooltip*='address'], button[data-item-id*='address'], .W4Efsd:has-text('·')").first().textContent(new Locator.TextContentOptions().setTimeout(3000));
                        if (address != null && !address.isBlank()) {
                            business.setAddress(address.trim());
                        }
                    } catch (Exception e) {
                        log.debug("No address found for: {}", name);
                    }

                    try {
                        String phone = card.locator("button[data-tooltip*='phone'], span.UsdlK").first().textContent(new Locator.TextContentOptions().setTimeout(3000));
                        if (phone != null && !phone.isBlank()) {
                            business.setPhoneNumber(phone.trim());
                        }
                    } catch (Exception e) {
                        log.debug("No phone found for: {}", name);
                    }

                    try {
                        String website = card.locator("a[data-value='Website'], a[href*='http']").first().getAttribute("href", new Locator.GetAttributeOptions().setTimeout(3000));
                        if (website != null && !website.isBlank() && !website.contains("google.com")) {
                            business.setWebsiteUrl(website.trim());
                        }
                    } catch (Exception e) {
                        log.debug("No website found for: {}", name);
                    }

                    businesses.add(business);
                    log.info("Added business {}: {}", i + 1, name);

                } catch (Exception e) {
                    log.warn("Error parsing business card {}: {}", i, e.getMessage());
                }
            }

            if (businesses.isEmpty()) {
                log.error("No businesses found. Google Maps may have changed or blocked the request.");
                throw new RuntimeException("No results found. Try a different keyword or location.");
            }

            log.info("Successfully discovered {} businesses", businesses.size());

        } catch (Exception e) {
            log.error("Error discovering via Google Maps", e);
            throw new RuntimeException("Google Maps scraping failed: " + e.getMessage());
        }

        return businesses;
    }

    @Transactional(readOnly = true)
    public DiscoveryJob getJob(Long jobId) {
        return discoveryJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Discovery job not found: " + jobId));
    }

    @Transactional(readOnly = true)
    public List<DiscoveryJob> listJobs() {
        return discoveryJobRepository.findAllByOrderByCreatedAtDesc();
    }
}
