package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.browzwi.webscraper.domain.DiscoveryJob;
import com.browzwi.webscraper.repository.DiscoveredBusinessRepository;
import com.browzwi.webscraper.repository.DiscoveryJobRepository;
import com.browzwi.webscraper.service.settings.SettingsService;
import com.browzwi.webscraper.service.settings.TargetPageType;
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
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
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
    private static final Random random = new Random();

    private final DiscoveryJobRepository discoveryJobRepository;
    private final GoogleSearchDiscoveryService googleSearchDiscoveryService;
    private final DiscoveredBusinessRepository businessRepository;

    public UrlDiscoveryService(DiscoveryJobRepository discoveryJobRepository,
                               GoogleSearchDiscoveryService googleSearchDiscoveryService,
                               DiscoveredBusinessRepository businessRepository) {
        this.discoveryJobRepository = discoveryJobRepository;
        this.googleSearchDiscoveryService = googleSearchDiscoveryService;
        this.businessRepository = businessRepository;
    }

    @Transactional
    public DiscoveryJob discover(String keyword, String location, String targetPageType, String customPattern, Integer maxResults) {
        if (maxResults == null || maxResults < 1) {
            maxResults = 100;
        }
        
        DiscoveryJob job = new DiscoveryJob();
        job.setKeyword(keyword);
        job.setLocation(location);
        job.setTargetPageType(targetPageType);
        job.setCustomSearchPattern(customPattern);
        job.setStatus("RUNNING");
        job.setCreatedAt(LocalDateTime.now());
        discoveryJobRepository.save(job);

        List<DiscoveredBusiness> results = discoverViaMapsAndSearch(keyword, location, targetPageType, customPattern, maxResults);
        job.setDiscoverySource("GMAPS_GOOGLE_SEARCH");

        results.forEach(b -> b.setDiscoveryJob(job));
        job.setBusinesses(results);
        job.setResultsCount(results.size());
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        discoveryJobRepository.save(job);

        return job;
    }

    @Async
    @Transactional
    public void discoverAsync(Long jobId, String keyword, String location, String targetPageType, String customPattern, Integer maxResults) {
        if (maxResults == null || maxResults < 1) {
            maxResults = 100;
        }

        DiscoveryJob job = discoveryJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        try {
            List<DiscoveredBusiness> results = discoverViaMapsAndSearch(keyword, location, targetPageType, customPattern, maxResults);

            results.forEach(b -> {
                b.setDiscoveryJob(job);
                businessRepository.save(b);
            });

            job.setResultsCount(results.size());
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
            discoveryJobRepository.save(job);
        } catch (Exception e) {
            log.error("Discovery job {} failed", jobId, e);
            discoveryJobRepository.findById(jobId).ifPresent(j -> {
                j.setStatus("FAILED");
                j.setCompletedAt(LocalDateTime.now());
                discoveryJobRepository.save(j);
            });
        }
    }

    private List<DiscoveredBusiness> discoverViaMapsAndSearch(String keyword, String location, String targetPageType, String customPattern, Integer maxResults) {
        log.info("Discovering business names from Google Maps and generating search URLs");
        
        // Get business names from Google Maps
        List<DiscoveredBusiness> businesses = discoverBusinessNamesFromMaps(keyword, location, maxResults);
        log.info("Found {} business names from Maps", businesses.size());
        
        // Generate Google Search URL for each business
        TargetPageType pageType = TargetPageType.valueOf(targetPageType);
        for (DiscoveredBusiness business : businesses) {
            String searchQuery = business.getBusinessName() + " " + location + " " + pageType.getDisplayName();
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String googleSearchUrl = "https://www.google.com/search?q=" + encodedQuery;
            
            business.setTargetUrl(googleSearchUrl);
            business.setTargetPageType(targetPageType);
            business.setStatus("DISCOVERED");
            
            log.info("Generated URL for {}: {}", business.getBusinessName(), googleSearchUrl);
        }
        
        log.info("Discovery complete: {} businesses with Google Search URLs ready for export", businesses.size());
        return businesses;
    }

    private List<DiscoveredBusiness> discoverBusinessNamesFromMaps(String keyword, String location, Integer maxResults) {
        String searchQuery = URLEncoder.encode(keyword + " " + location, StandardCharsets.UTF_8);
        String mapsUrl = "https://www.google.com/maps/search/" + searchQuery;
        
        List<DiscoveredBusiness> businesses = new ArrayList<>();
        
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch();
             BrowserContext context = browser.newContext();
             Page page = context.newPage()) {
            
            page.navigate(mapsUrl);
            page.waitForSelector("div[role='feed']", new Page.WaitForSelectorOptions().setTimeout(10000));
            
            Locator resultsPanel = page.locator("div[role='feed']").first();
            
            // Scroll to load results (reduced delay)
            for (int scroll = 0; scroll < 50; scroll++) {
                resultsPanel.evaluate("el => el.scrollTo(0, el.scrollHeight)");
                Thread.sleep(1000);  // Reduced from 2000ms
                
                List<Locator> currentCards = page.locator("div[role='article']").all();
                
                if (currentCards.size() >= maxResults) {
                    break;
                }
                
                if (page.locator("span:has-text('reached the end')").count() > 0) {
                    break;
                }
            }
            
            List<Locator> businessCards = page.locator("div[role='article']").all();
            int limit = Math.min(businessCards.size(), maxResults);
            
            for (int i = 0; i < limit; i++) {
                Locator card = businessCards.get(i);
                
                try {
                    String name = extractBusinessName(card);
                    
                    if (name != null && !name.isBlank()) {
                        DiscoveredBusiness business = new DiscoveredBusiness();
                        business.setBusinessName(name.trim());
                        business.setStatus("PENDING_URL_DISCOVERY");
                        business.setAddress("");  // Will be extracted by Job
                        business.setPhoneNumber("");  // Will be extracted by Job
                        business.setWebsiteUrl("");  // Will be filled by Google Search
                        
                        businesses.add(business);
                        log.info("Added business name: {}", name);
                    }
                    
                } catch (Exception e) {
                    log.warn("Error extracting business name from card {}: {}", i, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Error discovering from Google Maps", e);
            throw new RuntimeException("Google Maps discovery failed: " + e.getMessage());
        }
        
        return businesses;
    }

    private String extractBusinessName(Locator card) {
        try {
            return card.locator("div.fontHeadlineSmall").first().textContent();
        } catch (Exception e) {
            try {
                return card.locator("a.hfpxzc").first().getAttribute("aria-label");
            } catch (Exception e2) {
                try {
                    return card.locator("h3, h2, .qBF1Pd").first().textContent();
                } catch (Exception e3) {
                    return null;
                }
            }
        }
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
