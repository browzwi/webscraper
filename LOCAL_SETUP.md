# WebScraper Local Setup Guide

## ✅ Setup Complete!

The WebScraper application is now running locally on your machine.

---

## 📋 Access Information

### Application URL
- **Main Application**: http://localhost:8080
- **H2 Database Console**: http://localhost:8080/h2-console

### Login Credentials
- **Username**: `admin`
- **Password**: `admin`

---

## 🚀 How to Start the Application

### Quick Start Command

```bash
cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main

export JAVA_HOME=/mnt/c/Users/lanzc/Downloads/zulu17.64.17-ca-jdk17.0.18-linux_x64/zulu17.64.17-ca-jdk17.0.18-linux_x64
export PATH=$JAVA_HOME/bin:$PATH

./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Alternative: Run JAR Directly

```bash
cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main

export JAVA_HOME=/mnt/c/Users/lanzc/Downloads/zulu17.64.17-ca-jdk17.0.18-linux_x64/zulu17.64.17-ca-jdk17.0.18-linux_x64
export PATH=$JAVA_HOME/bin:$PATH

java -jar target/webscraper-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

---

## 🛠️ Build Commands

### Clean Build
```bash
export JAVA_HOME=/mnt/c/Users/lanzc/Downloads/zulu17.64.17-ca-jdk17.0.18-linux_x64/zulu17.64.17-ca-jdk17.0.18-linux_x64
export PATH=$JAVA_HOME/bin:$PATH

cd /mnt/c/Users/lanzc/Downloads/webscraper-main/webscraper-main
./mvnw clean package -DskipTests
```

### Run Tests
```bash
./mvnw test
```

### Build and Run
```bash
./mvnw clean package -DskipTests && java -jar target/webscraper-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

---

## 📁 Configuration

### Current Profile: `local`

The `local` profile uses:
- **Database**: H2 in-memory database (no MySQL required)
- **H2 Console**: Enabled at `/h2-console`
- **Admin User**: auto-created with username `admin` / password `admin`
- **Storage**: `./data` directory

### H2 Database Connection Details

If you need to access the H2 console:
- **JDBC URL**: `jdbc:h2:mem:webscraper`
- **Username**: `sa`
- **Password**: (leave blank)
- **Driver**: `org.h2.Driver`

---

## 📊 Features Available

Once logged in, you can:

1. **Create Scraping Recipes**
   - Navigate to Recipes → New Recipe
   - Define YAML-based scraping configurations
   - Test recipes before saving

2. **Create Scraping Jobs**
   - Navigate to Jobs → New Job
   - Select a recipe and add target URLs
   - Schedule with cron expressions or run once

3. **Monitor Progress**
   - View real-time job status
   - Check target-level progress
   - View extracted data and logs

4. **Settings**
   - Choose between HtmlUnit (fast) or Playwright (full browser)
   - Configure timeout and storage settings

---

## 🔧 Environment Variables

Add these to your shell profile (`~/.bashrc` or `~/.zshrc`) for convenience:

```bash
export JAVA_HOME=/mnt/c/Users/lanzc/Downloads/zulu17.64.17-ca-jdk17.0.18-linux_x64/zulu17.64.17-ca-jdk17.0.18-linux_x64
export PATH=$JAVA_HOME/bin:$PATH
```

---

## 📝 Example Recipe

Here's a simple recipe to try:

```yaml
name: Example Site Scraper
description: Scrapes example.com
match:
  domains:
    - example.com
page:
  fields:
    - name: title
      selectors: ["h1"]
      source: TEXT
    - name: description
      selectors: ["p"]
      source: TEXT
options:
  fetcherType: HTMLUNIT
```

### Example Job URLs

Try scraping these test URLs:
- `https://example.com`
- `https://httpbin.org/html`
- `https://quotes.toscrape.com`

---

## ⚠️ Important Notes

### H2 Database Limitations
- Data is stored **in-memory** and will be **lost** when the application stops
- For persistent data, you need MySQL (see below)

### Playwright Browser
- Playwright requires browser binaries to be installed
- For now, use **HtmlUnit** fetcher (default for static sites)
- To install Playwright browsers:
  ```bash
  ./mvnw exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
  ```

---

## 🔗 Switching to MySQL (Optional)

If you want persistent data with MySQL:

1. **Install MySQL** on Windows or use an existing MySQL server

2. **Create database and user**:
   ```sql
   CREATE DATABASE webscraper;
   CREATE USER 'webscraper'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON webscraper.* TO 'webscraper'@'localhost';
   ```

3. **Update `application-dev.yml`**:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/webscraper
       username: webscraper
       password: your_password
   ```

4. **Run with dev profile**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

---

## 🐛 Troubleshooting

### Port Already in Use
If port 8080 is busy:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dserver.port=8081
```

### Java Version Error
Ensure Java 17 is being used:
```bash
$JAVA_HOME/bin/java -version
```

### Application Won't Start
Check logs in the terminal for error messages. Common issues:
- Port already in use
- Database connection failure
- Missing dependencies

### Memory Issues
Increase JVM heap size:
```bash
export MAVEN_OPTS="-Xmx1024m"
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 📚 Additional Resources

- [README.md](README.md) - Full project documentation
- [CODEBASE_GUIDE.md](CODEBASE_GUIDE.md) - Architecture and code guide
- [docs/](docs/) - Additional documentation

---

## 🛑 How to Stop the Application

If running in foreground (terminal):
- Press `Ctrl+C`

If running in background:
```bash
ps aux | grep webscraper | grep -v grep | awk '{print $2}' | xargs kill
```

Or find the PID:
```bash
ps aux | grep webscraper
kill <PID>
```

---

**Setup completed on:** March 5, 2026

**Status:** ✅ Running at http://localhost:8080
