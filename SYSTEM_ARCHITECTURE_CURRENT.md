# WebScraper System - Current Architecture

**Last Updated:** March 16, 2026

---

## High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                      WebScraper Platform                          │
│                                                                   │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐           │
│  │  Discovery  │   │   Recipes   │   │    Jobs     │           │
│  │  /discovery │   │  /recipes   │   │   /jobs     │           │
│  │             │   │             │   │             │           │
│  │ Google Maps │   │ YAML-based  │   │ Quartz      │           │
│  │ Google      │   │ CSS/XPath   │   │ Scheduled   │           │
│  │ Search      │   │ selectors   │   │ Execution   │           │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘           │
│         │                 │                  │                   │
│         ▼                 ▼                  ▼                   │
│  [discovery_job]   [scraper_recipes]   [scrape_jobs]            │
│  [discovered_      [app_settings]      [scrape_targets]         │
│   business]                            [scrape_result_data]     │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Settings  /settings                    │   │
│  │  Fetcher type · Discovery source · Claude API key ·      │   │
│  │  Playwright config · Google search site filter           │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.browzwi.webscraper/
├── WebScraperApplication.java
│
├── web/                                  # Controllers & view models
│   ├── DashboardController.java
│   ├── DiscoveryController.java
│   ├── RecipeController.java
│   ├── ScrapeJobController.java
│   ├── SettingsController.java
│   ├── LoginController.java
│   ├── UtilityController.java
│   ├── GlobalControllerAdvice.java
│   ├── dto/
│   │   ├── JobForm.java
│   │   ├── RecipeForm.java
│   │   ├── RecipeExample.java
│   │   └── SettingsForm.java
│   └── view/                             # Read-only view models (type-safe)
│       ├── ScrapeJobDetailView.java
│       ├── ScrapeJobListItemView.java
│       ├── ScrapeTargetDetailsView.java
│       ├── ScrapeTargetProgressGroupView.java
│       ├── ScrapeTargetProgressView.java
│       └── ScrapeTargetRowView.java
│
├── service/
│   ├── ScrapeJobService.java
│   ├── ScrapeJobSchedulerService.java
│   ├── ScraperRecipeService.java
│   ├── ScrapeTargetArchiveService.java
│   ├── UrlDiscoveryService.java
│   ├── GoogleSearchDiscoveryService.java
│   ├── BusinessWebsiteScrapeService.java
│   ├── AntiDetectPlaywrightService.java
│   ├── BraveSearchService.java
│   ├── job/
│   │   └── ScrapeQuartzJob.java
│   ├── settings/
│   │   ├── SettingsService.java
│   │   ├── ScrapeFetcherType.java        # HTMLUNIT | PLAYWRIGHT
│   │   ├── DiscoverySourceType.java      # GOOGLE_MAPS | GOOGLE_SEARCH | GMAPS_GOOGLE_SEARCH
│   │   └── TargetPageType.java
│   └── test/
│       ├── RecipeTestSession.java
│       └── RecipeTestSessionService.java
│
├── scraper/
│   ├── service/
│   │   ├── ScraperEngine.java            # Orchestrates fetch + extract + store
│   │   ├── PageFetcher.java              # Interface
│   │   ├── HtmlFetcher.java              # HtmlUnit implementation
│   │   ├── PlaywrightFetcher.java        # Playwright implementation
│   │   ├── HtmlProcessingService.java
│   │   ├── FieldExtractionService.java
│   │   ├── MarkdownConversionService.java
│   │   └── MultiPageScrapeResult.java
│   └── model/                            # YAML recipe deserialization
│       ├── RecipeConfig.java
│       ├── PageConfig.java
│       ├── SubPageConfig.java
│       ├── FieldConfig.java
│       ├── OptionsConfig.java
│       ├── MatchConfig.java
│       └── DataSourceType.java
│
├── domain/                               # JPA entities
│   ├── User.java
│   ├── UserRole.java
│   ├── AppSetting.java
│   ├── ScraperRecipe.java
│   ├── ScrapeJob.java
│   ├── ScrapeJobStatus.java
│   ├── ScrapeTarget.java
│   ├── ScrapeTargetStatus.java
│   ├── ScrapeResultData.java
│   ├── DiscoveryJob.java
│   └── DiscoveredBusiness.java
│
├── repository/                           # Spring Data JPA interfaces
│   ├── UserRepository.java
│   ├── AppSettingRepository.java
│   ├── ScraperRecipeRepository.java
│   ├── ScrapeJobRepository.java
│   ├── ScrapeTargetRepository.java
│   ├── ScrapeResultDataRepository.java
│   ├── DiscoveryJobRepository.java
│   └── DiscoveredBusinessRepository.java
│
├── storage/
│   ├── FileStorageService.java           # Manages ./data/{jobId}/{targetId}/
│   ├── FileStorageProperties.java
│   └── StorageException.java
│
├── security/
│   ├── SecurityConfig.java
│   ├── DatabaseUserDetailsService.java
│   └── DevAdminPasswordInitializer.java
│
└── config/
    └── QuartzConfig.java
