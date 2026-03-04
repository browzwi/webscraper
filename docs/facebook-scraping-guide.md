# Facebook Scraping: End-to-End Guide

This document provides a comprehensive overview of how the web scraper application handles Facebook page scraping, from configuration to data extraction.

## Overview

The scraper uses a **recipe-based, rule-driven approach** to extract structured data from Facebook public pages. It supports multi-page scraping, allowing you to scrape a main profile page and related sub-pages (About, Photos, etc.) in a single operation.

```mermaid
flowchart TB
    subgraph Config["Configuration Layer"]
        YAML[YAML Recipe File<br/>facebook-multipage-example.yaml]
        Match[URL Match Rules]
        Fields[Field Selectors]
        SubPages[Sub-Page Configs]
    end

    subgraph Engine["Scraper Engine"]
        SelectFetcher{Fetcher<br/>Selector}
        Fetch[Page Fetcher]
        Process[HTML Processing]
        Extract[Field Extraction]
        Convert[Markdown Conversion]
    end

    subgraph Output["Output Layer"]
        RawHTML[Raw HTML]
        ProcHTML[Processed HTML]
        MD[Markdown]
        JSON[Structured JSON]
    end

    YAML --> Match
    YAML --> Fields
    YAML --> SubPages
    
    Match --> SelectFetcher
    SelectFetcher --> Fetch
    Fetch --> Process
    Process --> Extract
    Extract --> Convert
    
    Fields --> Extract
    SubPages --> Extract
    
    Fetch --> RawHTML
    Process --> ProcHTML
    Convert --> MD
    Extract --> JSON

    style Config fill:#e1f5ff
    style Engine fill:#fff4e1
    style Output fill:#e8f5e9
```

## Architecture Components

```mermaid
classDiagram
    class RecipeConfig {
        +String name
        +String description
        +MatchConfig match
        +OptionsConfig options
        +PageConfig page
    }

    class MatchConfig {
        +List~String~ urlRegexes
    }

    class PageConfig {
        +String contentRoot
        +String hrefSelector
        +List~FieldConfig~ fields
        +List~SubPageConfig~ subPages
    }

    class FieldConfig {
        +String name
        +List~String~ selectors
        +DataSourceType source
        +String attributeName
        +boolean multiple
        +String dataPattern
    }

    class SubPageConfig {
        +String path
        +List~FieldConfig~ fields
    }

    class ScraperEngine {
        +execute(RecipeConfig, String, OptionsConfig) ScrapeExecutionResult
        +executeMultiPage(RecipeConfig, String, OptionsConfig, ProgressListener) MultiPageScrapeResult
        -selectFetcher() PageFetcher
        -mergeOptions(OptionsConfig, OptionsConfig) OptionsConfig
    }

    class PageFetcher {
        <<interface>>
        +fetch(String url) String
    }

    class HtmlFetcher {
        +fetch(String url) String
    }

    class PlaywrightFetcher {
        +fetch(String url) String
    }

    class FieldExtractionService {
        +extractFields(Document, PageConfig) Map~String, Object~
        +extractFieldsFromConfig(Document, List~FieldConfig~) Map~String, Object~
    }

    class HtmlProcessingService {
        +process(String, OptionsConfig, PageConfig) ProcessedHtmlResult
    }

    RecipeConfig *-- MatchConfig
    RecipeConfig *-- PageConfig
    RecipeConfig *-- OptionsConfig
    PageConfig *-- FieldConfig
    PageConfig *-- SubPageConfig
    SubPageConfig *-- FieldConfig

    ScraperEngine --> PageFetcher
    ScraperEngine --> FieldExtractionService
    ScraperEngine --> HtmlProcessingService
    PageFetcher <|.. HtmlFetcher
    PageFetcher <|.. PlaywrightFetcher

    style RecipeConfig fill:#fff4e1
    style PageConfig fill:#fff4e1
    style FieldConfig fill:#fff4e1
    style ScraperEngine fill:#e1f5ff
    style PageFetcher fill:#e8f5e9
```

## Configuration: YAML Recipe

### Example Facebook Recipe

