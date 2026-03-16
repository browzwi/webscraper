package com.browzwi.webscraper;

import com.browzwi.webscraper.storage.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Web Scraper Application.
 *
 * <p>Architectural rationale: This class serves as the Spring Boot application
 * entry point, configuring the application context and enabling property
 * configuration for file storage settings. It bootstraps all required services,
 * controllers, and scheduled jobs for the web scraping functionality.
 *
 * <p>Key constraints: This class must be in the root package to allow Spring's
 * component scanning to detect all application components.
 *
 * @since 1.0
 */
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(FileStorageProperties.class)
public class WebScraperApplication {

    /**
     * Main method to start the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(WebScraperApplication.class, args);
    }
}