```

---

## Module Breakdown

### 1. Discovery Module

**Entry point:** `GET/POST /discovery`

**Flow:**
```
DiscoveryController
    └── UrlDiscoveryService
            ├── AntiDetectPlaywrightService  (Google Maps scraping)
            ├── GoogleSearchDiscoveryService (Google Search scraping)
            └── BusinessWebsiteScrapeService (email + social links from website)
                    └── PlaywrightFetcher
```

**Discovery sources (configured in Settings):**
- `GOOGLE_MAPS` — Playwright scrapes Google Maps for business listings
- `GOOGLE_SEARCH` — Playwright scrapes Google Search results
- `GMAPS_GOOGLE_SEARCH` — Both sources combined

**Output stored in:**
- `discovery_job` — Job metadata (keyword, location, status, count)
- `discovered_business` — Per-business: name, address, phone, website, email, social links

**Templates:**
- `discovery/index.html` — Search form
- `discovery/progress.html` — Live progress (HTMX polling)
- `discovery/results.html` — Results table + Excel export

---

### 2. Recipe Module

**Entry point:** `GET/POST /recipes`

**What it does:** Stores YAML scraping recipes that define CSS/XPath selectors and field extraction rules.

**YAML model classes (`scraper/model/`):**
- `RecipeConfig` — Top-level: name, baseUrl, fields, subPages, options
- `PageConfig` — Per-page field list
- `SubPageConfig` — Sub-page navigation config
- `FieldConfig` — Selector, type (TEXT/HREF/ATTRIBUTE/HTML), attribute name
- `OptionsConfig` — fetcherType, waitForLoad, timeout
- `MatchConfig` — Pattern matching rules

**Templates:**
- `recipes/list.html`
- `recipes/form.html` — YAML editor with live test
- `recipes/test-result.html` — HTMX partial for test output

---

### 3. Jobs Module

**Entry point:** `GET/POST /jobs`

**Flow:**
```
ScrapeJobController
    └── ScrapeJobService
            ├── ScrapeJobSchedulerService  (Quartz registration)
            └── ScrapeQuartzJob (Quartz job)
                    └── ScraperEngine
                            ├── PageFetcher (HtmlUnit or Playwright)
                            ├── HtmlProcessingService
                            ├── FieldExtractionService
                            ├── MarkdownConversionService
                            └── FileStorageService  → ./data/{jobId}/{targetId}/
```

**Scraper engine selects fetcher at runtime** based on `SettingsService.getScrapeFetcherType()`:
- `HTMLUNIT` → `HtmlFetcher` (fast, no JS)
- `PLAYWRIGHT` → `PlaywrightFetcher` (full browser, JS support)

**File output per target:**
```
./data/{jobId}/{targetId}/
    ├── raw.html
    ├── processed.html
    └── processed.md
```

**Templates:**
- `jobs/list.html`
- `jobs/form.html`
- `jobs/detail.html`
- `jobs/target-details.html` — HTMX partial
- `jobs/target-progress.html` — HTMX polling partial

---

### 4. Settings Module

**Entry point:** `GET/POST /settings`

**Persisted in `app_settings` table (key/value):**

| Key | Values | Description |
|-----|--------|-------------|
| `scrapeFetcherType` | `HTMLUNIT`, `PLAYWRIGHT` | Scraping engine |
| `discoverySourceType` | `GOOGLE_MAPS`, `GOOGLE_SEARCH`, `GMAPS_GOOGLE_SEARCH` | Discovery source |
| `claudeApiKey` | string | Anthropic API key (optional) |
| `googleSearchSiteFilter` | string | e.g. `facebook.com` |
| `playwrightHeadless` | `true`/`false` | Headless browser mode |
| `playwrightTimeout` | integer (ms) | Page load timeout |

---

## Database Schema

### Core Scraping Tables

```sql
scraper_recipes
  id CHAR(36) PK
  name VARCHAR
  yaml_content TEXT
  created_at DATETIME

scrape_jobs
  id CHAR(36) PK
  job_name VARCHAR
  recipe_id CHAR(36) FK → scraper_recipes
  status VARCHAR          -- PENDING, RUNNING, COMPLETED, FAILED
  cron_expression VARCHAR
  created_at DATETIME