```yaml
# facebook-multipage-example.yaml
name: Facebook Public Page Multi-Page
description: Scrapes Facebook public pages including main page and about page

match:
  urlRegexes:
    - 'https://www.facebook.com/[A-Za-z0-9\.]+($|/.*)'

page:
  hrefSelector: "a[href*='facebook.com/']"
  
  # Main page fields
  fields:
    - name: title
      selectors: ["h1", "title"]
      source: TEXT
      
    - name: avatar
      selectors: ["svg[aria-label]:first-of-type g image"]
      source: ATTR
      attributeName: xlink:href
      
    - name: main_page_content
      selectors: ["[data-pagelet='ProfileTimeline']", ".userContentWrapper"]
      source: TEXT

  # Sub-pages with different field configurations
  subPages:
    - path: /about
      fields:
        - name: about_description
          selectors: ["[data-overviewsection='contact_basic_info']", ".about_section"]
          source: TEXT
          
        - name: contact_info
          selectors: ["[data-overviewsection='contact_basic_info'] .clearfix"]
          source: TEXT
          
        - name: website_url
          selectors: ["a[href^='http']"]
          source: ATTR
          attributeName: href
          
    - path: /photos
      fields:
        - name: photo_count
          selectors: [".photoCount", "[data-testid='photo_count']"]
          source: TEXT
          
        - name: recent_photos
          selectors: ["img[src*='scontent']"]
          source: ATTR
          attributeName: src
          multiple: true

options:
  stripCss: true
  stripJs: true
  removeAttributes: true
```

### Configuration Sections

```mermaid
flowchart LR
    subgraph Recipe["Recipe Structure"]
        Match["match<br/>URL regex patterns<br/>to trigger recipe"]
        Page["page<br/>Main page config<br/>+ field selectors"]
        SubPages["subPages<br/>Relative paths<br/>+ field selectors"]
        Options["options<br/>HTML processing<br/>rules"]
    end

    Match -->|Triggers on URL match| Page
    Page -->|After main scrape| SubPages
    Page -->|Applies to all| Options

    style Match fill:#e3f2fd
    style Page fill:#e3f2fd
    style SubPages fill:#e3f2fd
    style Options fill:#e3f2fd
```

## Execution Flow

### Multi-Page Scraping Sequence

```mermaid
sequenceDiagram
    participant Client
    participant Engine as ScraperEngine
    participant Fetcher as PageFetcher
    participant Processor as HtmlProcessingService
    participant Extractor as FieldExtractionService
    participant Storage as FileStorageService

    Client->>Engine: executeMultiPage(recipe, seedUrl)
    
    Note over Engine,Fetcher: Step 1: Scrape Main Page
    Engine->>Fetcher: fetch(seedUrl)
    Fetcher-->>Engine: rawHtml
    Engine->>Processor: process(rawHtml, options)
    Processor-->>Engine: processedHtml, hrefs
    Engine->>Extractor: extractFields(document, page.fields)
    Extractor-->>Engine: Map~String, Object~ fields
    Engine->>Engine: Store main page result
    
    Note over Engine,Fetcher: Step 2: Scrape Sub-Pages
    loop For each subPage config
        Engine->>Fetcher: fetch(seedUrl + subPage.path)
        Fetcher-->>Engine: rawHtml
        Engine->>Processor: process(rawHtml, options)
        Processor-->>Engine: processedHtml
        Engine->>Extractor: extractFields(document, subPage.fields)
        Extractor-->>Engine: Map~String, Object~ fields
        Engine->>Engine: Store sub-page result
    end
    
    Note over Engine,Storage: Step 3: Combine & Store
    Engine->>Engine: Merge all page data
    Engine->>Storage: Save raw HTML, processed HTML, JSON
    Engine-->>Client: MultiPageScrapeResult
```

### Field Extraction Detail

