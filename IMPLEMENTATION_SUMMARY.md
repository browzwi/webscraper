# Implementation Summary: Target Page Type Configuration

## Changes Implemented

### 1. **Created TargetPageType Enum**
- **File**: `src/main/java/com/browzwi/webscraper/service/settings/TargetPageType.java`
- **Purpose**: Define supported target page types (Facebook, Website, Instagram, LinkedIn, Twitter, TikTok, YouTube, Custom)
- **Features**:
  - Each type has display name, target domain, and search pattern
  - `buildSearchQuery()` method generates Google Search queries
  - Supports custom patterns for flexible searches

### 2. **Updated Domain Entities**

#### DiscoveredBusiness.java
- Added `targetUrl` field - stores the discovered URL (Facebook, Instagram, etc.)
- Added `targetPageType` field - stores the type of page discovered
- Added getters/setters for new fields

#### DiscoveryJob.java
- Added `targetPageType` field - stores user's selected target type
- Added `customSearchPattern` field - stores custom search pattern (for CUSTOM type)
- Added getters/setters for new fields

### 3. **Updated Form Objects**

#### DiscoveryForm (in DiscoveryController.java)
- Added `targetPageType` field (default: "FACEBOOK")
- Added `customPattern` field (for custom searches)
- Added getters/setters

### 4. **Refactored GoogleSearchDiscoveryService**

**Before**: Hardcoded Facebook-only discovery
```java
private static final String TARGET_DOMAIN = "facebook.com";
public String discoverFacebookUrl(String businessName, String location)
```

**After**: Configurable target type discovery
```java
public String discoverTargetUrl(String businessName, String location, 
                                TargetPageType pageType, String customPattern)
```

**Key Changes**:
- Removed hardcoded `TARGET_DOMAIN` constant
- Method now accepts `TargetPageType` parameter
- Uses `pageType.buildSearchQuery()` to generate search queries
- Updated `isValidDomain()` to support multiple target types
- Special handling for WEBSITE type (accepts any non-blocked domain)

### 5. **Updated UrlDiscoveryService**

**Method Signatures Updated**:
```java
// Before
public DiscoveryJob discover(String keyword, String location, Integer maxResults)
public void discoverAsync(Long jobId, String keyword, String location, Integer maxResults)
private List<DiscoveredBusiness> discoverViaMapsAndSearch(String keyword, String location, Integer maxResults)

// After
public DiscoveryJob discover(String keyword, String location, String targetPageType, String customPattern, Integer maxResults)
public void discoverAsync(Long jobId, String keyword, String location, String targetPageType, String customPattern, Integer maxResults)
private List<DiscoveredBusiness> discoverViaMapsAndSearch(String keyword, String location, String targetPageType, String customPattern, Integer maxResults)
```

**Key Changes**:
- Calls `googleSearchDiscoveryService.discoverTargetUrl()` instead of `discoverFacebookUrl()`
- Sets `business.setTargetUrl()` and `business.setTargetPageType()` instead of `setWebsiteUrl()`
- Logs now show dynamic page type (e.g., "Finding Instagram URLs")

### 6. **Updated DiscoveryController**

**startDiscovery() Method**:
- Saves `targetPageType` and `customSearchPattern` to DiscoveryJob
- Passes new parameters to `urlDiscoveryService.discoverAsync()`

### 7. **Database Migration**

**File**: `src/main/resources/db/changelog/007-add-target-page-type-fields.yaml`

**Changes**:
- Added `target_page_type` column to `discovery_job` table (default: 'FACEBOOK')
- Added `custom_search_pattern` column to `discovery_job` table
- Added `target_url` column to `discovered_business` table
- Added `target_page_type` column to `discovered_business` table

**Master Changelog Updated**: Added reference to new migration file

### 8. **UI Updates**

#### discovery/index.html
**Before**: 3-column grid (Keyword, Location, Max Results)

**After**: 
- First row: Keyword, Location, **Target Page Type** (dropdown)
- Second row: **Custom Pattern** (text input), Max Results

**Target Type Dropdown Options**:
- Facebook
- Official Website
- Instagram
- LinkedIn
- Twitter/X
- TikTok
- YouTube
- Custom

#### discovery/results.html
**Before**: Showed `websiteUrl` column

**After**: 
- Shows `targetUrl` column (the discovered URL)
- Shows `targetPageType` column (badge showing type: FACEBOOK, INSTAGRAM, etc.)

---

## How It Works Now

### Discovery Flow

1. **User Input**:
   - Keyword: "law firm"
   - Location: "Kawit, Cavite"
   - Target Page Type: "FACEBOOK" (or Instagram, Website, etc.)
   - Custom Pattern: (optional, only for CUSTOM type)

2. **Google Maps Phase**:
   - Searches Google Maps for "law firm Kawit, Cavite"
   - Extracts business names, addresses, phone numbers

3. **Google Search Phase**:
   - For each business, builds search query based on target type:
     - FACEBOOK: "{businessName} {location} Facebook"
     - INSTAGRAM: "{businessName} {location} Instagram"
     - WEBSITE: "{businessName} {location} official website"
     - etc.
   - Searches Google and extracts URL matching target domain
   - Saves to `targetUrl` field with `targetPageType`

4. **Results**:
   - Table shows discovered businesses with their target URLs
   - Each row displays the target page type (Facebook, Instagram, etc.)
   - Status shows DISCOVERED or NO_URL_FOUND

---

## Example Usage

### Discover Facebook Pages
```
Keyword: restaurant
Location: Makati
Target Page Type: Facebook
→ Finds Facebook pages for restaurants in Makati
```

### Discover Official Websites
```
Keyword: law firm
Location: BGC
Target Page Type: Official Website
→ Finds official websites for law firms in BGC
```

### Discover Instagram Profiles
```
Keyword: salon
Location: Quezon City
Target Page Type: Instagram
→ Finds Instagram profiles for salons in Quezon City
```

### Custom Search
```
Keyword: restaurant
Location: Manila
Target Page Type: Custom
Custom Pattern: menu
→ Searches for "restaurant Manila menu" (finds menu pages)
```

---

## Architecture Alignment

✅ **Discovery defines WHAT pages to target** (via targetPageType)
✅ **Recipe defines HOW to extract data** (via selectors)
✅ **Job executes Recipe on discovered URLs**

The system now matches the architecture document's vision:
- Discovery is configurable (not hardcoded to Facebook)
- Target type is stored in Discovery entities
- Recipes remain reusable across different target types
- Clear separation of concerns

---

## Testing Checklist

- [ ] Run application and verify no compilation errors
- [ ] Check Liquibase migration runs successfully
- [ ] Test discovery form with different target types
- [ ] Verify Google Search queries are built correctly
- [ ] Confirm targetUrl and targetPageType are saved to database
- [ ] Check results table displays target URLs and types correctly
- [ ] Test CUSTOM type with custom pattern
- [ ] Verify WEBSITE type accepts any non-blocked domain

---

## Next Steps (Optional Enhancements)

1. **Add target type filter to results page** - Filter businesses by discovered type
2. **Show target type in progress page** - Display type during discovery
3. **Add validation** - Require customPattern when CUSTOM type is selected
4. **Export enhancement** - Include targetPageType in Excel export
5. **Recipe matching** - Suggest recipes based on targetPageType
