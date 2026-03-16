# WebScraper Project - Current Status

**Last Updated:** March 16, 2026
**Version:** 1.0.0

---

## Feature Status

| Feature | Status | Notes |
|---------|--------|-------|
| Recipe Builder (YAML) | ✅ Working | CSS/XPath selectors, multi-page support |
| Recipe Live Test | ✅ Working | HTMX partial result rendering |
| Scrape Jobs | ✅ Working | Manual + Quartz-scheduled execution |
| Job Progress Tracking | ✅ Working | Per-target HTMX polling |
| HtmlUnit Fetcher | ✅ Working | Fast, no JS execution |
| Playwright Fetcher | ✅ Working | Full browser, JS support |
| File Storage | ✅ Working | `./data/{jobId}/{targetId}/` |
| URL Discovery (Google Maps) | ✅ Working | Playwright-based scraping |
| URL Discovery (Google Search) | ⚠️ Fragile | Selectors break when Google changes HTML |
| Business Email Extraction | ✅ Working | Visits website, parses mailto links |
| Social Media Link Extraction | ✅ Working | Facebook, Instagram, LinkedIn, etc. |
| Excel Export (Discovery) | ✅ Working | Apache POI |
| Settings Management | ✅ Working | Persisted in `app_settings` table |
| Claude AI Integration | ✅ Working | Optional; falls back to Playwright |
| Spring Security / Auth | ✅ Working | DB-backed users, BCrypt passwords |
| Liquibase Migrations | ✅ Working | H2 (test) + MySQL (dev/prod) |

---

## Project Statistics

| Component | Count |
|-----------|-------|
| Java Source Files | ~50 |
| Controllers | 7 |
| Services | 12 |
| Domain Entities | 11 |
| Repositories | 8 |
| Thymeleaf Templates | 30 |
| Liquibase Changelogs | 5 |

---

## Package Summary

```
com.browzwi.webscraper/
├── web/            Controllers, DTOs, view models
├── service/        Business logic, Quartz jobs, settings, test sessions
├── scraper/        Engine, fetchers (HtmlUnit/Playwright), YAML models
├── domain/         JPA entities
├── repository/     Spring Data JPA interfaces
├── storage/        File persistence under ./data/
├── security/       Spring Security config, dev admin initializer
└── config/         Quartz configuration
```

See `SYSTEM_ARCHITECTURE_CURRENT.md` for the full package tree and module breakdown.

---

## Database Tables

| Table | Purpose |
|-------|---------|
| `users` | Authentication |
| `app_settings` | Key/value runtime settings |
| `scraper_recipes` | YAML recipe definitions |
| `scrape_jobs` | Job metadata + schedule |
| `scrape_targets` | Per-URL targets within a job |
| `scrape_result_data` | Extracted JSON + file paths |
| `discovery_job` | Discovery run metadata |
| `discovered_business` | Per-business discovery results |

---

## Configuration

### `application.yml` (shared defaults)
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

### Runtime Settings (via `/settings` UI)

| Setting | Options |
|---------|---------|
| `scrapeFetcherType` | `HTMLUNIT`, `PLAYWRIGHT` |
| `discoverySourceType` | `GOOGLE_MAPS`, `GOOGLE_SEARCH`, `GMAPS_GOOGLE_SEARCH` |
| `claudeApiKey` | Anthropic API key (optional) |
| `googleSearchSiteFilter` | e.g. `facebook.com` (optional) |
| `playwrightHeadless` | `true` / `false` |
| `playwrightTimeout` | milliseconds |

---

## Known Limitations

1. **Google Search scraping is fragile** — Google's HTML structure changes frequently; selectors may stop working. Consider Google Custom Search API for production reliability.
2. **Rate limiting** — Aggressive Google Maps/Search scraping may trigger temporary blocks or CAPTCHAs.
3. **Email extraction accuracy** — Depends on whether the target website exposes mailto links.
4. **Playwright startup cost** — Browser launch adds latency; HtmlUnit is faster for static sites.

---

## Development Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.4+ (dev/prod) — H2 used automatically for tests
- Node.js (optional, for Tailwind CSS recompilation)

### Run (dev profile)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8080
# Default login: admin@cvsu.edu.ph / admin123
```

### Run Tests
```bash
./mvnw test -q
```

### Production Build
```bash
./mvnw clean package -Pproduction
java -jar target/webscraper-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

---

## Documentation Files

| File | Purpose |
|------|---------|
| `README.md` | Full feature guide and setup instructions |
| `SYSTEM_ARCHITECTURE_CURRENT.md` | Package structure, flows, DB schema, endpoints |
| `CHANGELOG.md` | Version history |
| `LOCAL_SETUP.md` | Local dev environment setup |
| `USAGE_GUIDE.md` | End-user guide |
| `CODEBASE_GUIDE.md` | Developer architecture guide |
| `docs/system-documentation.md` | Detailed system documentation |