```mermaid
flowchart TD
    Start[Start Field Extraction] --> Loop{For Each Field}
    Loop -->|No more fields| End[Return Results Map]
    Loop -->|Next Field| CheckMultiple{Is Multiple?}
    
    CheckMultiple -->|Yes| ExtractMulti[extractMultiple]
    CheckMultiple -->|No| ExtractSingle[extractSingle]
    
    ExtractMulti --> LoopSelectors{For Each Selector}
    LoopSelectors -->|No more selectors| CheckEmpty{Any values?}
    LoopSelectors -->|Next Selector| SelectElements[document.select selector]
    SelectElements --> ForEach[For Each Element]
    ForEach --> ExtractVal[extractValue]
    ExtractVal --> ApplyPattern[applyPattern regex]
    ApplyPattern --> AddToList[Add to values list]
    AddToList --> LoopSelectors
    
    CheckEmpty -->|Empty| LogWarn[Log warning]
    CheckEmpty -->|Has values| ReturnList[Return List~String~]
    ReturnList --> StoreResult[Store in results map]
    StoreResult --> Loop
    
    ExtractSingle --> LoopSelectors2{For Each Selector}
    LoopSelectors2 -->|No more selectors| CheckNull{Found value?}
    LoopSelectors2 -->|Next Selector| SelectFirst[document.select first]
    SelectFirst -->|Not found| LoopSelectors2
    SelectFirst -->|Found| ExtractVal2[extractValue]
    ExtractVal2 --> ApplyPattern2[applyPattern regex]
    ApplyPattern2 --> ReturnVal[Return String]
    ReturnVal --> StoreResult2[Store in results map]
    StoreResult2 --> Loop
    
    CheckNull -->|Null| LogWarn2[Log warning]
    CheckNull -->|Has value| StoreResult2
    
    LogWarn --> Loop
    LogWarn2 --> Loop

    style Start fill:#e8f5e9
    style End fill:#e8f5e9
    style ReturnList fill:#fff4e1
    style ReturnVal fill:#fff4e1
```

## Fetcher Selection

```mermaid
flowchart TD
    Start[Scrape Request] --> CheckSettings{Check SettingsService}
    CheckSettings -->|PLAYWRIGHT| Playwright[PlaywrightFetcher<br/>Full browser automation<br/>Chromium/Firefox/WebKit]
    CheckSettings -->|HTMLUNIT| HtmlUnit[HtmlFetcher<br/>HtmlUnit headless browser<br/>Lightweight JS support]
    
    Playwright --> LaunchBrowser[Launch browser instance]
    LaunchBrowser --> NewContext[Create browser context]
    NewContext --> NewPage[Open new page]
    NewPage --> Navigate[Navigate to URL]
    Navigate --> WaitForIdle[Wait for NETWORKIDLE]
    WaitForIdle --> GetContent[Get page.content]
    GetContent --> ReturnHTML[Return HTML]
    
    HtmlUnit --> BuildClient[Build WebClient]
    BuildClient --> ConfigClient[Configure: JS enabled,<br/>CSS enabled, timeout]
    ConfigClient --> LoadPage[Load HtmlPage]
    LoadPage --> WaitForJS[Wait for background JS]
    WaitForJS --> GetXml[Get page.asXml]
    GetXml --> ReturnHTML
    
    style Playwright fill:#e3f2fd
    style HtmlUnit fill:#e3f2fd
    style ReturnHTML fill:#e8f5e9
```

### Fetcher Comparison

| Feature | HtmlFetcher (HtmlUnit) | PlaywrightFetcher |
|---------|----------------------|-------------------|
| **Browser Engine** | HtmlUnit (legacy) | Chromium/Firefox/WebKit |
| **JavaScript** | Basic support | Full support |
| **Speed** | Faster | Slower (real browser) |
| **Accuracy** | May miss dynamic content | Renders like real user |
| **Resource Usage** | Low | High |
| **Recommended For** | Static/simple pages | Facebook, SPAs, dynamic content |

## HTML Processing Pipeline

```mermaid
flowchart LR
    RawHTML[Raw HTML] --> Parse[Jsoup.parse]
    Parse --> Document[HTML Document]
    
    Document --> StripCSS{stripCss?}
    StripCSS -->|Yes| RemoveStyle[Remove style, link<br/>rel=stylesheet]
    StripCSS -->|No| StripJS{stripJs?}
    RemoveStyle --> StripJS
    
    StripJS -->|Yes| RemoveScript[Remove script tags]
    StripJS -->|No| RemoveAttr{removeAttributes?}
    RemoveScript --> RemoveAttr
    
    RemoveAttr -->|Yes| FilterAttrs[Keep only:<br/>href, src, alt, title]
    RemoveAttr -->|No| ExtractHrefs
    
    FilterAttrs --> ExtractHrefs[Extract hrefs using<br/>hrefSelector]
    ExtractHrefs --> PrettyPrint[Disable pretty-print]
    PrettyPrint --> Output[Processed HTML]
    
    style RawHTML fill:#ffebee
    style Output fill:#e8f5e9
    style ExtractHrefs fill:#fff4e1
```

## Data Flow & Output

