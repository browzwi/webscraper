# WebScraper Codebase Guide

A comprehensive guide to understanding the WebScraper application architecture, components, and data flow.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Request Flow](#request-flow)
- [Recipe YAML Structure](#recipe-yaml-structure)
- [Database Schema](#database-schema)
- [Key Components Explained](#key-components-explained)
- [Design Patterns](#design-patterns)
- [Important Files to Study](#important-files-to-study)
- [Package Structure](#package-structure)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        WebScraper Application                    │
├─────────────────────────────────────────────────────────────────┤
│  Controllers (web/)                                              │
│  ├── DashboardController    - Main dashboard view               │
│  ├── RecipeController       - CRUD for scraping recipes         │
│  ├── ScrapeJobController    - Job management & scheduling       │
│  └── SettingsController     - App configuration                 │
│                                                                    │
│  Services (service/)                                             │
│  ├── ScraperRecipeService   - Recipe parsing & validation       │
│  ├── ScrapeJobService       - Job creation & management         │
│  ├── ScrapeJobSchedulerService - Quartz scheduling              │
│  └── ScraperRecipeService   - Recipe YAML handling              │
│                                                                    │
│  Scraper Engine (scraper/service/)                               │
│  ├── ScraperEngine          - Core scraping orchestration       │
│  ├── HtmlFetcher            - HtmlUnit-based fetching           │
│  ├── PlaywrightFetcher      - Full browser automation           │
│  ├── HtmlProcessingService  - HTML cleaning & processing        │
│  ├── FieldExtractionService - CSS selector data extraction     │
│  └── MarkdownConversionService - HTML to Markdown conversion   │
│                                                                    │
│  Repositories (repository/)                                       │
│  └── Spring Data JPA interfaces                                  │
│                                                                    │
│  Domain Entities (domain/)                                        │
│  ├── ScraperRecipe          - YAML recipe storage               │
│  ├── ScrapeJob              - Scheduled job definition          │
│  ├── ScrapeTarget           - Individual URL to scrape          │
│  └── ScrapeResultData       - Extracted data storage            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Request Flow

### Example: Creating & Running a Scraping Job

```
1. User submits job form (POST /jobs)
         ↓
2. ScrapeJobController.createJob()
         ↓
3. ScrapeJobService.createJob()
   ├── Validates recipe exists
   ├── Creates ScrapeJob entity
   ├── Creates ScrapeTarget for each URL
   └── Calls schedulerService.scheduleJob()
         ↓
4. ScrapeJobSchedulerService.scheduleJob()
   ├── Creates Quartz JobDetail
   ├── Creates Trigger (cron or immediate)
   └── Registers with Quartz Scheduler
         ↓
5. ScrapeQuartzJob.execute() (when triggered)
   ├── Fetches recipe YAML
   ├── Parses recipe configuration
   ├── For each ScrapeTarget:
   │   ├── ScraperEngine.execute()
   │   │   ├── PageFetcher.fetch() (HtmlUnit/Playwright)
   │   │   ├── HtmlProcessingService.process()
   │   │   ├── FieldExtractionService.extractFields()
   │   │   └── MarkdownConversionService.toMarkdown()
   │   └── Stores results via FileStorageService
   └── Updates job status
```

---

## Recipe YAML Structure

Recipes define what data to extract and how to navigate pages.

### Basic Recipe

```yaml
name: Example Scraper
description: Scrapes product data
match:
  domains:
    - example.com
page:
  contentRoot: "div.product"
  hrefSelector: "a.next-page"
  fields:
    - name: title
      selectors: ["h1.title"]
      source: TEXT
    - name: price
      selectors: ["span.price"]
      source: TEXT
      dataPattern: "\\$([\\d.]+)"  # Regex to extract number
    - name: image
      selectors: ["img.product-image"]
      source: ATTR
      attributeName: src
options:
  stripCss: true
  stripJs: true
  fetcherType: PLAYWRIGHT
```

### Multi-Page Recipe

```yaml
name: Site and Sub-Pages
match:
  domains:
    - example.com
page:
  hrefSelector: "a[href^='/']"
  fields:
    - name: title
      selectors: ["title", "h1"]
      source: TEXT
    - name: body
      selectors: ["article", "main"]
      source: HTML
  subPages:
    - name: reviews
      path: "/reviews"
      fields:
        - name: reviewText
          selectors: ["div.review"]
          source: TEXT
        - name: rating
          selectors: ["span.rating"]
          source: TEXT
options:
  stripCss: true
  stripJs: true
```

### Field Source Types

| Source | Description | Example |
|--------|-------------|---------|
| `TEXT` | Extract visible text | `"Hello World"` |
| `HTML` | Extract inner HTML | `"<b>Hello</b> World"` |
| `ATTR` | Extract attribute value | Requires `attributeName` |

---

## Database Schema

### Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐
│  ScraperRecipe  │◄──────│   ScrapeJob     │
│─────────────────│  1:N  │─────────────────│
│ id (UUID)       │       │ id (UUID)       │
│ name            │       │ recipe_id (FK)  │
│ yaml_content    │       │ status          │
│ enabled         │       │ schedule_cron   │
└─────────────────┘       └────────┬────────┘
                                   │ 1:N
                                   ▼
                          ┌─────────────────┐
                          │  ScrapeTarget   │
                          │─────────────────│
                          │ id (UUID)       │
                          │ job_id (FK)     │
                          │ url             │
                          │ status          │
                          │ error_message   │
                          └────────┬────────┘
                                   │ 1:1
                                   ▼
                          ┌─────────────────┐
                          │ ScrapeResultData│
                          │─────────────────│
                          │ target_id (FK)  │
                          │ data_json       │
                          │ progress_json   │
                          └─────────────────┘

┌─────────────────┐
│      User       │
│─────────────────│
│ id (UUID)       │
│ username        │
│ password_hash   │
│ role            │
│ enabled         │
└─────────────────┘

┌─────────────────┐
│   AppSetting    │
│─────────────────│
│ setting_key (PK)│
│ setting_value   │
└─────────────────┘
```

### Table Descriptions

| Table | Purpose |
|-------|---------|
| `scraper_recipes` | Stores YAML-based scraping configurations |
| `scrape_jobs` | Scheduled job definitions with cron expressions |
| `scrape_targets` | Individual URLs to scrape within a job |
| `scrape_result_data` | Extracted structured data and progress |
| `users` | Application users with roles |
| `app_settings` | Application-wide settings (e.g., fetcher type) |

---

## Key Components Explained

### 1. ScraperEngine - The Core

The `ScraperEngine` orchestrates the entire scraping process:

```java
public ScrapeExecutionResult execute(RecipeConfig recipe, String url, 
                                      OptionsConfig overrides, 
                                      ProgressListener progressListener) {
    
    // Step 1: Fetch HTML
    PageFetcher fetcher = selectFetcher();  // HtmlUnit or Playwright
    String rawHtml = fetcher.fetch(url);
    
    // Step 2: Process HTML (strip CSS/JS, extract hrefs)
    ProcessedHtmlResult processed = htmlProcessingService.process(
        rawHtml, effectiveOptions, recipe.getPage());
    
    // Step 3: Extract structured data
    Map<String, Object> fields = fieldExtractionService.extractFields(
        rawDocument, recipe.getPage());
    
    // Step 4: Convert to Markdown
    String markdown = markdownConversionService.toMarkdown(
        processed.processedHtml());
    
    // Step 5: Return result
    return new ScrapeExecutionResult(
        rawHtml, processed.processedHtml(), markdown, 
        structuredData, processed.hrefs(), progressSteps);
}
```

### 2. Page Fetchers

| Fetcher | Use Case | Pros | Cons |
|---------|----------|------|------|
| **HtmlFetcher** | Static sites | Fast, lightweight | No JavaScript |
| **PlaywrightFetcher** | Dynamic sites | Full browser, JS support | Slower, more resources |

**HtmlFetcher** (HtmlUnit):
```java
public String fetch(String url) {
    try (WebClient client = buildClient()) {
        WebRequest request = new WebRequest(new URL(url));
        HtmlPage page = client.getPage(request);
        client.waitForBackgroundJavaScript(timeoutMillis);
        return page.asXml();
    }
}
```

**PlaywrightFetcher** (Full Browser):
```java
public String fetch(String url) {
    try (Playwright playwright = Playwright.create();
         Browser browser = selectBrowser(playwright)) {
        
        try (BrowserContext context = browser.newContext(...);
             Page page = context.newPage()) {
            
            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            return page.content();
        }
    }
}
```

### 3. Quartz Scheduler

Enables cron-based recurring jobs:

**Configuration** (`QuartzConfig.java`):
```java
@Configuration
public class QuartzConfig {
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(ApplicationContext applicationContext) {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setJobFactory(jobFactory(applicationContext.getAutowireCapableBeanFactory()));
        return factoryBean;
    }
    
    // Custom job factory that autowires Quartz job instances
    private SpringBeanJobFactory jobFactory(AutowireCapableBeanFactory beanFactory) {
        return new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);  // Enable @Autowired in jobs
                return job;
            }
        };
    }
}
```

**Scheduling** (`ScrapeJobSchedulerService.java`):
```java
@Transactional
public void scheduleJob(ScrapeJob job) {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    
    // Create Quartz JobDetail
    JobDetail detail = JobBuilder.newJob(ScrapeQuartzJob.class)
        .withIdentity(key)
        .usingJobData("jobId", job.getId().toString())
        .build();
    
    // Create Trigger (cron or immediate)
    Trigger trigger = buildTrigger(job, key);
    
    scheduler.scheduleJob(detail, trigger);
}

private Trigger buildTrigger(ScrapeJob job, JobKey key) {
    TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger()
        .withIdentity("trigger-" + job.getId())
        .forJob(key);
    
    if (StringUtils.hasText(job.getScheduleCron())) {
        builder.withSchedule(CronScheduleBuilder.cronSchedule(job.getScheduleCron()));
    } else {
        builder.startNow();  // One-time job
    }
    return builder.build();
}
```

### 4. File Storage

Artifacts are stored in a hierarchical directory structure:

```
{storageRoot}/
  {jobId}/
    {targetId}/
      raw.html              # Original HTML
      processed.html        # Cleaned HTML (CSS/JS removed)
      processed.md          # Markdown version
      pages/
        01-main/
          raw.html
          processed.html
          processed.md
          meta.properties
        02-about/
          ...
    archives/
      job-archive-20240101-120000.zip
```

**Storage Service** (`FileStorageService.java`):
```java
@Service
public class FileStorageService {
    
    public Path resolveJobDir(UUID jobId) {
        return Paths.get(storageRoot, jobId.toString());
    }
    
    public Path resolveTargetDir(UUID jobId, UUID targetId) {
        return Paths.get(storageRoot, jobId.toString(), targetId.toString());
    }
    
    public void saveRawHtml(UUID jobId, UUID targetId, String html) {
        Path dir = resolveTargetDir(jobId, targetId);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("raw.html"), html);
    }
}
```

### 5. HTML Processing

**HtmlProcessingService** cleans and processes HTML:

```java
public ProcessedHtmlResult process(String rawHtml, OptionsConfig options, 
                                    PageConfig pageConfig) {
    Document document = Jsoup.parse(rawHtml);
    
    // Strip CSS if configured
    if (options.isStripCss()) {
        document.select("style, link[rel=stylesheet]").remove();
    }
    
    // Strip JavaScript if configured
    if (options.isStripJs()) {
        document.select("script").remove();
    }
    
    // Remove non-whitelisted attributes
    if (options.isRemoveAttributes()) {
        for (Element element : document.getAllElements()) {
            attributes.forEach(attribute -> {
                if (!ATTRIBUTE_WHITELIST.contains(attribute.getKey())) {
                    element.removeAttr(attribute.getKey());
                }
            });
        }
    }
    
    // Extract hrefs using configured selector
    String hrefSelector = pageConfig.getHrefSelector() != null 
        ? pageConfig.getHrefSelector() : "a[href]";
    List<String> hrefs = document.select(hrefSelector)
        .stream()
        .map(e -> e.attr("href"))
        .distinct()
        .toList();
    
    return new ProcessedHtmlResult(processed, hrefs, document);
}
```

### 6. Field Extraction

**FieldExtractionService** extracts data using CSS selectors:

```java
public Map<String, Object> extractFields(Document document, PageConfig pageConfig) {
    Map<String, Object> results = new HashMap<>();
    
    for (FieldConfig field : pageConfig.getFields()) {
        Object value = field.isMultiple() 
            ? extractMultiple(document, field) 
            : extractSingle(document, field);
        results.put(field.getName(), value);
    }
    return results;
}

private String extractValue(Element element, FieldConfig field) {
    return switch (field.getSource()) {
        case TEXT -> element.text();
        case HTML -> element.html();
        case ATTR -> element.attr(field.getAttributeName());
    };
}

// Apply regex pattern if configured
private String applyPattern(String value, String dataPattern) {
    if (dataPattern == null) return value;
    Pattern pattern = Pattern.compile(dataPattern);
    var matcher = pattern.matcher(value);
    if (matcher.find()) {
        return matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
    }
    return value;
}
```

---

## Design Patterns

| Pattern | Implementation |
|---------|----------------|
| **Strategy** | `PageFetcher` interface with `HtmlFetcher`/`PlaywrightFetcher` implementations |
| **DTO/Form** | `RecipeForm`, `JobForm`, `SettingsForm` for view binding |
| **Repository** | Spring Data JPA interfaces |
| **Service Layer** | Business logic isolated from controllers |
| **Template Method** | Multi-page vs single-page scraping flow |
| **Dependency Injection** | Constructor injection throughout |
| **Factory** | `ScraperEngine` selects fetcher based on settings |

---

## Important Files to Study

| File | Purpose |
|------|---------|
| `WebScraperApplication.java` | Main entry point |
| `ScraperEngine.java` | Core scraping logic |
| `ScrapeJobService.java` | Job creation & management |
| `ScrapeQuartzJob.java` | Quartz job execution |
| `FieldExtractionService.java` | CSS selector extraction |
| `db.changelog-1.0.yaml` | Database schema definition |
| `RecipeConfig.java` | Root YAML configuration model |
| `PageConfig.java` | Page-level scraping configuration |
| `FieldConfig.java` | Individual field extraction rules |
| `SubPageConfig.java` | Sub-page navigation configuration |
| `QuartzConfig.java` | Quartz scheduler configuration |
| `FileStorageService.java` | File system persistence |

---

## Package Structure

```
src/main/java/com/browzwi/webscraper/
├── WebScraperApplication.java          # Main entry point
├── web/                                # Web layer (Controllers)
│   ├── DashboardController.java
│   ├── RecipeController.java
│   ├── ScrapeJobController.java
│   ├── SettingsController.java
│   ├── LoginController.java
│   ├── UtilityController.java
│   ├── dto/                            # Form objects
│   │   ├── RecipeForm.java
│   │   ├── JobForm.java
│   │   ├── SettingsForm.java
│   │   └── RecipeExample.java
│   └── view/                           # View adapters
│       └── *.java
├── service/                            # Business logic layer
│   ├── ScraperRecipeService.java
│   ├── ScrapeJobService.java
│   ├── ScrapeJobSchedulerService.java
│   ├── ScrapeTargetArchiveService.java
│   ├── settings/
│   │   └── SettingsService.java
│   ├── job/
│   │   └── ScrapeQuartzJob.java
│   └── test/
│       └── RecipeTestSessionService.java
├── scraper/                            # Scraping engine
│   ├── service/
│   │   ├── ScraperEngine.java
│   │   ├── HtmlFetcher.java
│   │   ├── PlaywrightFetcher.java
│   │   ├── HtmlProcessingService.java
│   │   ├── FieldExtractionService.java
│   │   ├── MarkdownConversionService.java
│   │   └── *.java
│   └── model/                          # Recipe YAML models
│       ├── RecipeConfig.java
│       ├── PageConfig.java
│       ├── FieldConfig.java
│       ├── SubPageConfig.java
│       ├── OptionsConfig.java
│       ├── MatchConfig.java
│       └── DataSourceType.java
├── domain/                             # JPA entities
│   ├── ScraperRecipe.java
│   ├── ScrapeJob.java
│   ├── ScrapeTarget.java
│   ├── ScrapeResultData.java
│   ├── User.java
│   └── AppSetting.java
├── repository/                         # Spring Data JPA
│   ├── ScraperRecipeRepository.java
│   ├── ScrapeJobRepository.java
│   ├── ScrapeTargetRepository.java
│   ├── ScrapeResultDataRepository.java
│   ├── UserRepository.java
│   └── AppSettingRepository.java
├── storage/                            # File persistence
│   ├── FileStorageService.java
│   └── FileStorageProperties.java
├── security/                           # Security configuration
│   ├── SecurityConfig.java
│   ├── DatabaseUserDetailsService.java
│   └── DevAdminPasswordInitializer.java
└── web/                                # Additional web components
    └── GlobalControllerAdvice.java
```

---

## Configuration Files

| File | Purpose |
|------|---------|
| `src/main/resources/application.yml` | Base configuration |
| `src/main/resources/application-dev.yml` | Development profile |
| `src/main/resources/application.properties` | Additional properties |
| `src/main/resources/db/changelog/db.changelog-master.yaml` | Liquibase master changelog |
| `src/main/resources/db/changelog/db.changelog-1.0.yaml` | Database schema |
| `src/main/resources/templates/` | Thymeleaf views |

---

## Quick Reference

### Common Cron Expressions

| Expression | Description |
|------------|-------------|
| `0 0 * * * ?` | Every hour |
| `0 0 0 * * ?` | Daily at midnight |
| `0 0 9 * * MON-FRI` | Weekdays at 9 AM |
| `0 0/30 * * * ?` | Every 30 minutes |

### Scraper Engine Selection

Configure in `/settings` UI:

| Engine | Best For |
|--------|----------|
| **HtmlUnit** | Static HTML, fast scraping |
| **Playwright** | JavaScript-heavy sites (React, Vue, Angular) |

### Storage Location

Default: `./data/` (relative to application root)

Configure in `application.yml`:
```yaml
webscraper:
  storage:
    root: /var/data/webscraper
```

---

## Summary

The WebScraper application is a well-architected Spring Boot application that provides:

1. **Recipe-based scraping** - Users define YAML recipes with CSS selectors and extraction rules
2. **Job scheduling** - Quartz integration for one-time and recurring scraping jobs
3. **Multi-page support** - Scrape main pages and follow configured sub-pages
4. **Flexible fetching** - Choose between HtmlUnit (lighter) or Playwright (full browser)
5. **Progress tracking** - Real-time progress updates during scraping
6. **Result storage** - Store raw HTML, processed HTML, Markdown, and structured JSON data
7. **Archive generation** - Package all results into downloadable ZIP archives

The architecture follows clean separation of concerns with controllers handling HTTP requests, services managing business logic, repositories handling data access, and the scraper engine encapsulating all scraping operations.

---

**Last Updated:** March 5, 2026
