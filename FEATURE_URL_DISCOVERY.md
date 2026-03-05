# Feature: URL Discovery via Google Maps
## Implementation Guide for WebScraper Platform

---

## Overview

This document describes how to implement a **URL Discovery** feature in the existing WebScraper Spring Boot application. Instead of manually inputting URLs, users can type a **keyword + location** and the system will automatically discover business URLs from Google Maps.

The discovered data should include:
- Business Name
- Business Address / Location
- Email Address (scraped from business website)
- Other Media Links / Social Media (scraped from business website)
- Contact Number

---

## Existing Codebase Context

- **Framework:** Spring Boot 3.5.7, Java 17
- **Frontend:** Thymeleaf + HTMX + Tailwind CSS
- **Database:** MySQL (prod) / H2 (local)
- **Scraping Engines available:** HtmlUnit, Playwright (already integrated)
- **Package base:** `com.browzwi.webscraper`
- **Existing structure:**
  - `web/` — Controllers
  - `service/` — Business logic
  - `scraper/service/` — Scraping engine
  - `domain/` — JPA Entities
  - `repository/` — Spring Data Repositories
  - `templates/` — Thymeleaf HTML views

---

## Two Implementation Options

### Option A: Google Places API (Official - Recommended for Production)
- Uses Google Places Text Search API
- Requires API Key from Google Cloud Console
- Free tier: $200/month credit
- Endpoint: `https://maps.googleapis.com/maps/api/place/textsearch/json`

### Option B: Playwright Scraper (Free - Good for Development)
- Uses existing Playwright integration already in the codebase
- Navigates to `https://www.google.com/maps/search/{keyword}`
- Scrapes business listings directly
- No API key needed
- Risk: May be blocked by Google over time

**Both options must be supported. User can choose in Settings which one to use.**

---

## What Needs to Be Built

### 1. Database Changes

Add a new table `discovery_job` and update the existing `scrape_job` table.

#### New Entity: `DiscoveryJob`

```java
// domain/DiscoveryJob.java
@Entity
@Table(name = "discovery_job")
public class DiscoveryJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;
    private String location;         // e.g. "Kawit, Cavite"
    private String discoverySource;  // "GOOGLE_PLACES_API" or "PLAYWRIGHT"
    private String status;           // PENDING, RUNNING, COMPLETED, FAILED
    private Integer resultsCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "discoveryJob", cascade = CascadeType.ALL)
    private List<DiscoveredBusiness> businesses = new ArrayList<>();
}
```

#### New Entity: `DiscoveredBusiness`

```java
// domain/DiscoveredBusiness.java
@Entity
@Table(name = "discovered_business")
public class DiscoveredBusiness {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String businessName;
    private String address;
    private String phoneNumber;
    private String websiteUrl;
    private String emailAddress;      // populated after website scrape
    private String socialMediaLinks;  // JSON string, populated after website scrape
    private String status;            // DISCOVERED, SCRAPED, FAILED

    @ManyToOne
    @JoinColumn(name = "discovery_job_id")
    private DiscoveryJob discoveryJob;
}
```

#### Liquibase Migration File

Create: `src/main/resources/db/changelog/changes/add-discovery-tables.xml`

```xml
<changeSet id="add-discovery-tables" author="webscraper">
    <createTable tableName="discovery_job">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="keyword" type="VARCHAR(255)"/>
        <column name="location" type="VARCHAR(255)"/>
        <column name="discovery_source" type="VARCHAR(50)"/>
        <column name="status" type="VARCHAR(50)"/>
        <column name="results_count" type="INT"/>
        <column name="created_at" type="DATETIME"/>
        <column name="completed_at" type="DATETIME"/>
    </createTable>

    <createTable tableName="discovered_business">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true"/>
        </column>
        <column name="business_name" type="VARCHAR(255)"/>
        <column name="address" type="VARCHAR(500)"/>
        <column name="phone_number" type="VARCHAR(100)"/>
        <column name="website_url" type="VARCHAR(500)"/>
        <column name="email_address" type="VARCHAR(255)"/>
        <column name="social_media_links" type="TEXT"/>
        <column name="status" type="VARCHAR(50)"/>
        <column name="discovery_job_id" type="BIGINT">
            <constraints foreignKeyName="fk_business_discovery_job"
                         references="discovery_job(id)"/>
        </column>
    </createTable>
</changeSet>
```