```mermaid
flowchart TB
    subgraph Input["Input"]
        SeedURL[Seed URL<br/>e.g., facebook.com/username]
        Recipe[YAML Recipe]
    end
    
    subgraph Process["Processing"]
        MainPage[Scrape Main Page]
        AboutPage[Scrape /about]
        PhotosPage[Scrape /photos]
        Merge[Merge All Results]
    end
    
    subgraph Output["Output Files"]
        RawDir[data/jobs/{jobId}/raw/]
        ProcDir[data/jobs/{jobId}/processed/]
        JsonDir[data/jobs/{jobId}/json/]
    end
    
    Input --> MainPage
    MainPage --> AboutPage
    MainPage --> PhotosPage
    AboutPage --> Merge
    PhotosPage --> Merge
    Merge --> RawDir
    Merge --> ProcDir
    Merge --> JsonDir
    
    RawDir --> RawFiles[raw-{timestamp}.html]
    ProcDir --> ProcFiles[processed-{timestamp}.html]
    JsonDir --> JsonFiles[{timestamp}.json]
    
    JsonFiles --> StructuredData["Structured JSON:<br/>{<br/>  title: '...',<br/>  avatar: '...',<br/>  about_description: '...',<br/>  contact_info: '...',<br/>  website_url: '...',<br/>  photo_count: '...',<br/>  recent_photos: [...],<br/>  url: '...',<br/>  timestamp: '...'<br/>}"]
    
    style Input fill:#e1f5ff
    style Process fill:#fff4e1
    style Output fill:#e8f5e9
    style StructuredData fill:#f3e5f5
```

## Complete End-to-End Flow

```mermaid
flowchart TB
    Start[User Initiates Scrape] --> LoadRecipe[Load YAML Recipe]
    LoadRecipe --> ValidateMatch{URL Matches<br/>urlRegexes?}
    ValidateMatch -->|No| Skip[Skip Recipe]
    ValidateMatch -->|Yes| SelectFetcher{Check Settings}
    
    SelectFetcher -->|Playwright| UsePlaywright[Use PlaywrightFetcher]
    SelectFetcher -->|HtmlUnit| UseHtmlUnit[Use HtmlFetcher]
    
    UsePlaywright --> FetchMain[Fetch Main Page]
    UseHtmlUnit --> FetchMain
    
    FetchMain --> ProcessMain[Process HTML:<br/>Strip CSS/JS/Attrs]
    ProcessMain --> ExtractMain[Extract Fields:<br/>title, avatar, content]
    ExtractMain --> StoreMain[Store Main Result]
    
    StoreMain --> HasSubPages{Has<br/>subPages?}
    HasSubPages -->|No| FinalMerge[Merge & Save]
    HasSubPages -->|Yes| LoopSubPages[For Each Sub-Page]
    
    LoopSubPages --> BuildURL[Build URL:<br/>seedUrl + path]
    BuildURL --> FetchSub[Fetch Sub-Page]
    FetchSub --> ProcessSub[Process HTML]
    ProcessSub --> ExtractSub[Extract Sub-Page Fields]
    ExtractSub --> StoreSub[Store Sub-Page Result]
    StoreSub --> MoreSubPages{More<br/>Sub-Pages?}
    MoreSubPages -->|Yes| LoopSubPages
    MoreSubPages -->|No| FinalMerge
    
    FinalMerge --> ConvertMD[Convert to Markdown]
    ConvertMD --> SaveFiles[Save Files:<br/>raw.html, processed.html, data.json, output.md]
    SaveFiles --> ReturnResult[Return MultiPageScrapeResult]
    ReturnResult --> End[Complete]
    
    style Start fill:#e8f5e9
    style End fill:#e8f5e9
    style FinalMerge fill:#fff4e1
    style SaveFiles fill:#e1f5ff
```

## Key Implementation Details

### 1. URL Matching

The scraper checks if a URL matches the recipe's `urlRegexes`:

```java
// MatchConfig.java
public class MatchConfig {
    private List<String> urlRegexes;
    
    public boolean matches(String url) {
        return urlRegexes.stream()
            .anyMatch(regex -> url.matches(regex));
    }
}
```

### 2. Multi-Page Execution

