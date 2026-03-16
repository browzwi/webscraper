# Changelog

All notable changes to this project are documented here.

---

## [Unreleased]

---

## [1.0.0] - March 2026

### Added

#### URL Discovery System
- `DiscoveryController` — UI for creating and viewing discovery jobs
- `UrlDiscoveryService` — Orchestrates multi-source discovery
- `GoogleSearchDiscoveryService` — Google Search scraping via Playwright with optional Claude AI
- `AntiDetectPlaywrightService` — Anti-detection Playwright wrapper for Google Maps scraping
- `BusinessWebsiteScrapeService` — Extracts email addresses and social media links from business websites
- `BraveSearchService` — Brave Search integration (alternative search source)
- `DiscoveryJob` entity + `DiscoveryJobRepository`
- `DiscoveredBusiness` entity + `DiscoveredBusinessRepository`
- `DiscoverySourceType` enum: `GOOGLE_MAPS`, `GOOGLE_SEARCH`, `GMAPS_GOOGLE_SEARCH`
- `TargetPageType` enum for filtering discovery targets
- Liquibase: `add-discovery-tables.xml` — `discovery_job` and `discovered_business` tables
- Liquibase: `007-add-target-page-type-fields.yaml` — `target_page_type` column on `discovered_business`
- Templates: `discovery/index.html`, `discovery/progress.html`, `discovery/results.html`
- Excel export of discovery results via Apache POI

#### Settings System
- `SettingsController` — View and update runtime settings
- `SettingsService` — Reads/writes `app_settings` table
- `SettingsForm` DTO
- `ScrapeFetcherType` enum: `HTMLUNIT`, `PLAYWRIGHT`
- Runtime settings: `scrapeFetcherType`, `discoverySourceType`, `claudeApiKey`, `googleSearchSiteFilter`, `playwrightHeadless`, `playwrightTimeout`
- Template: `settings/index.html`

#### AI Integration
- Anthropic Claude SDK dependency (`anthropic-java 0.1.0`)
- Optional Claude AI for enhanced Google Search result extraction
- Configurable via `claudeApiKey` setting; falls back to Playwright if not set

### Core Scraping Platform (Initial Release)

#### Recipes
- `RecipeController` — CRUD + live test
- `ScraperRecipeService`
- `RecipeForm`, `RecipeExample` DTOs
- YAML recipe model: `RecipeConfig`, `PageConfig`, `SubPageConfig`, `FieldConfig`, `OptionsConfig`, `MatchConfig`, `DataSourceType`
- `RecipeTestSession` + `RecipeTestSessionService` — In-memory test session management
- Templates: `recipes/list.html`, `recipes/form.html`, `recipes/test-result.html`

#### Jobs
- `ScrapeJobController` — CRUD, start, pause, target drill-down
- `ScrapeJobService` — Job lifecycle management
- `ScrapeJobSchedulerService` — Quartz job registration
- `ScrapeQuartzJob` — Quartz job implementation
- `ScrapeTargetArchiveService` — Target result archiving
- `JobForm` DTO
- View models: `ScrapeJobDetailView`, `ScrapeJobListItemView`, `ScrapeTargetDetailsView`, `ScrapeTargetProgressGroupView`, `ScrapeTargetProgressView`, `ScrapeTargetRowView`
- Templates: `jobs/list.html`, `jobs/form.html`, `jobs/detail.html`, `jobs/target-details.html`, `jobs/target-progress.html`, `jobs/target-progress-modal.html`, `jobs/job-archive-modal.html`

#### Scraper Engine
- `ScraperEngine` — Orchestrates fetch → extract → store pipeline
- `PageFetcher` interface
- `HtmlFetcher` — HtmlUnit 2.70.0 implementation
- `PlaywrightFetcher` — Playwright 1.46.0 implementation
- `HtmlProcessingService` — Jsoup-based HTML cleaning
- `FieldExtractionService` — CSS/XPath field extraction
- `MarkdownConversionService` — Flexmark HTML-to-Markdown
- `MultiPageScrapeResult` — Multi-page result aggregation

#### Storage
- `FileStorageService` — Manages `./data/{jobId}/{targetId}/` with `raw.html`, `processed.html`, `processed.md`
- `FileStorageProperties` — `webscraper.storage.root` config binding

#### Domain Entities
- `User`, `UserRole`
- `AppSetting`
- `ScraperRecipe`
- `ScrapeJob`, `ScrapeJobStatus`
- `ScrapeTarget`, `ScrapeTargetStatus`
- `ScrapeResultData`

#### Security
- `SecurityConfig` — Spring Security with form login
- `DatabaseUserDetailsService` — DB-backed user authentication
- `DevAdminPasswordInitializer` — Auto-resets dev admin password on startup

#### Infrastructure
- `QuartzConfig` — Quartz scheduler configuration
- `GlobalControllerAdvice` — Global model attributes
- Liquibase: `db.changelog-1.0.yaml` — Core schema (users, recipes, jobs, targets, results, app_settings)
- Liquibase: `006-add-progress-json-changelog.yaml` — `progress_json` on `scrape_targets`
- Spring profiles: `application.yml` (shared), `application-dev.yml` (MySQL), `application-local.yml`, test `application.yml` (H2)

#### Frontend
- `layout.html` — Base layout with Tailwind CSS, HTMX 1.9.12, CSRF injection
- Dashboard, login, error pages
- Reusable fragments: `recipe-test-result.html`, `target-detail.html`, `example-recipes-modal.html`

### Dependencies
- Java 17
- Spring Boot 3.5.7 (MVC, Security, Data JPA, Quartz)
- MySQL 8.4 (prod) / H2 (test)
- Liquibase
- Playwright 1.46.0
- HtmlUnit 2.70.0
- Jsoup 1.18.1
- Flexmark 0.64.8
- Apache POI 5.2.5
- Anthropic Claude SDK 0.1.0
- Thymeleaf + HTMX 1.9.12 + Tailwind CSS
