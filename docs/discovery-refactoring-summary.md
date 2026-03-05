# Discovery System Refactoring - Summary

## Changes Completed

### 1. DiscoverySourceType.java
**Location:** `src/main/java/com/browzwi/webscraper/service/settings/DiscoverySourceType.java`

**Changes:**
- Removed: `GOOGLE_PLACES_API`, `PLAYWRIGHT`, `BRAVE_SEARCH`
- Added: `GOOGLE_MAPS` (renamed from PLAYWRIGHT), `GOOGLE_SEARCH` (new)
- Both sources now use Playwright only - no paid APIs

### 2. SettingsService.java
**Location:** `src/main/java/com/browzwi/webscraper/service/settings/SettingsService.java`

**Changes:**
- Removed: `GOOGLE_PLACES_API_KEY`, `BRAVE_SEARCH_API_KEY` constants
- Removed: `getGooglePlacesApiKey()`, `updateGooglePlacesApiKey()`, `getBraveSearchApiKey()`, `updateBraveSearchApiKey()` methods
- Added: `GOOGLE_SEARCH_SITE_FILTER_KEY` constant
- Added: `getGoogleSearchSiteFilter()`, `updateGoogleSearchSiteFilter()` methods
- Updated default discovery source to `GOOGLE_MAPS`

### 3. SettingsForm.java
**Location:** `src/main/java/com/browzwi/webscraper/web/dto/SettingsForm.java`

**Changes:**
- Removed: `googlePlacesApiKey`, `braveSearchApiKey` fields
- Added: `googleSearchSiteFilter` field

### 4. SettingsController.java
**Location:** `src/main/java/com/browzwi/webscraper/web/SettingsController.java`

**Changes:**
- Removed: API key binding in `view()` and `update()` methods
- Added: `googleSearchSiteFilter` binding

### 5. GoogleSearchDiscoveryService.java (NEW)
**Location:** `src/main/java/com/browzwi/webscraper/service/GoogleSearchDiscoveryService.java`

**Features:**
- Scrapes Google Search results using Playwright
- Supports site filtering (e.g., "facebook.com")
- Extracts: title, URL, snippet
- Auto-detects social media platforms from URLs
- Implements anti-bot measures:
  - Randomized delays (1500-3500ms)
  - Realistic User-Agent
  - waitForSelector() for result confirmation
- Scrapes up to 3 pages (~30 results)
- Cleans Google redirect URLs (/url?q=...)
- Maps results to DiscoveredBusiness entities

### 6. UrlDiscoveryService.java
**Location:** `src/main/java/com/browzwi/webscraper/service/UrlDiscoveryService.java`

**Changes:**
- Removed: `discoverViaGooglePlacesApi()`, `enrichWithPlaceDetails()`, `discoverViaGoogleSearchPlaywright()` methods
- Removed: `ObjectMapper`, `HttpClient`, `AntiDetectPlaywrightService` dependencies
- Renamed: `discoverViaPlaywright()` → `discoverViaGoogleMaps()`
- Added: `GoogleSearchDiscoveryService` dependency injection
- Updated: `discover()` method to route to GOOGLE_MAPS or GOOGLE_SEARCH
- Removed: Unused imports (Jackson, HttpClient, etc.)

### 7. settings/index.html
**Location:** `src/main/resources/templates/settings/index.html`

**Changes:**
- Removed: 3-column grid with GOOGLE_PLACES_API, PLAYWRIGHT, GOOGLE_SEARCH_PLAYWRIGHT
- Added: 2-column grid with GOOGLE_MAPS and GOOGLE_SEARCH
- Removed: Google Places API key input field
- Added: Site Filter input field (shown only when GOOGLE_SEARCH is selected)
- Added: JavaScript `toggleSiteFilter()` function to show/hide site filter based on selection
- Updated: Icons, descriptions, and badges for new sources

## How It Works

### Google Maps Discovery (GOOGLE_MAPS)
1. Navigates to `https://www.google.com/maps/search/{keyword}+{location}`
2. Waits 5 seconds for results to load
3. Tries multiple selector strategies to find business cards
4. Extracts: business name, address, phone number, website
5. Returns up to 20 businesses

### Google Search Discovery (GOOGLE_SEARCH)
1. Constructs query: `{keyword} {location}` + optional `site:{filter}`
2. Navigates to `https://www.google.com/search?q={query}&start={page}`
3. Scrapes up to 3 pages (10 results per page)
4. Extracts: title (→ business_name), URL (→ website_url), snippet
5. Cleans Google redirect URLs
6. Auto-detects social media platforms
7. Returns up to 30 businesses

## Configuration

### Settings UI
- **Google Maps**: Scrapes Google Maps for business listings
- **Google Search**: Scrapes Google Search results with optional site filtering

### Site Filter (Google Search only)
- Optional field to filter results to specific domain
- Example: "facebook.com" to find only Facebook pages
- Leave blank to search all sites

## Testing Steps

1. **Stop the application** (Ctrl+C in PowerShell)

2. **Rebuild:**
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   ```

3. **Start:**
   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

4. **Test Google Maps:**
   - Go to Settings → Select "Google Maps" → Save
   - Go to Discovery → Enter "coffee shops" + "Makati" → Discover
   - Should find businesses with names, addresses, phones

5. **Test Google Search (all sites):**
   - Go to Settings → Select "Google Search" → Leave site filter blank → Save
   - Go to Discovery → Enter "coffee shops" + "Makati" → Discover
   - Should find businesses with names and website URLs

6. **Test Google Search (Facebook only):**
   - Go to Settings → Select "Google Search" → Enter "facebook.com" in site filter → Save
   - Go to Discovery → Enter "coffee shops" + "Makati" → Discover
   - Should find only Facebook pages

## Benefits

✅ **Simplified**: Only 2 discovery sources instead of 3
✅ **No API Keys**: Both sources are free, no paid APIs
✅ **Consistent**: Both use Playwright, same technology stack
✅ **Flexible**: Google Search supports site filtering for targeted discovery
✅ **Maintainable**: Removed complex API integration code
✅ **Clear Naming**: GOOGLE_MAPS and GOOGLE_SEARCH are self-explanatory

## Files Modified

1. `DiscoverySourceType.java` - Enum updated
2. `SettingsService.java` - Removed API key methods, added site filter
3. `SettingsForm.java` - Updated fields
4. `SettingsController.java` - Updated bindings
5. `UrlDiscoveryService.java` - Removed old methods, updated routing
6. `settings/index.html` - Simplified UI to 2 options

## Files Created

1. `GoogleSearchDiscoveryService.java` - New service for Google Search scraping

## Migration Notes

- Existing data in database is not affected
- Old settings (API keys) will remain in database but are unused
- Default discovery source is now GOOGLE_MAPS
- Users will need to reconfigure their discovery source preference
