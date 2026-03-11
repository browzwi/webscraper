# WebScraper Project - Changelog & Current Status

**Last Updated:** March 10, 2026  
**Version:** 1.0.0

---

## Latest Changes (March 10, 2026)

### ✅ Implemented Features

#### 1. URL Discovery System
- **Three discovery sources supported:**
  - `GOOGLE_MAPS` - Direct Google Maps scraping via Playwright
  - `GOOGLE_SEARCH` - Google Search results via Playwright
  - `GMAPS_GOOGLE_SEARCH` - Combined approach (Google Maps + Google Search)

- **Discovery Services:**
  - `UrlDiscoveryService` - Main orchestration service
  - `GoogleSearchDiscoveryService` - Google Search scraping with Claude AI integration
  - `PlaywrightFetcher` - Browser automation for dynamic content

#### 2. Business Data Extraction
- **Discovered Business Entity Fields:**
  - `businessName` - Business name from Google Maps
  - `address` - Full business address
  - `phoneNumber` - Contact number
  - `websiteUrl` - Business website URL
  - `emailAddress` - Email extracted from website
  - `socialMediaLinks` - Facebook, Instagram, LinkedIn, etc.
  - `status` - DISCOVERED, PROCESSED, FAILED

#### 3. Discovery Job Management
- **DiscoveryJob Entity:**
  - `keyword` - Search keyword (e.g., "accounting firms")
  - `location` - Target location (e.g., "Cavite, Philippines")
  - `discoverySource` - Which source was used
  - `status` - PENDING, RUNNING, COMPLETED, FAILED
  - `resultsCount` - Number of businesses discovered
  - `createdAt` / `completedAt` - Timestamps

#### 4. AI Integration
- **Claude AI Support:**
  - Optional Claude API integration for enhanced search
  - Configurable via Settings
  - Falls back to Playwright scraping if no API key

#### 5. Settings System
- **Configurable Options:**
  - Discovery source type (GOOGLE_MAPS, GOOGLE_SEARCH, GMAPS_GOOGLE_SEARCH)
  - Claude API key
  - Google search site filter
  - Playwright browser settings (headless, timeout)

---

## Project Statistics

| Metric | Count |
|--------|-------|
| **Java Files** | 87 |
| **Controllers** | 8 |
| **Services** | 15+ |
| **Domain Entities** | 12 |
| **Repositories** | 10 |
| **Thymeleaf Templates** | 20+ |

---

## Current Package Structure

```
com.browzwi.webscraper/
├── WebScraperApplication.java
├── web/
│   ├── DashboardController.java
│   ├── DiscoveryController.java         # URL discovery UI
│   ├── RecipeController.java
│   ├── ScrapeJobController.java
│   ├── SettingsController.java          # App settings
│   ├── LoginController.java
│   ├── UtilityController.java
│   └── dto/
│       ├── SettingsForm.java
│       └── ... (other DTOs)
├── service/
│   ├── UrlDiscoveryService.java         # Main discovery logic
│   ├── GoogleSearchDiscoveryService.java # Google Search scraping
│   ├── ScrapeJobService.java
│   ├── ScrapeJobSchedulerService.java
│   ├── ScraperRecipeService.java
│   ├── settings/
│   │   ├── SettingsService.java
│   │   ├── DiscoverySourceType.java
│   │   └── AppSetting.java
│   └── job/
│       └── (Quartz job classes)
├── scraper/
│   ├── service/
│   │   ├── ScraperEngine.java
│   │   ├── HtmlFetcher.java
│   │   ├── PlaywrightFetcher.java
│   │   ├── HtmlProcessingService.java
│   │   ├── FieldExtractionService.java
│   │   └── MarkdownConversionService.java
│   └── model/
│       ├── RecipeConfig.java
│       ├── PageConfig.java
│       ├── FieldConfig.java
│       └── ... (YAML models)
├── domain/
│   ├── User.java
│   ├── UserRole.java
│   ├── ScraperRecipe.java
│   ├── ScrapeJob.java
│   ├── ScrapeTarget.java
│   ├── ScrapeResultData.java
│   ├── DiscoveryJob.java               # NEW: Discovery jobs
│   ├── DiscoveredBusiness.java         # NEW: Discovered businesses
│   └── AppSetting.java
├── repository/
│   ├── UserRepository.java
│   ├── ScraperRecipeRepository.java
│   ├── ScrapeJobRepository.java
│   ├── ScrapeTargetRepository.java
│   ├── ScrapeResultDataRepository.java
│   ├── DiscoveryJobRepository.java     # NEW
│   ├── DiscoveredBusinessRepository.java # NEW
│   └── AppSettingRepository.java
└── security/
    └── (Security configuration)
```

---

## Database Schema

### New Tables (March 2026)