---

### 2. New Service: `UrlDiscoveryService`

Create: `src/main/java/com/browzwi/webscraper/service/UrlDiscoveryService.java`

This service handles the discovery logic. It checks which option is configured (API or Playwright) and calls the appropriate method.

```java
@Service
public class UrlDiscoveryService {

    private final AppSettings appSettings; // existing settings service
    private final PlaywrightFetcher playwrightFetcher; // existing
    private final DiscoveredBusinessRepository businessRepository;
    private final DiscoveryJobRepository discoveryJobRepository;

    // Main entry point
    public DiscoveryJob discover(String keyword, String location) {
        DiscoveryJob job = new DiscoveryJob();
        job.setKeyword(keyword);
        job.setLocation(location);
        job.setStatus("RUNNING");
        job.setCreatedAt(LocalDateTime.now());
        discoveryJobRepository.save(job);

        List<DiscoveredBusiness> results;

        String source = appSettings.getDiscoverySource(); // "GOOGLE_PLACES_API" or "PLAYWRIGHT"

        if ("GOOGLE_PLACES_API".equals(source)) {
            results = discoverViaGooglePlacesApi(keyword, location);
            job.setDiscoverySource("GOOGLE_PLACES_API");
        } else {
            results = discoverViaPlaywright(keyword, location);
            job.setDiscoverySource("PLAYWRIGHT");
        }

        results.forEach(b -> b.setDiscoveryJob(job));
        job.setBusinesses(results);
        job.setResultsCount(results.size());
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        discoveryJobRepository.save(job);

        return job;
    }

    // Option A: Google Places API
    private List<DiscoveredBusiness> discoverViaGooglePlacesApi(String keyword, String location) {
        String apiKey = appSettings.getGooglePlacesApiKey();
        String query = keyword + " " + location;
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
                   + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                   + "&key=" + apiKey;

        // Use RestTemplate or HttpClient to call the API
        // Parse JSON response
        // For each result in response.results:
        //   - extract name → businessName
        //   - extract formatted_address → address
        //   - extract formatted_phone_number → phoneNumber (needs Place Details call)
        //   - extract website → websiteUrl (needs Place Details call)

        // Note: Basic text search returns name + address only.
        // For phone + website, make a second call to Place Details API:
        // GET https://maps.googleapis.com/maps/api/place/details/json?place_id={place_id}&fields=name,formatted_phone_number,website,formatted_address&key={apiKey}

        List<DiscoveredBusiness> businesses = new ArrayList<>();
        // ... implementation here
        return businesses;
    }

    // Option B: Playwright Scraper
    private List<DiscoveredBusiness> discoverViaPlaywright(String keyword, String location) {
        // Use existing Playwright integration
        // Navigate to: https://www.google.com/maps/search/{keyword}+{location}
        // Wait for results to load
        // Extract each listing:
        //   - div[class*='Nv2PK'] or similar → each business card
        //   - Business name: h3 or span with business name class
        //   - Address: look for address span
        //   - Phone: look for phone span
        //   - Website link: look for website button href

        // Note: Google Maps HTML structure changes frequently.
        // Selectors may need updating over time.
        // Recommended selectors to try (verify in browser):
        //   Business cards: div.Nv2PK
        //   Name: div.fontHeadlineSmall
        //   Address: button[data-tooltip="Copy address"] span
        //   Phone: button[data-tooltip="Copy phone number"] span
        //   Website: a[data-value="Website"]

        List<DiscoveredBusiness> businesses = new ArrayList<>();
        // ... implementation using playwrightFetcher
        return businesses;
    }
}
```

