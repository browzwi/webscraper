# WebScraper Usage Guide

A step-by-step guide on how to use the WebScraper application.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Creating Your First Scraping Recipe](#creating-your-first-scraping-recipe)
3. [Creating a Scraping Job](#creating-a-scraping-job)
4. [Monitoring Job Progress](#monitoring-job-progress)
5. [Viewing Results](#viewing-results)
6. [Example Recipes](#example-recipes)
7. [Tips & Best Practices](#tips--best-practices)

---

## Getting Started

### 1. Access the Application

Open your browser and navigate to:
```
http://172.22.74.128:8080
```

### 2. Login

Use the default credentials:
- **Username:** `admin`
- **Password:** `admin`

### 3. Dashboard

After logging in, you'll see the dashboard with:
- Overview of all scraping jobs
- Quick stats (total jobs, completed, running, failed)
- Recent activity

---

## Creating Your First Scraping Recipe

A **Recipe** defines what data to extract from a website.

### Step 1: Navigate to Recipes

1. Click on **"Recipes"** in the left sidebar
2. Click **"New Recipe"** button

### Step 2: Fill in Recipe Details

| Field | Description |
|-------|-------------|
| **Name** | A descriptive name (e.g., "Product Scraper") |
| **Description** | What this recipe does |
| **YAML Content** | The scraping configuration |

### Step 3: Define Your Recipe (YAML)

Here's a simple example to scrape quotes from a practice website:

```yaml
name: Quotes Scraper
description: Scrapes quotes from quotes.toscrape.com
match:
  domains:
    - quotes.toscrape.com
page:
  contentRoot: "div.quote"
  fields:
    - name: quote
      selectors: ["span.text"]
      source: TEXT
    - name: author
      selectors: ["small.author"]
      source: TEXT
    - name: tags
      selectors: ["div.tags a.tag"]
      source: TEXT
      multiple: true
options:
  fetcherType: HTMLUNIT
```

### Step 4: Test Your Recipe

1. Click **"Test Recipe"** button
2. Enter a test URL: `https://quotes.toscrape.com`
3. Click **"Run Test"**
4. Review the extracted data in the results panel

### Step 5: Save the Recipe

Click **"Save Recipe"** to store it for use in jobs.

---

## Creating a Scraping Job

A **Job** uses a recipe to scrape specific URLs.

### Step 1: Navigate to Jobs

1. Click on **"Jobs"** in the left sidebar
2. Click **"New Job"** button

### Step 2: Fill in Job Details

| Field | Description | Example |
|-------|-------------|---------|
| **Job Name** | Descriptive name | "Daily Quotes Scraping" |
| **Recipe** | Select from dropdown | "Quotes Scraper" |
| **Target URLs** | URLs to scrape (one per line) | `https://quotes.toscrape.com` |
| **Schedule** | Cron expression (optional) | Leave empty for one-time |

### Step 3: Configure Schedule (Optional)

For recurring jobs, use cron expressions:

| Schedule | Cron Expression |
|----------|-----------------|
| Every hour | `0 0 * * * ?` |
| Daily at midnight | `0 0 0 * * ?` |
| Every 30 minutes | `0 0/30 * * * ?` |
| Weekdays at 9 AM | `0 0 9 * * MON-FRI` |

### Step 4: Create the Job

Click **"Create Job"** to save and optionally start the job.

---

## Monitoring Job Progress

### View Job List

1. Go to **Jobs** → **Job List**
2. See all jobs with their status:
   - 🟡 **PENDING** - Waiting to start
   - 🔵 **RUNNING** - Currently scraping
   - 🟢 **COMPLETED** - Finished successfully
   - 🔴 **FAILED** - Error occurred

### View Job Details

1. Click on a job name
2. See detailed information:
   - Job configuration
   - Target URLs with individual status
   - Progress indicators
   - Error messages (if any)

### Real-Time Progress

For running jobs, you can:
- Watch the progress bar update
- See which URLs are being processed
- View errors as they occur

---

## Viewing Results

### Access Scraped Data

1. Go to **Jobs** → Select a completed job
2. Click **"View Results"** button
3. You'll see:
   - **Structured Data** (JSON format)
   - **Processed HTML** (cleaned HTML)
   - **Markdown** (converted content)

### Download Results

Click **"Download"** to save:
- JSON data file
- HTML files
- Markdown files
- Complete archive (ZIP)

### Example Output (JSON)

```json
{
  "quote": "The world as we have created it is a process of our thinking.",
  "author": "Albert Einstein",
  "tags": ["change", "deep-thoughts", "thinking"]
}
```

---

## Example Recipes

### 1. Simple Article Scraper

```yaml
name: Article Scraper
description: Extracts article title, content, and author
match:
  domains:
    - example.com
page:
  contentRoot: "article"
  fields:
    - name: title
      selectors: ["h1.article-title"]
      source: TEXT
    - name: author
      selectors: ["span.author"]
      source: TEXT
    - name: content
      selectors: ["div.article-body"]
      source: HTML
    - name: publishDate
      selectors: ["time.published"]
      source: TEXT
      dataPattern: "\\d{4}-\\d{2}-\\d{2}"
options:
  fetcherType: HTMLUNIT
  stripCss: true
  stripJs: true
```

### 2. E-commerce Product Scraper

```yaml
name: Product Scraper
description: Scrapes product information
match:
  domains:
    - example-shop.com
page:
  contentRoot: "div.product-card"
  fields:
    - name: productName
      selectors: ["h2.product-name"]
      source: TEXT
    - name: price
      selectors: ["span.price"]
      source: TEXT
      dataPattern: "\\$?([\\d.]+)"
    - name: image
      selectors: ["img.product-image"]
      source: ATTR
      attributeName: src
    - name: description
      selectors: ["p.product-description"]
      source: TEXT
    - name: rating
      selectors: ["div.rating span"]
      source: TEXT
options:
  fetcherType: PLAYWRIGHT
  waitForLoad: true
```

### 3. Multi-Page Scraper (Main + Sub-pages)

```yaml
name: Site Crawler
description: Scrapes main page and sub-pages
match:
  domains:
    - example.com
page:
  hrefSelector: "a[href^='/']"
  fields:
    - name: title
      selectors: ["h1"]
      source: TEXT
    - name: description
      selectors: ["meta[name='description']"]
      source: ATTR
      attributeName: content
  subPages:
    - name: about
      path: "/about"
      fields:
        - name: aboutText
          selectors: ["div.about-content"]
          source: TEXT
    - name: contact
      path: "/contact"
      fields:
        - name: email
          selectors: ["a[href^='mailto:']"]
          source: ATTR
          attributeName: href
        - name: phone
          selectors: ["a[href^='tel:']"]
          source: TEXT
options:
  fetcherType: HTMLUNIT
```

### 4. JavaScript-Heavy Site Scraper

```yaml
name: React Site Scraper
description: For sites that require JavaScript
match:
  domains:
    - react-app.com
page:
  fields:
    - name: dynamicContent
      selectors: ["div#app-content"]
      source: HTML
    - name: loadedItems
      selectors: ["div.item"]
      source: TEXT
      multiple: true
options:
  fetcherType: PLAYWRIGHT
  waitForLoad: true
  timeout: 30000
```

---

## Tips & Best Practices

### 🎯 Recipe Design

1. **Start Simple** - Begin with basic selectors, then add complexity
2. **Test First** - Always test your recipe before creating jobs
3. **Use Specific Selectors** - Prefer ID and class selectors over generic tags
4. **Handle Multiple Values** - Use `multiple: true` for lists/arrays

### 📋 Selector Tips

| Type | Example | When to Use |
|------|---------|-------------|
| **ID** | `#main-content` | Most specific, fastest |
| **Class** | `.product-name` | Common, reliable |
| **Tag** | `h1`, `p` | Generic, use with caution |
| **Attribute** | `[data-id]` | When data is in attributes |
| **Combined** | `div.product > h2.title` | Precise targeting |

### ⚙️ Fetcher Selection

| Fetcher | Use For | Speed |
|---------|---------|-------|
| **HTMLUNIT** | Static HTML sites | Fast |
| **PLAYWRIGHT** | JavaScript-heavy sites (React, Vue, Angular) | Slower but powerful |

### 🕐 Scheduling Tips

1. **Respect Websites** - Don't scrape too frequently
2. **Off-Peak Hours** - Schedule jobs during low-traffic times
3. **Rate Limiting** - Add delays between requests if needed
4. **Monitor Failures** - Set up alerts for failed jobs

### 🔧 Troubleshooting

| Issue | Solution |
|-------|----------|
| No data extracted | Check CSS selectors, test in browser DevTools |
| Empty results | Verify the page structure matches your selectors |
| Timeout errors | Increase timeout in options or switch to Playwright |
| JavaScript content not loading | Use PLAYWRIGHT fetcher instead of HTMLUNIT |
| 403 Forbidden | Check robots.txt, add user-agent headers |

### 📊 Data Extraction Options

| Source Type | Description | Example |
|-------------|-------------|---------|
| `TEXT` | Extract visible text | `"Hello World"` |
| `HTML` | Extract inner HTML | `"<b>Hello</b>"` |
| `ATTR` | Extract attribute | `src`, `href`, `data-*` |

### 🔒 Legal & Ethical Considerations

1. **Check robots.txt** - `https://example.com/robots.txt`
2. **Respect Terms of Service** - Read the website's ToS
3. **Rate Limiting** - Don't overload servers
4. **Copyright** - Be mindful of scraped content usage
5. **Personal Data** - Avoid scraping PII without consent

---

## Quick Reference

### Common CSS Selectors

```css
/* By ID */
#main-content

/* By Class */
.product-card

/* By Tag */
h1, p, div

/* By Attribute */
[data-product-id]
[href^="https://"]

/* Combined */
div.products > a.product-link

/* Pseudo-selectors */
a[href]:first-child
li:nth-child(2)
```

### Cron Expression Format

```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12)
│ │ │ │ │ ┌───────────── day of week (MON-SUN)
│ │ │ │ │ │
│ │ │ │ │ │
* * * * * ?
```

### Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Save Form | `Ctrl+S` |
| Test Recipe | `Ctrl+T` |
| Refresh Page | `F5` |
| Open Navigation | `Ctrl+K` |

---

## Support & Resources

- **Documentation:** Check `README.md` and `CODEBASE_GUIDE.md`
- **Logs:** View application logs for debugging
- **H2 Console:** http://172.22.74.128:8080/h2-console (for database inspection)

---

**Happy Scraping!** 🚀