#### `discovery_job`
```sql
CREATE TABLE discovery_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    discovery_source VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    results_count INT,
    created_at DATETIME,
    completed_at DATETIME
);
```

#### `discovered_business`
```sql
CREATE TABLE discovered_business (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone_number VARCHAR(100),
    website_url VARCHAR(500),
    email_address VARCHAR(255),
    social_media_links TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DISCOVERED',
    discovery_job_id BIGINT NOT NULL,
    FOREIGN KEY (discovery_job_id) REFERENCES discovery_job(id)
);
```

---

## Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.5.7**
  - Spring MVC
  - Spring Security
  - Spring Data JPA
  - Spring Quartz
- **MySQL 8.4** (Production)
- **H2** (Development/Testing)
- **Liquibase** (Schema migrations)

### Scraping & Processing
- **Playwright 1.46.0** - Browser automation
- **HtmlUnit 2.70.0** - Headless browser
- **Jsoup 1.18.1** - HTML parsing
- **Flexmark 0.64.8** - Markdown conversion
- **Apache POI 5.2.5** - Excel export

### Frontend
- **Thymeleaf** - Server-side templates
- **HTMX 1.9.12** - Dynamic updates
- **Tailwind CSS** - Styling

### AI Integration
- **Anthropic Claude API** - AI-powered search

---

## Key Configuration Options

### `application.yml`
```yaml
webscraper:
  storage:
    root: ./data
  fetcher:
    playwright:
      browser: chromium
      headless: true
      timeout: 20000
```

### Settings (via SettingsController)
- `discoverySourceType` - GOOGLE_MAPS, GOOGLE_SEARCH, or GMAPS_GOOGLE_SEARCH
- `claudeApiKey` - Anthropic API key for AI features
- `googleSearchSiteFilter` - Optional site filter for Google Search
- `playwrightHeadless` - Run browser in headless mode
- `playwrightTimeout` - Request timeout in milliseconds

---

## API Endpoints

### Discovery
- `GET /discovery` - Discovery job list
- `POST /discovery` - Create new discovery job
- `GET /discovery/{id}` - View discovery job details
- `POST /discovery/{id}/convert-to-recipe` - Convert discovered URLs to scraping recipe

### Settings
- `GET /settings` - View settings
- `POST /settings` - Update settings

### Scraping
- `GET /recipes` - List scraping recipes
- `POST /recipes` - Create recipe
- `GET /jobs` - List scrape jobs
- `POST /jobs` - Create scrape job

---

## Usage Examples

### Create Discovery Job (via UI)
1. Navigate to `/discovery`
2. Enter keyword (e.g., "accounting firms")
3. Enter location (e.g., "Cavite, Philippines")
4. Select discovery source
5. Click "Discover"

### Create Discovery Job (via API)
```bash
POST /discovery
Content-Type: application/x-www-form-urlencoded

keyword=accounting+firms&location=Cavite,+Philippines&maxResults=100
```

### Convert to Scraping Recipe
After discovery, convert businesses to scrape targets:
1. View discovery job details
2. Click "Convert to Recipe"
3. Configure extraction fields
4. Save as new recipe

---

## Known Issues & Limitations

1. **Google Maps Scraping:**
   - May be rate-limited by Google
   - Consider using official Google Places API for production

2. **Playwright Performance:**
   - Browser automation is slower than API calls
   - Headless mode recommended for production

3. **Email Extraction:**
   - Requires visiting each business website
   - Success rate varies by website structure

---

## Future Enhancements

- [ ] Official Google Places API integration
- [ ] Bing Maps as alternative discovery source
- [ ] Batch email extraction service
- [ ] Social media profile scraper
- [ ] Export to CSV/Excel directly from discovery
- [ ] Scheduled discovery jobs
- [ ] Duplicate detection for discovered businesses

---

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.4+
- Node.js 18+ (for Tailwind CSS)

### Quick Start
```bash
# Clone repository
cd webscraper-main

# Install dependencies
./mvnw clean install

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Access UI
open http://localhost:8080
```

### Default Credentials (dev profile)
- **Username:** `admin@cvsu.edu.ph`
- **Password:** `admin123`

---

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=UrlDiscoveryServiceTest

# Run with coverage
./mvnw clean test jacoco:report
```

---

## Build & Deployment

```bash
# Build JAR
./mvnw clean package

# Run JAR
java -jar target/webscraper-0.0.1-SNAPSHOT.jar

# Production build
./mvnw clean package -Pproduction
```

---

## Contact & Support

For issues or questions, refer to:
- `README.md` - General documentation
- `LOCAL_SETUP.md` - Local development setup
- `USAGE_GUIDE.md` - User guide
- `CODEBASE_GUIDE.md` - Code architecture guide