---

### 3. New Service: `BusinessWebsiteScrapeService`

After getting the business list, scrape each `websiteUrl` to extract email and social media links.

Create: `src/main/java/com/browzwi/webscraper/service/BusinessWebsiteScrapeService.java`

```java
@Service
public class BusinessWebsiteScrapeService {

    private final HtmlFetcher htmlFetcher; // existing

    public void enrichBusinessData(DiscoveredBusiness business) {
        if (business.getWebsiteUrl() == null) return;

        String html = htmlFetcher.fetch(business.getWebsiteUrl());

        // Extract email addresses using regex
        // Pattern: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}
        String email = extractEmail(html);
        business.setEmailAddress(email);

        // Extract social media links
        // Look for href containing: facebook.com, twitter.com, instagram.com,
        //                           linkedin.com, tiktok.com, youtube.com
        List<String> socialLinks = extractSocialLinks(html);
        business.setSocialMediaLinks(String.join(",", socialLinks));

        business.setStatus("SCRAPED");
    }

    private String extractEmail(String html) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private List<String> extractSocialLinks(String html) {
        List<String> links = new ArrayList<>();
        List<String> socialDomains = List.of(
            "facebook.com", "instagram.com", "twitter.com",
            "linkedin.com", "tiktok.com", "youtube.com"
        );
        // Parse href attributes from <a> tags, filter by socialDomains
        // Use Jsoup (already likely in project) to parse HTML
        Document doc = Jsoup.parse(html);
        doc.select("a[href]").forEach(el -> {
            String href = el.attr("abs:href");
            socialDomains.forEach(domain -> {
                if (href.contains(domain)) links.add(href);
            });
        });
        return links;
    }
}
```

---

### 4. New Controller: `DiscoveryController`

Create: `src/main/java/com/browzwi/webscraper/web/DiscoveryController.java`

```java
@Controller
@RequestMapping("/discovery")
public class DiscoveryController {

    private final UrlDiscoveryService urlDiscoveryService;
    private final BusinessWebsiteScrapeService websiteScrapeService;
    private final DiscoveryJobRepository discoveryJobRepository;

    // Show discovery page
    @GetMapping
    public String showDiscoveryPage(Model model) {
        model.addAttribute("jobs", discoveryJobRepository.findAll());
        return "discovery/index";
    }

    // Start a new discovery job (HTMX compatible)
    @PostMapping("/start")
    public String startDiscovery(@RequestParam String keyword,
                                  @RequestParam String location,
                                  Model model) {
        DiscoveryJob job = urlDiscoveryService.discover(keyword, location);

        // After discovery, enrich each business with email/social
        job.getBusinesses().forEach(websiteScrapeService::enrichBusinessData);

        model.addAttribute("job", job);
        return "discovery/results :: resultsFragment"; // HTMX partial
    }

    // View results of a discovery job
    @GetMapping("/{jobId}/results")
    public String viewResults(@PathVariable Long jobId, Model model) {
        DiscoveryJob job = discoveryJobRepository.findById(jobId).orElseThrow();
        model.addAttribute("job", job);
        return "discovery/results";
    }

    // Export to CSV/Excel
    @GetMapping("/{jobId}/export")
    public ResponseEntity<byte[]> exportResults(@PathVariable Long jobId) {
        // Generate CSV with columns:
        // Business Name, Address, Email, Social Media Links, Contact Number
        // Return as downloadable file
    }
}
```

---

### 5. Settings Update

Add new settings fields to existing settings:

In `AppSettings` entity or settings service, add:
```java
private String discoverySource;    // "GOOGLE_PLACES_API" or "PLAYWRIGHT"
private String googlePlacesApiKey; // Only needed if using Google Places API
```

