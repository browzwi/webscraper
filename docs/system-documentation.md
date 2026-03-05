# WebScraper System Documentation

**Version:** 1.0  
**Date:** March 5, 2026  
**Author:** System Documentation

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Technology Stack](#technology-stack)
3. [System Architecture](#system-architecture)
4. [Installation & Setup](#installation--setup)
5. [Features & Usage](#features--usage)
6. [URL Discovery Feature](#url-discovery-feature)
7. [Recipe System](#recipe-system)
8. [Job Management](#job-management)
9. [Database Schema](#database-schema)
10. [Configuration](#configuration)
11. [Troubleshooting](#troubleshooting)
12. [API Reference](#api-reference)

---

## System Overview

WebScraper is a Spring Boot-based web scraping platform with a modern web interface that allows users to:

- **Create scraping recipes** using YAML configurations with CSS selectors
- **Schedule scraping jobs** with Quartz-based scheduling
- **Discover business URLs** automatically from Google Maps or Brave Search
- **Extract structured data** from websites (text, HTML, attributes)
- **Export results** in JSON, HTML, Markdown, or Excel/CSV formats
- **Monitor progress** in real-time with detailed status tracking

### Key Capabilities

- ✅ Multi-page scraping (main page + sub-pages)
- ✅ Dual scraping engines (HtmlUnit for static sites, Playwright for JavaScript-heavy sites)
- ✅ Automatic URL discovery from Google Maps
- ✅ Email and social media extraction from business websites
- ✅ Scheduled recurring jobs with cron expressions
- ✅ Real-time progress tracking
- ✅ User authentication and authorization

---

## Technology Stack

### Backend
- **Java 17** - Programming language
- **Spring Boot 3.5.7** - Application framework
  - Spring MVC - Web layer
  - Spring Security - Authentication & authorization
  - Spring Data JPA - Database access
  - Spring Quartz - Job scheduling
- **Liquibase** - Database schema management
- **MySQL 8.4** - Production database
- **H2** - In-memory database for local development

### Frontend
- **Thymeleaf** - Server-side templating engine
- **HTMX 1.9.12** - Dynamic partial page updates
- **Tailwind CSS** - Utility-first CSS framework
- **JavaScript** - Minimal client-side interactions

### Scraping Engines
- **HtmlUnit 2.70.0** - Headless browser for static sites (fast, lightweight)
- **Playwright 1.46.0** - Full browser automation for dynamic sites (Chromium)
- **Jsoup 1.18.1** - HTML parsing and CSS selector extraction

### Additional Libraries
- **Flexmark** - HTML to Markdown conversion
- **Apache POI** - Excel file generation
- **OpenCSV** - CSV file generation
- **Jackson** - JSON processing

---

## System Architecture

### Layered Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        WebScraper Application                    │
├─────────────────────────────────────────────────────────────────┤
│  Web Layer (Controllers)                                         │
│  ├── DashboardController    - Main dashboard view               │
│  ├── RecipeController       - CRUD for scraping recipes         │
│  ├── ScrapeJobController    - Job management & scheduling       │
│  ├── DiscoveryController    - URL discovery from Google Maps    │
│  └── SettingsController     - Application configuration         │
│                                                                   │
│  Service Layer (Business Logic)                                  │
│  ├── ScraperRecipeService   - Recipe parsing & validation       │
│  ├── ScrapeJobService       - Job creation & management         │
│  ├── ScrapeJobSchedulerService - Quartz scheduling              │
│  ├── UrlDiscoveryService    - Google Maps/Brave Search          │
│  └── BusinessWebsiteScrapeService - Email/social media extract  │
│                                                                   │
│  Scraper Engine                                                   │
│  ├── ScraperEngine          - Core scraping orchestration       │
│  ├── HtmlFetcher            - HtmlUnit-based fetching           │
│  ├── PlaywrightFetcher      - Full browser automation           │
│  ├── HtmlProcessingService  - HTML cleaning & processing        │
│  ├── FieldExtractionService - CSS selector data extraction      │
│  └── MarkdownConversionService - HTML to Markdown conversion    │
│                                                                   │
│  Data Layer (Repositories)                                        │
│  └── Spring Data JPA interfaces                                  │
│                                                                   │
│  Domain Entities                                                  │
│  ├── ScraperRecipe          - YAML recipe storage               │
│  ├── ScrapeJob              - Scheduled job definition          │
│  ├── ScrapeTarget           - Individual URL to scrape          │
│  ├── ScrapeResultData       - Extracted data storage            │
│  ├── DiscoveryJob           - URL discovery job                 │
│  └── DiscoveredBusiness     - Discovered business data          │
└─────────────────────────────────────────────────────────────────┘
```

### Request Flow

```
User Request
    ↓
Controller (validates input, prepares model)
    ↓
Service (business logic, orchestration)
    ↓
Scraper Engine (fetches & processes HTML)
    ↓
Repository (persists data)
    ↓
File Storage (saves raw/processed HTML, JSON)
    ↓
Response (Thymeleaf renders view)
```

---

## Installation & Setup

### Prerequisites

- **Java 17** or higher
- **Maven 3.6+** (or use included Maven wrapper)
- **MySQL 8.0+** (for production) or H2 (for local development)
- **Node.js** (optional, for Tailwind CSS compilation)

### Local Development Setup

1. **Clone the repository:**
   ```bash
   cd C:\Users\lanzc\Downloads\webscraper-main\webscraper-main
   ```

2. **Run with local profile (H2 database):**
   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

3. **Access the application:**
   - URL: http://localhost:8080
   - Username: `admin`
   - Password: `admin`

### Production Setup with MySQL

1. **Create MySQL database:**
   ```sql
   CREATE DATABASE webscraper;
   CREATE USER 'webscraper'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON webscraper.* TO 'webscraper'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configure database connection:**
   Edit `src/main/resources/application-dev.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/webscraper
       username: webscraper
       password: your_password
   ```

3. **Build and run:**
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
   ```

### Install Playwright Browsers (Optional)

For JavaScript-heavy sites:
```powershell
.\mvnw.cmd exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
```

---

## Features & Usage

### 1. Dashboard

**URL:** http://localhost:8080

The dashboard provides:
- Overview of all scraping jobs
- Quick stats (total jobs, completed, running, failed)
- Recent activity
- Quick access to recipes, jobs, and discovery

### 2. User Authentication

**Default Credentials:**
- Username: `admin`
- Password: `admin`

**Security Features:**
- Spring Security with database-backed user management
- Password encryption (BCrypt)
- Session management
- CSRF protection

---

## URL Discovery Feature

### Overview

The URL Discovery feature automatically finds businesses from Google Maps or Brave Search based on keyword and location.

### How It Works

1. **User Input:**
   - Keyword (e.g., "coffee shops", "law offices", "restaurants")
   - Location (e.g., "Makati, Metro Manila", "Kawit, Cavite")

2. **Discovery Process:**
   - Searches Google Maps using Playwright
   - Extracts business information:
     - Business Name
     - Address
     - Phone Number
     - Website URL

3. **Website Scraping:**
   - Visits each business website
   - Extracts email addresses (regex pattern matching)
   - Extracts social media links (Facebook, Instagram, Twitter, LinkedIn, TikTok, YouTube)

4. **Data Storage:**
   - Saves all discovered data to database
   - Allows export to Excel/CSV

### Usage Steps

1. **Navigate to Discovery:**
   - Click "Discovery" in the sidebar

2. **Enter Search Criteria:**
   ```
   Keyword: coffee shops
   Location: Makati, Metro Manila
   ```

3. **Click "Discover Businesses"**

4. **Wait for Completion:**
   - Status will change from "RUNNING" to "COMPLETED"
   - Typically takes 30-60 seconds for 10-20 businesses

5. **View Results:**
   - Click on the completed job
   - View table with all discovered data

6. **Export Data:**
   - Click "Export to Excel/CSV"
   - Download spreadsheet with all business information

### Discovery Sources

**Option 1: Playwright (Google Maps)**
- Free, no API key required
- Scrapes Google Maps directly
- May be blocked by Google over time
- Recommended for: Development, testing

**Option 2: Google Places API**
- Requires API key from Google Cloud Console
- More reliable, official API
- Free tier: $200/month credit
- Recommended for: Production use

**Option 3: Brave Search API**
- Free API, no blocking
- More reliable than Playwright
- Better data quality
- Recommended for: Production use

### Configuration

Go to **Settings** → **Discovery Source** to choose:
- `PLAYWRIGHT` - Google Maps scraping (default)
- `GOOGLE_PLACES_API` - Official Google API
- `BRAVE_SEARCH` - Brave Search API

---

## Recipe System

### What is a Recipe?

A recipe is a YAML configuration that defines:
- What website to scrape
- What data to extract
- How to extract it (CSS selectors)
- Processing options

### Recipe Structure

```yaml
name: Recipe Name
description: What this recipe does
match:
  domains:
    - example.com
page:
  contentRoot: "div.container"
  hrefSelector: "a[href]"
  fields:
    - name: title
      selectors: ["h1.title", "h2.heading"]
      source: TEXT
    - name: price
      selectors: ["span.price"]
      source: TEXT
      dataPattern: "\\$([\\d.]+)"
    - name: image
      selectors: ["img.product-image"]
      source: ATTR
      attributeName: src
  subPages:
    - path: /about
      fields:
        - name: description
          selectors: ["div.about"]
          source: TEXT
options:
  stripCss: true
  stripJs: true
  fetcherType: HTMLUNIT
```

### Field Configuration

**Field Properties:**
- `name` - Field identifier
- `selectors` - Array of CSS selectors (tries in order)
- `source` - Data source type:
  - `TEXT` - Extract visible text
  - `HTML` - Extract inner HTML
  - `ATTR` - Extract attribute value
- `attributeName` - Required if source is ATTR
- `dataPattern` - Optional regex pattern for extraction
- `multiple` - Set to true to extract all matches (returns array)

### Creating a Recipe

1. **Navigate to Recipes:**
   - Click "Recipes" → "New Recipe"

2. **Fill in YAML:**
   ```yaml
   name: Product Scraper
   description: Scrapes product information
   match:
     domains:
       - example.com
   page:
     fields:
       - name: title
         selectors: ["h1.product-title"]
         source: TEXT
       - name: price
         selectors: ["span.price"]
         source: TEXT
   options:
     fetcherType: HTMLUNIT
   ```

3. **Test Recipe:**
   - Click "Test Recipe"
   - Enter test URL
   - Verify extracted data

4. **Save Recipe:**
   - Click "Save Recipe"

### Finding CSS Selectors

1. Open website in Chrome
2. Right-click element → "Inspect"
3. Right-click HTML element → "Copy" → "Copy selector"
4. Use in recipe

---

## Job Management

### Creating a Job

1. **Navigate to Jobs:**
   - Click "Jobs" → "New Job"

2. **Configure Job:**
   - **Job Name:** Descriptive name
   - **Recipe:** Select from dropdown
   - **Target URLs:** One URL per line
   - **Schedule:** Optional cron expression

3. **Click "Create Job"**

### Job Scheduling

**Cron Expression Examples:**

| Schedule | Cron Expression |
|----------|-----------------|
| Every hour | `0 0 * * * ?` |
| Daily at midnight | `0 0 0 * * ?` |
| Every 30 minutes | `0 0/30 * * * ?` |
| Weekdays at 9 AM | `0 0 9 * * MON-FRI` |

**Cron Format:**
```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12)
│ │ │ │ │ ┌───────────── day of week (MON-SUN)
│ │ │ │ │ │
* * * * * ?
```

### Job Status

- **PENDING** - Waiting to start
- **RUNNING** - Currently scraping
- **COMPLETED** - Finished successfully
- **FAILED** - Error occurred

### Viewing Results

1. Go to "Jobs" → Click on job
2. View target-level status
3. Click "View Results"
4. Download JSON, HTML, or Markdown

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

┌─────────────────┐       ┌─────────────────┐
│  DiscoveryJob   │◄──────│DiscoveredBusiness│
│─────────────────│  1:N  │─────────────────│
│ id (BIGINT)     │       │ id (BIGINT)     │
│ keyword         │       │ job_id (FK)     │
│ location        │       │ business_name   │
│ status          │       │ address         │
│ results_count   │       │ phone_number    │
└─────────────────┘       │ website_url     │
                          │ email_address   │
                          │ social_media    │
                          └─────────────────┘
```

### Key Tables

**scraper_recipes**
- Stores YAML-based scraping configurations
- Fields: id, name, description, yaml_content, enabled, created_at

**scrape_jobs**
- Scheduled job definitions with cron expressions
- Fields: id, recipe_id, name, status, schedule_cron, options_json

**scrape_targets**
- Individual URLs to scrape within a job
- Fields: id, job_id, url, status, error_message

**scrape_result_data**
- Extracted structured data and progress
- Fields: target_id, data_json, progress_json

**discovery_job**
- URL discovery job metadata
- Fields: id, keyword, location, status, discovery_source, results_count

**discovered_business**
- Discovered business information
- Fields: id, job_id, business_name, address, phone_number, website_url, email_address, social_media_links

---

## Configuration

### Application Profiles

| Profile | Description | Database |
|---------|-------------|----------|
| `local` | Local development | H2 in-memory |
| `dev` | Development with MySQL | MySQL |
| `production` | Production-ready | MySQL |

### Key Configuration Properties

**application-local.yml:**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:webscraper
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console

webscraper:
  storage:
    root: ./data
  fetcher:
    playwright:
      browser: chromium
      headless: true
      timeout: 20000
  security:
    admin:
      username: admin
      password: admin
```

### Settings Management

**Access Settings:**
- Click "Settings" in sidebar

**Available Settings:**
- **Scraper Engine:** HtmlUnit (fast) or Playwright (full browser)
- **Discovery Source:** Playwright, Google Places API, or Brave Search
- **Google Places API Key:** For Google Places API
- **Brave Search API Key:** For Brave Search API
- **Storage Location:** Where to save scraped data
- **Timeout:** Request timeout in milliseconds

---

## Troubleshooting

### Common Issues

**1. Port 8080 Already in Use**

Error: `Port 8080 was already in use`

Solution:
```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill the process
taskkill /PID <PID> /F
```

**2. Playwright Browser Not Found**

Error: `Executable doesn't exist`

Solution:
```powershell
.\mvnw.cmd exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
```

**3. Database Connection Failed**

Error: `Communications link failure`

Solution:
- Verify MySQL is running
- Check credentials in `application-dev.yml`
- Ensure database exists

**4. Discovery Returns No Results**

Possible causes:
- Google Maps blocked the request
- No businesses found for keyword/location
- Selectors changed (Google Maps updates frequently)

Solution:
- Switch to Brave Search API (more reliable)
- Try different keyword/location
- Check PowerShell logs for errors

**5. No Emails/Social Media Found**

Cause: Businesses don't have websites listed

Solution:
- Search for businesses that typically have websites (restaurants, hotels, cafes)
- Use Google Places API (provides more data)
- Manually add website URLs if known

### Viewing Logs

**PowerShell Window:**
- All application logs appear here
- Look for ERROR or WARN messages

**Enable Debug Logging:**
Edit `application-local.yml`:
```yaml
logging:
  level:
    com.browzwi.webscraper: DEBUG
```

---

## API Reference

### REST Endpoints

**Dashboard**
- `GET /` - Main dashboard

**Recipes**
- `GET /recipes` - List all recipes
- `POST /recipes` - Create new recipe
- `GET /recipes/{id}` - View recipe details
- `PUT /recipes/{id}` - Update recipe
- `DELETE /recipes/{id}` - Delete recipe
- `POST /recipes/{id}/test` - Test recipe execution

**Jobs**
- `GET /jobs` - List all jobs
- `POST /jobs` - Create new job
- `GET /jobs/{id}` - View job details
- `POST /jobs/{id}/start` - Start job manually
- `POST /jobs/{id}/pause` - Pause scheduled job
- `GET /jobs/{id}/results` - Get job results

**Discovery**
- `GET /discovery` - Discovery page
- `POST /discovery/start` - Start discovery job
- `GET /discovery/{jobId}/results` - View discovery results
- `GET /discovery/{jobId}/export` - Export to Excel/CSV

**Settings**
- `GET /settings` - View settings
- `PUT /settings` - Update settings

---

## File Storage Structure

```
data/
└── jobs/
    └── {jobId}/
        └── {targetId}/
            ├── raw.html              # Original HTML
            ├── processed.html        # Cleaned HTML
            ├── processed.md          # Markdown version
            └── pages/
                ├── 01-main/
                │   ├── raw.html
                │   ├── processed.html
                │   ├── processed.md
                │   └── meta.properties
                └── 02-about/
                    └── ...
```

---

## Performance Considerations

### Scraping Speed

**HtmlUnit:**
- Fast (100-200ms per page)
- Low memory usage
- Best for: Static HTML sites

**Playwright:**
- Slower (2-5 seconds per page)
- High memory usage (full browser)
- Best for: JavaScript-heavy sites (React, Vue, Angular)

### Optimization Tips

1. **Use HtmlUnit when possible** - Much faster for static sites
2. **Limit concurrent jobs** - Avoid overwhelming the system
3. **Add delays between requests** - Respect target websites
4. **Use specific CSS selectors** - Faster than generic selectors
5. **Disable unnecessary options** - Don't strip CSS/JS if not needed

---

## Security Best Practices

1. **Change default admin password** immediately
2. **Use environment variables** for sensitive data (API keys, passwords)
3. **Enable HTTPS** in production
4. **Restrict access** with firewall rules
5. **Regular backups** of database
6. **Monitor logs** for suspicious activity
7. **Keep dependencies updated** for security patches

---

## Deployment

### Production Deployment Steps

1. **Build production JAR:**
   ```powershell
   .\mvnw.cmd clean package -Pproduction -DskipTests
   ```

2. **Set environment variables:**
   ```bash
   export SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/webscraper
   export SPRING_DATASOURCE_USERNAME=prod_user
   export SPRING_DATASOURCE_PASSWORD=secure_password
   export WEBSCRAPER_STORAGE_ROOT=/var/data/webscraper
   ```

3. **Run application:**
   ```bash
   java -jar target/webscraper-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
   ```

### Docker Deployment (Optional)

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/webscraper-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

## Support & Maintenance

### Regular Maintenance Tasks

1. **Database backups** - Daily automated backups
2. **Log rotation** - Prevent disk space issues
3. **Update dependencies** - Monthly security updates
4. **Monitor disk space** - Scraped data can grow large
5. **Review failed jobs** - Investigate and fix issues

### Monitoring

**Key Metrics to Monitor:**
- Job success rate
- Average scraping time
- Database size
- Disk space usage
- Memory usage
- Error rates

---

## Changelog

### Version 1.0 (March 5, 2026)

**Features:**
- ✅ Recipe-based scraping system
- ✅ Job scheduling with Quartz
- ✅ URL discovery from Google Maps
- ✅ Email and social media extraction
- ✅ Multi-page scraping support
- ✅ HtmlUnit and Playwright engines
- ✅ Excel/CSV export
- ✅ User authentication
- ✅ Real-time progress tracking

**Bug Fixes:**
- Fixed Google Maps selector issues
- Improved error handling in discovery
- Better timeout management

---

## License

This project is licensed under the MIT License.

---

## Contact & Support

For issues, questions, or feature requests:
- Check existing documentation
- Review test cases for usage examples
- Check PowerShell logs for errors

---

**Last Updated:** March 5, 2026  
**System Version:** 1.0  
**Documentation Version:** 1.0