```java
// ScraperEngine.executeMultiPage()
public MultiPageScrapeResult executeMultiPage(RecipeConfig recipe,
                                              String seedUrl,
                                              OptionsConfig overrides,
                                              ProgressListener progressListener) {
    // 1. Scrape main page
    ScrapeExecutionResult mainResult = execute(recipe, seedUrl, overrides, listener);
    pageResults.put("main", new PageResult(seedUrl, ...));
    
    // 2. Scrape each sub-page
    for (SubPageConfig subPage : recipe.getPage().getSubPages()) {
        String subPageUrl = buildSubPageUrl(seedUrl, subPage.getPath());
        ScrapeExecutionResult subResult = scrapeSubPage(recipe, subPageUrl, subPage, ...);
        pageResults.put(pageKey, new PageResult(subPageUrl, ...));
    }
    
    // 3. Merge all results
    return new MultiPageScrapeResult(combinedData, pageResults, progress);
}
```

### 3. Field Extraction

```java
// FieldExtractionService.extractSingle()
private Object extractSingle(Document document, FieldConfig field) {
    for (String selector : field.getSelectors()) {
        Element element = document.select(selector).stream().findFirst().orElse(null);
        if (element != null) {
            String value = extractValue(element, field);
            if (value != null && !value.isBlank()) {
                return applyPattern(value, field.getDataPattern());
            }
        }
    }
    return null;
}
```

### 4. Sub-Page URL Construction

```java
// ScraperEngine.buildSubPageUrl()
private String buildSubPageUrl(String seedUrl, String path) {
    String cleanSeedUrl = seedUrl.endsWith("/") ? 
        seedUrl.substring(0, seedUrl.length() - 1) : seedUrl;
    String cleanPath = path.startsWith("/") ? path : "/" + path;
    return cleanSeedUrl + cleanPath;
}

// Example:
// seedUrl: "https://www.facebook.com/username"
// path: "/about"
// Result: "https://www.facebook.com/username/about"
```

## Output Structure

### JSON Output Example

```json
{
  "title": "Facebook Page Name",
  "avatar": "data:image/svg+xml;base64,...",
  "main_page_content": "User posts and timeline content...",
  "about_description": "Page description text...",
  "contact_info": "Contact information...",
  "website_url": "https://example.com",
  "photo_count": "1,234",
  "recent_photos": [
    "https://scontent.xx.fbcdn.net/v/photo1.jpg",
    "https://scontent.xx.fbcdn.net/v/photo2.jpg"
  ],
  "url": "https://www.facebook.com/username",
  "timestamp": "2026-03-04T10:30:00Z",
  "hrefs": [
    "https://www.facebook.com/username/about",
    "https://www.facebook.com/username/photos",
    "https://www.facebook.com/username/posts"
  ]
}
```

### File Storage Structure

```
data/
└── jobs/
    └── {jobId}/
        ├── raw/
        │   └── raw-{timestamp}.html
        ├── processed/
        │   └── processed-{timestamp}.html
        └── json/
            └── {timestamp}.json
```

## Limitations & Considerations

```mermaid
mindmap
  root((Facebook<br/>Scraping<br/>Limitations))
    Technical
      Dynamic class names
      Anti-bot measures
      Login walls
      Rate limiting
    Configuration
      Manual CSS selector updates
      No semantic understanding
      Recipe maintenance overhead
    Content
      Public pages only
      No private content
      Limited by Facebook DOM
    Performance
      Playwright is resource-heavy
      JavaScript wait times
      Network idle delays
```

### Key Limitations

1. **No LLM/AI Intelligence**: Pure CSS selector-based extraction
2. **Manual Maintenance**: DOM changes require recipe updates
3. **Public Content Only**: Cannot scrape behind login walls
4. **Fragile Selectors**: Facebook's dynamic class names may break selectors
5. **Resource Intensive**: Playwright requires browser instances

## Best Practices

1. **Use Playwright for Facebook**: Better JavaScript rendering
2. **Test Selectors Regularly**: Facebook updates DOM frequently
3. **Use Data Attributes**: Prefer `[data-pagelet='...']` over class names
4. **Handle Errors Gracefully**: Sub-page failures don't stop main scrape
5. **Monitor Progress**: Use `ProgressListener` for long-running scrapes

## Related Files

| File | Purpose |
|------|---------|
| `facebook-multipage-example.yaml` | Example Facebook recipe |
| `ScraperEngine.java` | Core scraping orchestration |
| `FieldExtractionService.java` | CSS selector-based extraction |
| `HtmlProcessingService.java` | HTML cleaning & href extraction |
| `PlaywrightFetcher.java` | Full browser fetching |
| `HtmlFetcher.java` | Lightweight HtmlUnit fetching |
| `RecipeConfig.java` | Recipe configuration model |
| `PageConfig.java` | Page-level configuration |
| `SubPageConfig.java` | Sub-page configuration |
