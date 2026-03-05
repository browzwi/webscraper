# WebScraper

A Spring Boot-based web scraping platform with a modern web interface built using Thymeleaf, HTMX, and Tailwind CSS. This application provides a comprehensive solution for scheduling, executing, and managing web scraping jobs with support for both static and dynamic websites.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Usage Guide](#usage-guide)
- [API Endpoints](#api-endpoints)
- [Testing](#testing)
- [Build & Deployment](#build--deployment)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Features

- **Visual Recipe Builder**: Create scraping recipes using YAML configuration with visual form interface
- **Multi-Page Scraping**: Support for navigating and scraping multiple pages with sub-page configurations
- **Dynamic Content Support**: Choose between HtmlUnit (fast, lightweight) or Playwright (full browser automation) for JavaScript-heavy sites
- **Scheduled Jobs**: Quartz-based scheduler for automated recurring scraping tasks
- **Real-Time Progress Tracking**: Monitor scraping progress with detailed target-level status updates
- **Markdown Conversion**: Automatic HTML-to-Markdown conversion for clean output
- **Data Export**: Structured JSON data storage with filesystem persistence
- **User Authentication**: Spring Security with database-backed user management
- **Responsive UI**: Modern, mobile-friendly interface with HTMX-powered partial updates
- **Error Handling**: Comprehensive error pages and validation feedback
- **URL Discovery**: Auto-discover businesses from Google Maps or Google Search (no API keys required)
- **Email & Social Media Extraction**: Automatically extract contact info from business websites

## Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.5.7**
  - Spring MVC
  - Spring Security
  - Spring Data JPA
  - Spring Quartz (Job Scheduling)
- **Liquibase** - Database schema management
- **MySQL 8.4** - Production database (H2 for testing)

### Frontend
- **Thymeleaf** - Server-side templating
- **HTMX 1.9.12** - Dynamic partial page updates
- **Tailwind CSS** - Utility-first CSS framework
- **JavaScript** - Minimal client-side interactions

### Scraping Engines
- **HtmlUnit** - Headless browser for static sites
- **Playwright** - Full browser automation for dynamic sites (Chromium)

### Testing
- **JUnit 5**
- **Mockito**
- **Spring Boot Test**
- **Cucumber** (BDD)

## Project Structure

```
webscraper/
├── src/main/java/com/browzwi/webscraper/
│   ├── WebScraperApplication.java      # Main entry point
│   ├── web/                            # Controllers & DTOs
│   │   ├── DashboardController.java
│   │   ├── RecipeController.java
│   │   ├── ScrapeJobController.java
│   │   ├── SettingsController.java
│   │   └── dto/                        # Form objects
│   ├── service/                        # Business logic
│   │   ├── ScraperRecipeService.java
│   │   ├── ScrapeJobService.java
│   │   ├── ScrapeJobSchedulerService.java
│   │   └── settings/                   # App settings
│   ├── scraper/                        # Scraping engine
│   │   ├── service/
│   │   │   ├── ScraperEngine.java
│   │   │   ├── HtmlFetcher.java
│   │   │   ├── PlaywrightFetcher.java
│   │   │   ├── HtmlProcessingService.java
│   │   │   └── MarkdownConversionService.java
│   │   └── model/                      # Recipe YAML models
│   ├── domain/                         # JPA entities
│   ├── repository/                     # Spring Data repositories
│   ├── storage/                        # File storage service
│   └── security/                       # Security configuration
├── src/main/resources/
│   ├── templates/                      # Thymeleaf views
│   │   ├── dashboard.html
│   │   ├── recipes/
│   │   ├── jobs/
│   │   └── settings/
│   ├── db/changelog/                   # Liquibase migrations
│   └── application.yml                 # Configuration
├── src/test/                           # Unit & integration tests
└── docs/                               # Documentation
```

## Prerequisites

- **Java 17** or higher
- **Maven 3.6+** (or use the included Maven wrapper)
- **MySQL 8.0+** (for development/production)
- **Node.js** (optional, for Tailwind CSS compilation)

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/webscraper-main.git
cd webscraper-main/webscraper-main
```

### 2. Database Setup

Create a MySQL database and user:

```sql
CREATE DATABASE webscraper;
CREATE USER 'webscraper'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON webscraper.* TO 'webscraper'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configure Application

Edit `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/webscraper
    username: webscraper
    password: your_password
```

### 4. Build the Project

```bash
./mvnw clean install
```

### 5. Install Playwright Browsers (Optional)

If using Playwright for dynamic sites:

```bash
./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

## Configuration

### Application Profiles

| Profile | Description |
|---------|-------------|
| `dev` | Development with MySQL, debug logging |
| `test` | H2 in-memory database for testing |
| `production` | Production-ready configuration |

### Key Configuration Properties

```yaml
webscraper:
  storage:
    root: ./data                    # File storage location
  fetcher:
    playwright:
      browser: chromium             # Browser engine
      headless: true                # Run headless
      timeout: 20000                # Page load timeout (ms)
  security:
    admin:
      username: admin               # Dev admin username
      password: admin123            # Dev admin password
```

### Scraper Engine Selection

Configure in `/settings` UI or via database:

- **HtmlUnit**: Fast, lightweight, no JavaScript execution
- **Playwright**: Full browser, supports JavaScript, slower

### URL Discovery Sources

Configure in `/settings` → **Discovery Source**:

| Source | Description | API Key Required |
|--------|-------------|------------------|
| **Google Maps** | Scrapes Google Maps business listings (60+ results per search) | ❌ No |
| **Google Search** | Scrapes Google Search results with optional site filter (e.g., facebook.com) | ❌ No |

**Google Search Site Filter**: Optionally filter results to specific domains (e.g., "facebook.com" for Facebook pages only, leave blank for all sites).

## Running the Application

### Development Mode

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Access the application at: http://localhost:8080

### Production Mode

```bash
./mvnw -Pproduction package
java -jar target/webscraper-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

### Docker (Optional)

```bash
docker-compose up -d
```

## Usage Guide

### 1. Create a Scraping Recipe

1. Navigate to **Recipes** → **New Recipe**
2. Fill in the form:
   - **Name**: Descriptive recipe name
   - **Base URL**: Starting URL for scraping
   - **Fields**: Define data fields to extract (CSS/XPath selectors)
   - **Sub-pages** (optional): Configure pagination or detail page navigation
3. Click **Save Recipe**

#### Example YAML Recipe

```yaml
name: Example Site Scraper
baseUrl: https://example.com
fields:
  - name: title
    selector: h1.title
    type: TEXT
  - name: description
    selector: meta[name="description"]
    type: ATTRIBUTE
    attribute: content
  - name: link
    selector: a.read-more
    type: HREF
options:
  fetcherType: PLAYWRIGHT
  waitForLoad: true
subPages:
  - name: details
    selector: a.detail-link
    fields:
      - name: price
        selector: span.price
        type: TEXT
```

### 2. Create a Scraping Job

1. Navigate to **Jobs** → **New Job**
2. Select a recipe from the dropdown
3. Configure:
   - **Job Name**: Unique identifier
   - **Targets**: URLs to scrape
   - **Schedule**: Cron expression for recurring jobs
4. Click **Create Job**

### 3. Monitor Job Progress

1. Go to **Jobs** → **Job List**
2. Click on a job to view details
3. View real-time progress per target:
   - ✅ Completed
   - ⏳ In Progress
   - ❌ Failed
4. Click on individual targets for detailed results

### 4. View Scraped Data

1. Navigate to completed job
2. Click **View Results**
3. Download JSON data or view processed HTML/Markdown

### 5. Configure Settings

Access **Settings** to:
- Switch between HtmlUnit and Playwright
- Configure timeout and retry policies
- Manage storage location
- **Configure URL Discovery source** (Google Maps or Google Search)
- **Set site filter for Google Search** (optional, e.g., "facebook.com")

### 6. URL Discovery (Auto-Find Businesses)

1. Navigate to **Discovery** in sidebar
2. Enter search criteria:
   - **Keyword**: e.g., "coffee shops", "law firms", "restaurants"
   - **Location**: e.g., "Makati, Metro Manila", "Kawit, Cavite"
3. Click **Discover Businesses**
4. Wait for completion (30-60 seconds)
5. View discovered businesses with:
   - Business name
   - Address
   - Phone number
   - Website URL
   - Email address (auto-extracted from website)
   - Social media links (Facebook, Instagram, Twitter, LinkedIn, TikTok, YouTube)
6. Click **Export to Excel** to download results

**Discovery Sources** (configure in Settings):
- **Google Maps**: Best for local businesses with complete information (60+ results)
- **Google Search**: Best for finding websites with specific criteria, supports site filtering (e.g., facebook.com)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Dashboard |
| GET | `/recipes` | List all recipes |
| POST | `/recipes` | Create new recipe |
| GET | `/recipes/{id}` | View recipe details |
| PUT | `/recipes/{id}` | Update recipe |
| DELETE | `/recipes/{id}` | Delete recipe |
| POST | `/recipes/{id}/test` | Test recipe execution |
| GET | `/jobs` | List all jobs |
| POST | `/jobs` | Create new job |
| GET | `/jobs/{id}` | View job details |
| POST | `/jobs/{id}/start` | Start job manually |
| POST | `/jobs/{id}/pause` | Pause scheduled job |
| GET | `/jobs/{id}/results` | Get job results |
| GET | `/settings` | View settings |
| PUT | `/settings` | Update settings |

## Testing

### Run All Tests

```bash
./mvnw test -q
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=ScraperEngineTest
```

### Test Coverage

```bash
./mvnw clean verify jacoco:report
```

### Key Test Classes

- `ScraperEngineTest` - Core scraping logic
- `ScrapeJobServiceTest` - Job management
- `ScraperRecipeServiceTest` - Recipe parsing
- `MarkdownConversionServiceTest` - HTML to Markdown
- `MultiPageScraperEngineTest` - Pagination handling

## Build & Deployment

### Build JAR

```bash
./mvnw clean package -DskipTests
```

### Deploy to Production

1. Set production profile and database credentials
2. Build with production profile:
   ```bash
   ./mvnw package -Pproduction
   ```
3. Deploy JAR to server
4. Run with:
   ```bash
   java -jar webscraper-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
   ```

### Environment Variables

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/webscraper
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=secure_password
export WEBSCRAPER_STORAGE_ROOT=/var/data/webscraper
```

## Troubleshooting

### Common Issues

#### 1. Playwright Browser Not Found

**Error**: `PlaywrightException: Executable doesn't exist`

**Solution**:
```bash
./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

#### 2. Database Connection Failed

**Error**: `Communications link failure`

**Solution**:
- Verify MySQL is running: `systemctl status mysql`
- Check credentials in `application-dev.yml`
- Ensure database exists and user has privileges

#### 3. Liquibase Migration Errors

**Error**: `Migration failed for change set`

**Solution**:
- Check `DATABASECHANGELOG` table for failed migrations
- Review changelog files in `src/main/resources/db/changelog/`
- For dev only: Drop and recreate database

#### 4. Scraping Timeout

**Error**: `TimeoutException: Page did not load`

**Solution**:
- Increase timeout in `/settings` UI
- Switch to Playwright for JavaScript-heavy sites
- Check target site accessibility

#### 5. File Storage Permission Denied

**Error**: `StorageException: Could not create directory`

**Solution**:
```bash
chmod -R 755 ./data
chown -R $USER:$USER ./data
```

### Logs

View application logs:

```bash
# Console output (dev mode)
# Or check log file if configured
tail -f logs/application.log
```

Enable debug logging in `application-dev.yml`:

```yaml
logging:
  level:
    com.browzwi.webscraper: DEBUG
```

## Design Principles

This project follows SOLID principles and clean code practices:

- **Single Responsibility**: Each class has one purpose
- **Open/Closed**: Open for extension, closed for modification
- **Dependency Injection**: Constructor injection throughout
- **DTO Pattern**: Decoupled form objects for views
- **Strategy Pattern**: Pluggable fetcher engines

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -am 'feat: add new feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Submit a pull request

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `style:` Code style changes (formatting)
- `refactor:` Code refactoring
- `test:` Test additions/changes
- `chore:` Build/config changes

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on GitHub
- Check existing documentation in `/docs`
- Review test cases for usage examples

---

**Built with** ❤️ **using Spring Boot, Thymeleaf, and HTMX**
