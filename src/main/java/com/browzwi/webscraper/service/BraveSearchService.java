package com.browzwi.webscraper.service;

import com.browzwi.webscraper.domain.DiscoveredBusiness;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class BraveSearchService {

    private static final Logger log = LoggerFactory.getLogger(BraveSearchService.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BRAVE_API_URL = "https://api.search.brave.com/res/v1/web/search";

    public BraveSearchService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<DiscoveredBusiness> search(String keyword, String location, String apiKey) {
        List<DiscoveredBusiness> businesses = new ArrayList<>();
        String query = keyword + " in " + location;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BRAVE_API_URL + "?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&count=15"))
                .header("Accept", "application/json")
                .header("X-Subscription-Token", apiKey)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                log.error("Brave API returned status: {}", response.statusCode());
                throw new RuntimeException("Brave Search API error: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("web").path("results");

            for (JsonNode result : results) {
                DiscoveredBusiness business = new DiscoveredBusiness();
                business.setBusinessName(result.path("title").asText("Unknown"));
                business.setWebsiteUrl(result.path("url").asText(""));
                business.setAddress(location);
                business.setPhoneNumber("");
                business.setEmailAddress("");
                business.setSocialMediaLinks("");
                business.setStatus("DISCOVERED");
                businesses.add(business);
            }

            log.info("Brave Search found {} businesses", businesses.size());

        } catch (Exception e) {
            log.error("Brave Search failed: {}", e.getMessage());
            throw new RuntimeException("Search failed: " + e.getMessage());
        }

        return businesses;
    }
}