Update the Settings UI page (`templates/settings/index.html`) to include:
- Dropdown: Discovery Source (Google Places API / Playwright)
- Text field: Google Places API Key (shown only when API option is selected)

---

### 6. New Thymeleaf Templates

#### `templates/discovery/index.html`
- Form with two fields: **Keyword** and **Location**
- Button: "Discover Businesses"
- Table below showing past discovery jobs
- Use HTMX: `hx-post="/discovery/start"` `hx-target="#results"`

#### `templates/discovery/results.html`
- Table with columns: Business Name, Address, Email, Social Media, Phone
- Export button: "Download as Excel/CSV"
- Button per row: "Scrape Website" (to manually trigger enrichment)

---

### 7. Navigation Update

Add **"Discovery"** link to the left sidebar navigation (in `templates/fragments/sidebar.html` or equivalent).

---

## Full Flow Summary

```
User types keyword + location
        ↓
DiscoveryController.startDiscovery()
        ↓
UrlDiscoveryService.discover()
        ↓ (based on settings)
   [Option A]                    [Option B]
Google Places API         Playwright scrapes Google Maps
        ↓                          ↓
   List<DiscoveredBusiness> with name, address, phone, websiteUrl
        ↓
BusinessWebsiteScrapeService.enrichBusinessData()
   - Scrape each websiteUrl
   - Extract email via regex
   - Extract social media links
        ↓
Save to database
        ↓
Show results table to user
        ↓
Export to Excel/CSV
```

---

## Dependencies to Add in `pom.xml`

```xml
<!-- Jsoup for HTML parsing (if not already present) -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>

<!-- Apache POI for Excel export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

---

## Important Notes for Implementation

1. **Google Places API Key:** Get from [Google Cloud Console](https://console.cloud.google.com). Enable "Places API". Free tier gives $200/month credit.

2. **Playwright Selectors for Google Maps:** Google frequently changes their HTML structure. The selectors provided are approximate — verify them in Chrome DevTools before implementing.

3. **Rate Limiting:** Add delays between website scrapes (at least 1-2 seconds) to avoid being blocked.

4. **Async Processing:** For large results (50+ businesses), run the enrichment (`BusinessWebsiteScrapeService`) asynchronously using `@Async` to avoid request timeout.

5. **Email Extraction Limitation:** Not all websites display emails in plain text. Some use contact forms. The regex approach will work for most but not all.

6. **Export Format:** Use Apache POI to generate `.xlsx` file with the 5 columns matching the required spreadsheet format.

---

## Files to Create Summary

| File | Type | Purpose |
|------|------|---------|
| `domain/DiscoveryJob.java` | Entity | Stores discovery job info |
| `domain/DiscoveredBusiness.java` | Entity | Stores each discovered business |
| `repository/DiscoveryJobRepository.java` | Repository | DB access for DiscoveryJob |
| `repository/DiscoveredBusinessRepository.java` | Repository | DB access for DiscoveredBusiness |
| `service/UrlDiscoveryService.java` | Service | Google Maps / Playwright discovery logic |
| `service/BusinessWebsiteScrapeService.java` | Service | Email + social media extraction |
| `web/DiscoveryController.java` | Controller | Handles HTTP requests |
| `templates/discovery/index.html` | Template | Discovery form + job list UI |
| `templates/discovery/results.html` | Template | Results table + export button |
| `db/changelog/changes/add-discovery-tables.xml` | Migration | Database schema changes |

---

## Files to Modify Summary

| File | Change |
|------|--------|
| `domain/AppSettings.java` (or equivalent) | Add discoverySource + googlePlacesApiKey fields |
| `templates/settings/index.html` | Add discovery settings fields |
| `templates/fragments/sidebar.html` | Add Discovery nav link |
| `pom.xml` | Add Jsoup + Apache POI dependencies |

---

*This document is intended to be read by an AI assistant to implement the described feature into the existing WebScraper Spring Boot codebase.*