scrape_targets
  id CHAR(36) PK
  job_id CHAR(36) FK → scrape_jobs
  url VARCHAR
  status VARCHAR          -- PENDING, RUNNING, COMPLETED, FAILED
  progress_json TEXT

scrape_result_data
  id CHAR(36) PK
  target_id CHAR(36) FK → scrape_targets
  json_data TEXT
  raw_html_path VARCHAR
  processed_html_path VARCHAR
  processed_md_path VARCHAR
```

### Discovery Tables

```sql
discovery_job
  id BIGINT PK
  keyword VARCHAR
  location VARCHAR
  discovery_source VARCHAR   -- GOOGLE_MAPS | GOOGLE_SEARCH | GMAPS_GOOGLE_SEARCH
  status VARCHAR             -- PENDING, RUNNING, COMPLETED, FAILED
  results_count INT
  created_at DATETIME
  completed_at DATETIME

discovered_business
  id BIGINT PK
  discovery_job_id BIGINT FK → discovery_job
  business_name VARCHAR
  address VARCHAR
  phone_number VARCHAR
  website_url VARCHAR
  email_address VARCHAR
  social_media_links TEXT    -- JSON map of platform → URL
  target_page_type VARCHAR
  status VARCHAR             -- DISCOVERED, PROCESSED, FAILED
```

### Auth & Settings Tables

```sql
users
  id CHAR(36) PK
  username VARCHAR
  password VARCHAR (BCrypt)
  role VARCHAR

app_settings
  id CHAR(36) PK
  setting_key VARCHAR UNIQUE
  setting_value TEXT
```

---

## Liquibase Changelogs

| File | Description |
|------|-------------|
| `db.changelog-1.0.yaml` | Core schema: users, recipes, jobs, targets, results, app_settings |
| `006-add-progress-json-changelog.yaml` | Adds `progress_json` to `scrape_targets` |
| `changes/add-discovery-tables.xml` | Adds `discovery_job` and `discovered_business` |
| `007-add-target-page-type-fields.yaml` | Adds `target_page_type` to `discovered_business` |

---

## Thymeleaf Templates

```
templates/
├── layout.html                    # Base layout (Tailwind, HTMX, CSRF)
├── dashboard.html
├── login.html
├── error.html
├── error/403.html
├── fragments/
│   ├── head.html
│   ├── recipe-test-result.html    # HTMX partial
│   ├── target-detail.html         # HTMX partial
│   ├── example-recipes-modal.html
│   ├── server-time.html
│   └── fragments.html
├── recipes/
│   ├── list.html
│   ├── form.html
│   └── test-result.html
├── jobs/
│   ├── list.html
│   ├── form.html
│   ├── detail.html
│   ├── target-details.html
│   ├── target-progress.html
│   ├── target-progress-modal.html
│   └── job-archive-modal.html
├── discovery/
│   ├── index.html
│   ├── progress.html
│   └── results.html
└── settings/
    └── index.html
```

---

## Design Patterns

| Pattern | Where Used |
|---------|-----------|
| Strategy | `PageFetcher` interface → `HtmlFetcher` / `PlaywrightFetcher` selected at runtime |
| DTO/Form | `RecipeForm`, `JobForm`, `SettingsForm` decouple views from entities |
| View Model | `ScrapeJob*View` classes provide type-safe, render-ready data to templates |
| Layered MVC | Controllers → Services → Repositories; no repo access from controllers |
| Quartz Scheduler | `ScrapeJobSchedulerService` registers `ScrapeQuartzJob` for cron-based execution |
| Template Composition | All views extend `layout.html` via Thymeleaf fragments |

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/` | Dashboard |
| GET/POST | `/recipes` | List / create recipes |
| GET/POST | `/recipes/{id}` | View / update recipe |
| DELETE | `/recipes/{id}` | Delete recipe |
| POST | `/recipes/{id}/test` | Test recipe (HTMX) |
| GET/POST | `/jobs` | List / create jobs |
| GET | `/jobs/{id}` | Job detail |
| POST | `/jobs/{id}/start` | Start job |
| POST | `/jobs/{id}/pause` | Pause job |
| GET | `/jobs/{id}/targets/{tid}` | Target detail (HTMX) |
| GET/POST | `/discovery` | List / create discovery jobs |
| GET | `/discovery/{id}` | Discovery job detail |
| GET | `/discovery/{id}/progress` | Progress polling (HTMX) |
| GET | `/discovery/{id}/results` | Results view |
| GET/POST | `/settings` | View / update settings |
