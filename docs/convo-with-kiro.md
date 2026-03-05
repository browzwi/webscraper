# Conversation with Kiro

## Google Search Scraping - March 6, 2026

### Key Points Discussed

#### 1. Google Search Working Successfully
- Found 178 result blocks on first search for "Law Firm Cavite"
- Successfully bypassed bot detection using persistent browser context
- Manual setup (cookies/CAPTCHA) worked on first run
- Subsequent runs reuse saved session

#### 2. Location Filtering Issue
- Google Search returns results from all over Philippines, not just Cavite
- Unlike Google Maps which filters by location properly
- Recommendation: Use Google Maps for location-based searches (already gets 60+ results)

#### 3. JavaScript Errors During Website Scraping
- HtmlUnit throws JavaScript errors when scraping complex websites
- These errors are normal and already suppressed (`setThrowExceptionOnScriptError(false)`)
- Doesn't stop the scraping process - just noise in logs
- Successfully extracted business data despite errors

#### 4. Discovery Sources Explained
- **Google Search** → searches all websites
- **Facebook** → adds `site:facebook.com` to search (only Facebook pages)
- **Instagram** → adds `site:instagram.com` (only Instagram)
- **Google Maps** → searches Google Maps directly

#### 5. Why `site:facebook.com` in Search Bar
- It's a Google Search operator, not going to Facebook directly
- App uses Google to find Facebook pages (easier to scrape than Facebook itself)
- More efficient than scraping all results and filtering afterwards
- Google already indexed all Facebook pages

#### 6. Results from Testing
- Law Firm Cavite search: Only 1 business enriched (ANARNA LAW OFFICE)
- Low conversion rate from 178 blocks to actual businesses
- Accounting Cavite search: 20 result blocks found (Facebook-only search)

### Alternative Approach Discussed
Instead of `site:facebook.com` in search query:
1. Search Google normally for everything
2. Get all results (websites, social media, etc.)
3. Filter afterwards by domain

**Pros:** One search, more flexible, can filter multiple platforms
**Cons:** Slower, Google prioritizes regular websites over social media

### Current Status
- Google Search scraping works but needs location filtering improvement
- Google Maps recommended for local business searches
- Individual website scraping working despite JavaScript errors
- Persistent browser context successfully avoiding bot detection
