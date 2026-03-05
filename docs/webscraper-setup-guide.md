# WebScraper - Complete Setup & Troubleshooting Guide

**Created:** March 5, 2026  
**Project:** WebScraper Spring Boot Application  
**Location:** `/mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main/`

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Features Implemented](#features-implemented)
3. [Current Status](#current-status)
4. [The Discovery Problem](#the-discovery-problem)
5. [Why It Happens](#why-it-happens)
6. [Solutions](#solutions)
7. [Installation Commands](#installation-commands)
8. [API Setup Guides](#api-setup-guides)
9. [Running the Application](#running-the-application)
10. [Access Information](#access-information)
11. [Troubleshooting](#troubleshooting)

---

## Project Overview

WebScraper is a Spring Boot-based web scraping platform with:
- **Backend:** Java 17, Spring Boot 3.5.7
- **Frontend:** Thymeleaf + HTMX + Tailwind CSS
- **Database:** MySQL (production) / H2 (local development)
- **Scraping Engines:** HtmlUnit, Playwright
- **Package:** `com.browzwi.webscraper`

---

## Features Implemented

### ✅ Working Features:

| Feature | Status | Description |
|---------|--------|-------------|
| **Scraper Engine** | ✅ Working | HtmlUnit (fast) or Playwright (JavaScript support) |
| **Recipes** | ✅ Working | Create YAML-based scraping configurations |
| **Jobs** | ✅ Working | Schedule and run scraping jobs |
| **Settings** | ✅ Working | Configure application settings |
| **Dashboard** | ✅ Working | View job statistics and status |
| **Authentication** | ✅ Working | Spring Security with admin user |
| **Excel Export** | ✅ Working | Export results to .xlsx files |

### ❌ Not Working:

| Feature | Status | Reason |
|---------|--------|--------|
| **URL Discovery** | ❌ Not Working | Missing browser dependencies for Playwright |

---

## Current Status

### Available Discovery Options (in Settings):

1. **Google Places API**
   - Requires API key from Google Cloud
   - Most reliable option
   - $200/month free credit (~28,000 searches)

2. **Playwright Scraper**
   - Scrapes Google Maps directly
   - ❌ Doesn't work - missing browser dependencies

3. **Google Search (Playwright)**
   - Anti-detection Google search
   - ❌ Doesn't work - missing browser dependencies

---

## The Discovery Problem

### What Happens:

```
User clicks "Start Discovery"
    ↓
Application tries to launch Chrome browser via Playwright
    ↓
ERROR: No Chromium browser installed in WSL!
    ↓
Nothing happens (silent failure)
    ↓
Page shows "0 businesses found" or loads indefinitely
```

### Error Logs Show:

```
Playwright Host validation warning:
Host system is missing dependencies to run browsers.
Please install them with:
    sudo mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI 
    -D exec.args="install-deps"
```

---

## Why It Happens

### NOT Because Of:

| Myth | Reality |
|------|---------|
| ❌ "Because I'm on localhost" | ✅ Works fine on localhost |
| ❌ "Because it's not deployed" | ✅ Works in development mode |
| ❌ "Because of firewall" | ✅ Not a network issue |
| ❌ "Because WSL can't access internet" | ✅ WSL has full internet access |
| ❌ "Because site can't be reached" | ✅ Server is running fine |

### The REAL Reason:

**Playwright needs a real Chrome/Chromium browser to operate.**

Currently on your WSL system:
- ❌ No Chromium browser installed for Playwright
- ❌ Missing system dependencies (libraries Playwright needs)
- ❌ No sudo access to install packages

**That's why Discovery doesn't proceed.**

---

## Solutions

### Solution 1: Install Browser Dependencies (Requires Sudo)

**Step 1: Get Sudo Access**

Check if you have sudo:
```bash
sudo whoami
```

If it asks for password, you have sudo but need the password.

If it says "user is not in sudoers file", enable it:

**Option A - Enable Root in WSL:**
1. Open Windows PowerShell as Administrator
2. Run: `wsl -u root`
3. Inside WSL: `echo "lanzcab12 ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers`
4. Exit: `exit`

**Option B - Add User to Sudo Group:**
1. Open Windows PowerShell as Administrator
2. Run: `wsl --shutdown` then `wsl -u root`
3. Inside WSL: `adduser lanzcab12 sudo`
4. Exit and restart WSL

**Step 2: Install Dependencies**

```bash
# Update package lists
sudo apt-get update

# Install Playwright browser dependencies
sudo apt-get install -y \
    libnss3 \
    libnspr4 \
    libasound2t64 \
    libatk-bridge2.0-0 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libatspi2.0-0

# Install Chromium browser for Playwright
cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main
./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# Restart the application
```

**Result:** ✅ Discovery will work with Playwright options

---

### Solution 2: Use API Instead (No Installation, Recommended)

Get a FREE API key that doesn't need browser installation.

#### Option A: Brave Search API (Recommended)

**Pros:**
- ✅ FREE (2,500 queries/month)
- ✅ No credit card required
- ✅ Works immediately
- ✅ No browser dependencies needed
- ✅ Most reliable option

**Setup:**
1. Visit: https://brave.com/search/api/
2. Click "Get Started" or "Sign Up"
3. Create account (email + password)
4. Verify email
5. Go to Dashboard → Copy API key
6. In WebScraper: Settings → URL Discovery → Select "Brave Search API"
7. Paste API key → Save

**Usage:** 2,500 queries × 20 results = **50,000 businesses/month FREE**

#### Option B: Google Places API

**Pros:**
- ✅ Official Google API
- ✅ Most reliable
- ✅ $200/month free credit (~28,000 searches)

**Cons:**
- ⚠️ Requires credit card for signup
- ⚠️ More complex setup

**Setup:**
1. Visit: https://console.cloud.google.com/
2. Create new project
3. Enable "Places API"
4. Create Credentials → API Key
5. Copy API key
6. In WebScraper: Settings → URL Discovery → Select "Google Places API"
7. Paste API key → Save

---

### Solution 3: Keep Current Setup

**Status:** Discovery won't work until you get API key or install dependencies.

**What Still Works:**
- ✅ Manual URL scraping (enter URLs directly in Jobs)
- ✅ Recipe creation and testing
- ✅ Job scheduling
- ✅ All other features

**Workaround:** Manually collect business URLs and add them to Jobs manually.

---

## Installation Commands

### Full Installation (If You Have Sudo):

```bash
# 1. Update system
sudo apt-get update

# 2. Install Playwright dependencies
sudo apt-get install -y libnss3 libnspr4 libasound2t64 libatk-bridge2.0-0 libdrm2 libxkbcommon0 libxcomposite1 libxdamage1 libxfixes3 libxrandr2 libgbm1 libpango-1.0-0 libcairo2 libatspi2.0-0

# 3. Install Chromium browser
cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main
./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# 4. Restart application
# (Stop current instance and run ./mvnw spring-boot:run -Dspring-boot.run.profiles=local)
```

### Quick Start (Application):

```bash
# Set Java home
export JAVA_HOME=/mnt/c/Users/lanzc/Downloads/zulu17.64.17-ca-jdk17.0.18-linux_x64/zulu17.64.17-ca-jdk17.0.18-linux_x64
export PATH=$JAVA_HOME/bin:$PATH

# Navigate to project
cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main

# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## API Setup Guides

### Brave Search API - Step by Step

1. **Visit Signup Page**
   - URL: https://brave.com/search/api/
   
2. **Create Account**
   - Enter email
   - Create password
   - Verify email

3. **Get API Key**
   - Login to dashboard
   - Navigate to "API Keys" section
   - Click "Generate New Key"
   - Copy the key (looks like: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`)

4. **Configure in WebScraper**
   - Login to WebScraper (admin/admin)
   - Go to Settings
   - Scroll to "URL Discovery"
   - Select "Brave Search API"
   - Paste API key
   - Click "Save Discovery Settings"

5. **Test**
   - Go to Discovery page
   - Enter keyword (e.g., "restaurant")
   - Enter location (e.g., "kawit, cavite")
   - Click "Start Discovery"
   - Should return real results in 2-5 seconds

### Google Places API - Step by Step

1. **Create Google Cloud Project**
   - Visit: https://console.cloud.google.com/
   - Click "Select a Project" → "New Project"
   - Name it (e.g., "WebScraper")
   - Click "Create"

2. **Enable Places API**
   - In Dashboard, click "Enable APIs and Services"
   - Search for "Places API"
   - Click "Enable"

3. **Create API Key**
   - Go to "Credentials"
   - Click "Create Credentials" → "API Key"
   - Copy the key (looks like: `AIzaSyxxxxxxxxxxxxxxxxxxxxxxxxx`)

4. **Configure in WebScraper**
   - Same as Brave Search steps above
   - Select "Google Places API" instead
   - Paste Google API key

5. **Check Usage**
   - Monitor usage in Google Cloud Console
   - Free tier: $200/month credit
   - Set up billing alerts to avoid surprises

---

## Running the Application

### Start Application:

```bash
# Set environment
export JAVA_HOME=/mnt/c/Users/lanzc/Downloads/zulu17.64.17-ca-jdk17.0.18-linux_x64/zulu17.64.17-ca-jdk17.0.18-linux_x64
export PATH=$JAVA_HOME/bin:$PATH

# Navigate to project
cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Stop Application:

```bash
# Find process
ps aux | grep webscraper | grep -v grep

# Kill process
kill <PID>
```

### Check Status:

```bash
# Check if running
ps aux | grep webscraper

# Check port
ss -tlnp | grep 8080

# Test HTTP
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/login
```

---

## Access Information

### URLs:

| Page | URL |
|------|-----|
| **Homepage** | http://172.22.74.128:8080 |
| **Login** | http://172.22.74.128:8080/login |
| **Dashboard** | http://172.22.74.128:8080/dashboard |
| **Recipes** | http://172.22.74.128:8080/recipes |
| **Jobs** | http://172.22.74.128:8080/jobs |
| **Discovery** | http://172.22.74.128:8080/discovery |
| **Settings** | http://172.22.74.128:8080/settings |

### Login Credentials:

```
Username: admin
Password: admin
```

### Important Notes:

- **Use WSL IP address:** `172.22.74.128`
- **NOT localhost:** Windows Chrome can't reach WSL localhost
- **Port:** Always `:8080`

---

## Troubleshooting

### Problem: "Site can't be reached"

**Solution:**
1. Check if app is running:
   ```bash
   ps aux | grep webscraper
   ss -tlnp | grep 8080
   ```
2. Use correct URL: `http://172.22.74.128:8080` (NOT localhost)
3. Check WSL IP: `hostname -I | awk '{print $1}'`
4. Restart app if needed

### Problem: "Discovery doesn't proceed"

**Solution:**
1. Check logs: `tail -100 /tmp/app.log | grep -i error`
2. If shows Playwright errors → Need browser dependencies
3. Either:
   - Install dependencies (requires sudo)
   - Get API key (Brave or Google)

### Problem: "HTTP 500 Error"

**Solution:**
1. Check application logs
2. Common causes:
   - Database connection failed
   - Template error
   - Missing configuration
3. Restart application

### Problem: "Build Failed"

**Solution:**
```bash
# Clean and rebuild
./mvnw clean package -DskipTests

# Check Java version
java -version  # Should be 17

# Check JAVA_HOME
echo $JAVA_HOME
```

---

## File Structure

```
webscraper-main/
├── src/
│   ├── main/
│   │   ├── java/com/browzwi/webscraper/
│   │   │   ├── domain/           # JPA Entities
│   │   │   ├── repository/       # Spring Data Repositories
│   │   │   ├── service/          # Business Logic
│   │   │   ├── web/              # Controllers
│   │   │   └── scraper/          # Scraping Engine
│   │   └── resources/
│   │       ├── templates/        # Thymeleaf HTML
│   │       └── db/changelog/     # Liquibase Migrations
│   └── test/                     # Unit Tests
├── pom.xml                       # Maven Dependencies
├── application.yml               # Configuration
└── README.md                     # Documentation
```

---

## Key Files Modified

### Entities:
- `DiscoveryJob.java` - Discovery job entity
- `DiscoveredBusiness.java` - Discovered business entity
- `AppSetting.java` - Application settings

### Services:
- `UrlDiscoveryService.java` - Main discovery orchestration
- `AntiDetectPlaywrightService.java` - Playwright with anti-detection
- `BraveSearchService.java` - Brave Search API integration
- `SettingsService.java` - Application settings management

### Controllers:
- `DiscoveryController.java` - Discovery feature endpoints
- `SettingsController.java` - Settings management
- `RecipeController.java` - Recipe CRUD
- `ScrapeJobController.java` - Job management

### Templates:
- `discovery/index.html` - Discovery search page
- `discovery/results.html` - Results display
- `settings/index.html` - Settings configuration

---

## Quick Reference

### Common Commands:

```bash
# Start app
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Build app
./mvnw clean package -DskipTests

# Run tests
./mvnw test

# Check logs
tail -f /tmp/app.log

# Find process
ps aux | grep webscraper

# Kill process
kill <PID>
```

### API Endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/discovery` | Discovery page |
| POST | `/discovery/start` | Start discovery job |
| GET | `/discovery/{id}/results` | View results |
| GET | `/discovery/{id}/export` | Export to Excel |
| GET | `/settings` | Settings page |
| POST | `/settings` | Save settings |

---

## Next Steps

### Immediate:

1. **Choose a solution:**
   - [ ] Get sudo access and install dependencies
   - [ ] Get Brave Search API key (recommended)
   - [ ] Get Google Places API key
   - [ ] Keep as is (Discovery won't work)

2. **If installing dependencies:**
   - Follow Solution 1 commands above
   - Restart application
   - Test Discovery

3. **If using API:**
   - Sign up for API key
   - Configure in Settings
   - Test Discovery

### Long-term:

1. **Deploy to production**
2. **Set up MySQL database**
3. **Configure production settings**
4. **Set up SSL/HTTPS**
5. **Add monitoring and logging**

---

## Contact & Resources

### Documentation:
- Spring Boot: https://spring.io/projects/spring-boot
- Playwright: https://playwright.dev/java/
- Brave Search API: https://brave.com/search/api/
- Google Places API: https://developers.google.com/maps/documentation/places/web-service

### Project Location:
```
/mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main/
```

### This Document:
```
/mnt/c/Users/lanzc/Downloads/WEBSCRAPER_SETUP_GUIDE.md
```

---

**Last Updated:** March 5, 2026  
**Status:** Application Running, Discovery Needs API Key or Browser Dependencies
