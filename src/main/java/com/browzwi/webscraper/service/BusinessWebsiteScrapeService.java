package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.browzwi.webscraper.scraper.service.HtmlFetcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for scraping business websites to extract email and social media links.
 *
 * @since 1.0
 */
@Service
public class BusinessWebsiteScrapeService {

    private static final Logger log = LoggerFactory.getLogger(BusinessWebsiteScrapeService.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    private static final List<String> SOCIAL_DOMAINS = List.of(
        "facebook.com", "instagram.com", "twitter.com", "x.com",
        "linkedin.com", "tiktok.com", "youtube.com", "pinterest.com"
    );

    private final HtmlFetcher htmlFetcher;

    public BusinessWebsiteScrapeService(HtmlFetcher htmlFetcher) {
        this.htmlFetcher = htmlFetcher;
    }

    public void enrichBusinessData(DiscoveredBusiness business) {
        if (business.getWebsiteUrl() == null || business.getWebsiteUrl().isBlank()) {
            log.debug("No website URL for business: {}", business.getBusinessName());
            return;
        }

        try {
            String html = htmlFetcher.fetch(business.getWebsiteUrl());
            Document doc = Jsoup.parse(html);

            String email = extractEmail(html);
            business.setEmailAddress(email);

            List<String> socialLinks = extractSocialLinks(doc);
            if (!socialLinks.isEmpty()) {
                business.setSocialMediaLinks(String.join(",", socialLinks));
            }

            business.setStatus("SCRAPED");
            log.info("Enriched business: {} - Email: {}, Social: {}", 
                business.getBusinessName(), email, socialLinks.size());

        } catch (Exception e) {
            log.error("Failed to enrich business: {}", business.getBusinessName(), e);
            business.setStatus("FAILED");
        }
    }

    private String extractEmail(String html) {
        Matcher matcher = EMAIL_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private List<String> extractSocialLinks(Document doc) {
        List<String> links = new ArrayList<>();

        for (Element element : doc.select("a[href]")) {
            String href = element.attr("abs:href");
            
            if (href != null && !href.isBlank()) {
                for (String domain : SOCIAL_DOMAINS) {
                    if (href.contains(domain) && !links.contains(href)) {
                        links.add(href);
                        break;
                    }
                }
            }
        }

        return links;
    }

    public void enrichMultiple(List<DiscoveredBusiness> businesses) {
        for (DiscoveredBusiness business : businesses) {
            try {
                enrichBusinessData(business);
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Error enriching business", e);
            }
        }
    }
}
